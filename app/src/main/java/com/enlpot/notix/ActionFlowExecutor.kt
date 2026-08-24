package com.enlpot.notix

import android.app.Notification
import android.app.PendingIntent
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import com.google.gson.JsonObject

/**
 * Action Flow 核心执行引擎（阶段 2B）。
 *
 * 职责：将 [BlockerRule.actions] 按 List 顺序严格串行执行，负责 Flow 生命周期状态、
 * 失败继续、异步完成信号（TTS onDone / DELAY 到期）与 at-most-once 防重复推进。
 *
 * 设计约束（阶段 2B 任务）：
 * - 每次 [execute] 创建独立 [FlowExecution]：currentIndex / failedActions / cancelled 均为该 Flow
 *   私有状态，多通知并发 Flow 互不污染；禁止全局 currentIndex、禁止 Executor 级共享 currentAction。
 * - 同步 Action（DISMISS/SILENT/CLICK_BUTTON/OPEN_NOTIFICATION/COPY）执行完立即推进下一个；
 *   抛异常 → catch → 记录 FAILED → 继续下一个，不终止整个 Flow。
 * - TTS / DELAY 为异步 Action：必须等 onDone/onError 或 postDelayed 到期后才推进，
 *   绝不 speak 后立即 next、绝不 postDelayed 后立即执行后续。
 * - 每个 Action 至多完成一次（at-most-once）：重复回调（onDone+onError / onDone+onDone）一律忽略。
 * - 运行时对象（StatusBarNotification/Notification.Action/PendingIntent）只存在于 [ActionContext] 内存，
 *   绝不序列化进 Rule JSON（Rule JSON 只保存 Action type + params）。
 *
 * 阶段 2C：已正式接入 NotificationBlockerService（宿主 [ActionFlowHost]），
 * 生产执行体 [RealSyncActionRunner] / [RealAsyncRunner] 提供真实 Android 能力；
 * JVM 单测注入 Fake 验证引擎逻辑。
 */

// ============ ActionContext ============

/**
 * 运行时上下文：Flow 开始时创建一次，整条链共享。
 *
 * 数据快照（packageName/appName/title/text/notificationKey/postTime）在 Flow 开始时捕获，
 * DISMISS/SILENT 消除通知后后续 TTS/COPY 仍可读取；运行时对象（sbn/notificationActions/
 * contentIntent）为实时内存引用，仅本次 Flow 使用，绝不落 Rule JSON。
 */
class ActionContext(
    val ruleId: String,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val notificationKey: String,
    val postTime: Long,
    /** 实时通知对象（CLICK_BUTTON/OPEN_NOTIFICATION/SILENT 使用；可为 null 便于 JVM 测试） */
    val sbn: StatusBarNotification? = null,
    /** 实时按钮列表（= sbn.notification.actions） */
    val notificationActions: Array<Notification.Action>? = null,
    /** 实时点击 Intent（= sbn.notification.contentIntent） */
    val contentIntent: PendingIntent? = null,
)

// ============ 执行体接口（生产真实实现 + 测试 Fake 注入） ============

/**
 * Action Flow 宿主能力：由 NotificationBlockerService 实现，向真实执行体提供 Android 副作用
 * （取消通知 / 低打扰重发 / 剪贴板 / TTS 模板与播报）。引擎与 Service 解耦，JVM 测试注入 Fake。
 */
interface ActionFlowHost {
    /** DISMISS：取消指定 key 的通知 */
    fun cancelNotificationCompat(key: String)

    /** SILENT：取消原通知 + 低打扰频道重发（保留点击/ongoing/图标） */
    fun repostSilent(ctx: ActionContext)

    /** COPY：写入系统剪贴板 */
    fun copyToClipboard(text: String)

    /** TTS：按模板构建播报文本（占位符替换 + 清洗），复用 Service 现有 buildTtsText */
    fun buildTtsText(template: String?, app: String?, title: String?, text: String?): String

    /** TTS：播报 [text]，完成后回调 [onDone](success)；Service 内部保留防抖 */
    fun speakTts(ctx: ActionContext, text: String, onDone: (Boolean) -> Unit)
}

/** 同步 Action 执行体：封装 Android API 调用；业务失败通过抛异常表达（引擎 catch → FAILED 继续） */
interface SyncActionRunner {
    fun dismiss(ctx: ActionContext)
    fun silent(ctx: ActionContext)
    fun clickButton(ctx: ActionContext, spec: ActionSpec)
    fun openNotification(ctx: ActionContext)
    fun copy(ctx: ActionContext, spec: ActionSpec)
}

/** 异步 Action 执行体：TTS 完成回调 onDone(success)、DELAY 到期回调 onComplete */
interface AsyncActionRunner {
    fun runTts(ctx: ActionContext, spec: ActionSpec, onDone: (Boolean) -> Unit)
    fun runDelay(delayMs: Long, onComplete: () -> Unit)
}

// ============ FlowResult / FlowExecution ============

/** Flow 终态 */
enum class FlowStatus {
    /** 全部 Action 成功 */
    SUCCESS,
    /** 有 Action 失败但 Flow 正常走完 */
    PARTIAL_FAILURE,
    /** 无 Action（空链） */
    EMPTY,
    /** 被取消（Service destroy / 外部取消） */
    CANCELLED,
}

/** 单个 Action 失败记录：index / type / reason */
data class ActionFailure(
    val index: Int,
    val type: RuleAction,
    val reason: String,
)

/** Flow 结果：字段从简，由 [FlowExecution.result] 在完成/取消后提供 */
class FlowResult(
    val status: FlowStatus,
    val failedActions: List<ActionFailure> = emptyList(),
    val executedCount: Int = 0,
) {
    val isSuccess: Boolean get() = status == FlowStatus.SUCCESS
    val hasFailures: Boolean get() = failedActions.isNotEmpty()
}

/**
 * 单次 Flow 执行状态：每次 [ActionFlowExecutor.execute] 创建独立实例。
 *
 * 所有状态变更均在 synchronized(this) 内完成，保证跨线程安全（TTS/DELAY 回调可能来自
 * 不同线程，与同步 Action 执行线程互斥）。
 */
class FlowExecution internal constructor(
    val actions: List<ActionSpec>,
    val context: ActionContext,
) {
    @Volatile
    var isCompleted: Boolean = false
        private set

    @Volatile
    var isCancelled: Boolean = false
        private set

    @Volatile
    var result: FlowResult? = null
        private set

    /** 已失败的 Action（index/type/reason），按执行顺序 */
    val failedActions: List<ActionFailure>
        get() = synchronized(this) { failures.toList() }

    private val failures = mutableListOf<ActionFailure>()
    private var currentIndex = -1
    private var currentActionFinished = true
    private var executedCount = 0

    /** 取消 Flow：后续 Action 与已挂起的异步回调一律不再推进 */
    fun cancel() {
        synchronized(this) {
            if (isCompleted || isCancelled) return
            isCancelled = true
            result = FlowResult(FlowStatus.CANCELLED, failures.toList(), executedCount)
        }
    }

    internal fun beginAction(index: Int) {
        currentIndex = index
        currentActionFinished = false
    }

    /**
     * 完成第 [index] 个 Action（at-most-once，需在 synchronized(this) 内调用或线程安全保证）。
     * @return true 表示本次完成生效；false 表示重复/过期回调被忽略（已取消/已完成/索引不符/已推进）
     */
    internal fun tryFinishAction(index: Int, failure: ActionFailure?): Boolean {
        if (isCancelled || isCompleted) return false
        if (currentIndex != index || currentActionFinished) return false
        finishAction(index, failure)
        return true
    }

    internal fun finishAction(index: Int, failure: ActionFailure?) {
        currentActionFinished = true
        executedCount++
        if (failure != null) failures.add(failure)
    }

    internal fun finishFlow(status: FlowStatus) {
        isCompleted = true
        result = FlowResult(status, failures.toList(), executedCount)
    }
}

// ============ ActionFlowExecutor ============

/**
 * 核心串行执行引擎（第一版保持简单：单类，不拆 Handler/Factory/Registry/Strategy 等抽象）。
 *
 * 线程模型：不自行持有 worker 线程池；调用方负责把 [execute] 放到合适的线程
 * （2C 接入时复用 Service 的 historyExecutor 或专用队列）。同步链在调用线程上串行执行，
 * TTS/DELAY 回调线程通过 [FlowExecution] 的 synchronized 与调用线程互斥。
 */
class ActionFlowExecutor(
    private val syncRunner: SyncActionRunner,
    private val asyncRunner: AsyncActionRunner,
    private val log: (String) -> Unit = {},
    /**
     * v8.0：宿主存活检查（注入 Service 的 !isDestroyed）。宿主销毁后，挂起的 DELAY/TTS
     * 回调不再推进后续动作，避免在已销毁 Service 上对失效通知执行取消/重发等副作用。
     */
    private val hostAlive: () -> Boolean = { true },
) {

    /**
     * 执行一条动作链。返回独立 [FlowExecution]；[onComplete] 在 Flow 自然完成时回调
     * （取消不回调，调用方直接观察 [FlowExecution.result]）。
     */
    fun execute(
        actions: List<ActionSpec>,
        context: ActionContext,
        onComplete: ((FlowResult) -> Unit)? = null,
    ): FlowExecution {
        val exec = FlowExecution(actions, context)
        log("ActionFlow start rule=${context.ruleId} pkg=${context.packageName} actions=${actions.size}")
        advance(exec, 0, onComplete)
        return exec
    }

    /** 推进到第 index 个 Action：严格串行，前一个成功/失败完成后才进入这里 */
    private fun advance(
        exec: FlowExecution,
        index: Int,
        onComplete: ((FlowResult) -> Unit)?,
    ) {
        synchronized(exec) {
            if (exec.isCancelled) return
            if (exec.isCompleted) return
            // v8.0：宿主已销毁则静默终止——避免销毁后 DELAY/TTS 回调继续推进动作
            if (!hostAlive()) {
                exec.cancel()
                return
            }
            if (index >= exec.actions.size) {
                val status = when {
                    exec.actions.isEmpty() -> FlowStatus.EMPTY
                    exec.failedActions.isEmpty() -> FlowStatus.SUCCESS
                    else -> FlowStatus.PARTIAL_FAILURE
                }
                exec.finishFlow(status)
                val r = exec.result
                log("ActionFlow complete rule=${exec.context.ruleId} status=$status executed=${r?.executedCount}")
                onComplete?.invoke(r!!)
                return
            }
            exec.beginAction(index)
            val spec = exec.actions[index]
            log("Action #$index ${spec.type} start")
            try {
                when (spec.type) {
                    RuleAction.DISMISS -> {
                        syncRunner.dismiss(exec.context)
                        completeAction(exec, index, null, onComplete)
                    }

                    RuleAction.CLICK_BUTTON -> {
                        if (spec.params == null) {
                            completeAction(exec, index, ActionFailure(index, spec.type, "params missing"), onComplete)
                        } else {
                            syncRunner.clickButton(exec.context, spec)
                            completeAction(exec, index, null, onComplete)
                        }
                    }

                    RuleAction.OPEN_NOTIFICATION -> {
                        syncRunner.openNotification(exec.context)
                        completeAction(exec, index, null, onComplete)
                    }

                    RuleAction.COPY -> {
                        if (spec.params == null) {
                            completeAction(exec, index, ActionFailure(index, spec.type, "params missing"), onComplete)
                        } else {
                            syncRunner.copy(exec.context, spec)
                            completeAction(exec, index, null, onComplete)
                        }
                    }

                    RuleAction.TTS -> {
                        if (spec.params == null) {
                            completeAction(exec, index, ActionFailure(index, spec.type, "params missing"), onComplete)
                        } else {
                            // ★ 必须等 onDone/onError 回调后才推进，绝不 speak 后立即 next
                            asyncRunner.runTts(exec.context, spec) { success ->
                                completeAction(
                                    exec, index,
                                    if (success) null else ActionFailure(index, spec.type, "tts failed"),
                                    onComplete
                                )
                            }
                        }
                    }

                    RuleAction.STRONG_REMIND -> {
                        // v8.10 新增：执行层留待 v8.11+ 接入 heads-up + 响铃 + 震动
                        log("Action #${index} ${spec.type} skipped (execution TODO)")
                        completeAction(exec, index, null, onComplete)
                    }

                    RuleAction.DELAY -> {
                        val ms = parseDelayMs(spec.params)
                        if (ms == null || ms <= 0) {
                            completeAction(exec, index, ActionFailure(index, spec.type, "invalid durationMs"), onComplete)
                        } else {
                            // ★ 必须等 postDelayed 到期后才推进
                            asyncRunner.runDelay(ms) {
                                completeAction(exec, index, null, onComplete)
                            }
                        }
                    }

                    RuleAction.POSTPONE -> {
                        // v8.10 新增：执行层留待 v8.11+ 接入 Handler.postDelayed 重新投递通知
                        log("Action #${index} ${spec.type} skipped (execution TODO)")
                        completeAction(exec, index, null, onComplete)
                    }
                }
            } catch (e: Exception) {
                // 任何同步 Action 抛异常 → FAILED → 继续下一个，不终止 Flow
                completeAction(
                    exec, index,
                    ActionFailure(index, spec.type, e.message ?: e.javaClass.simpleName),
                    onComplete
                )
            }
        }
    }

    /**
     * 完成第 index 个 Action 并推进下一个。
     * at-most-once：仅当该 Action 当前正在执行且未被完成过时才生效；重复/过期回调一律忽略。
     */
    private fun completeAction(
        exec: FlowExecution,
        index: Int,
        failure: ActionFailure?,
        onComplete: ((FlowResult) -> Unit)?,
    ) {
        synchronized(exec) {
            if (exec.isCancelled || exec.isCompleted) return
            // v8.0：宿主已销毁则静默终止
            if (!hostAlive()) {
                exec.cancel()
                return
            }
            // at-most-once：重复/过期回调（onDone+onError / onDone+onDone / 取消后到期）一律忽略
            val accepted = exec.tryFinishAction(index, failure)
            if (!accepted) return
            if (failure != null) {
                log("Action #$index ${failure.type} failed: ${failure.reason}")
            } else {
                log("Action #$index success")
            }
            advance(exec, index + 1, onComplete)
        }
    }

    private fun parseDelayMs(params: JsonObject?): Long? = try {
        params?.get("durationMs")?.asLong
    } catch (e: Exception) {
        null
    }
}

// ============ 生产实现（阶段 2B 骨架；2C 接入 NotificationBlockerService 时完善） ============

/**
 * 生产同步执行体：封装真实 Android 副作用（取消通知 / 低打扰重发 / 剪贴板 / 按钮匹配 /
 * contentIntent.send）。Android 能力经 [ActionFlowHost] 由 NotificationBlockerService 提供。
 *
 * 失败语义：业务失败（找不到按钮 / 无 contentIntent）抛异常，由引擎 catch → FAILED → 继续；
 * 不吞异常、不自行决定终止 Flow。
 */
class RealSyncActionRunner(private val host: ActionFlowHost) : SyncActionRunner {
    override fun dismiss(ctx: ActionContext) {
        host.cancelNotificationCompat(ctx.notificationKey)
    }

    override fun silent(ctx: ActionContext) {
        host.repostSilent(ctx)
    }

    override fun clickButton(ctx: ActionContext, spec: ActionSpec) {
        val label = spec.params?.get("buttonLabel")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
        if (label.isEmpty()) throw IllegalStateException("empty buttonLabel")
        val actions = ctx.notificationActions
        if (actions.isNullOrEmpty()) throw IllegalStateException("no notification actions for '$label'")
        // 匹配策略：精确匹配优先（忽略大小写）、包含匹配兜底；多个同名按钮取第一个
        val hit = actions.firstOrNull { it.title?.toString()?.equals(label, ignoreCase = true) == true }
            ?: actions.firstOrNull { it.title?.toString()?.contains(label, ignoreCase = true) == true }
        val intent = hit?.actionIntent
            ?: throw IllegalStateException("action '$label' not found")
        intent.send()
    }

    override fun openNotification(ctx: ActionContext) {
        val contentIntent = ctx.contentIntent
            ?: throw IllegalStateException("no contentIntent to open")
        contentIntent.send()
    }

    override fun copy(ctx: ActionContext, spec: ActionSpec) {
        // 复用现有复制逻辑：CopyMode 三态（TITLE/TEXT/TITLE_AND_TEXT），缺失默认 TITLE_AND_TEXT
        val mode = spec.params?.get("mode")?.takeIf { it.isJsonPrimitive }?.asString
            ?.let { runCatching { CopyMode.valueOf(it) }.getOrNull() } ?: CopyMode.TITLE_AND_TEXT
        val sb = StringBuilder()
        if (mode == CopyMode.TITLE_AND_TEXT || mode == CopyMode.TITLE) {
            if (!ctx.title.isNullOrBlank()) sb.append(ctx.title)
        }
        if (mode == CopyMode.TITLE_AND_TEXT || mode == CopyMode.TEXT) {
            if (!ctx.text.isNullOrBlank()) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(ctx.text)
            }
        }
        if (sb.isNotEmpty()) host.copyToClipboard(sb.toString())
    }
}

/**
 * 生产异步执行体：DELAY 用主线程 Handler.postDelayed（禁止 Thread.sleep，不阻塞 worker）；
 * TTS 桥接 [ActionFlowHost.speakTts]（含模板构建与防抖），等 onDone/onError 才回调。
 */
class RealAsyncRunner(private val host: ActionFlowHost) : AsyncActionRunner {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun runDelay(delayMs: Long, onComplete: () -> Unit) {
        mainHandler.postDelayed({ onComplete() }, delayMs)
    }

    override fun runTts(ctx: ActionContext, spec: ActionSpec, onDone: (Boolean) -> Unit) {
        val template = spec.params?.get("template")?.takeIf { it.isJsonPrimitive }?.asString
        val spoken = host.buildTtsText(template, ctx.appName, ctx.title, ctx.text)
        host.speakTts(ctx, spoken, onDone)
    }
}
