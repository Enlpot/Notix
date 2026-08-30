package com.enlpot.notix

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.enlpot.notix.setup.SetupState
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class NotificationBlockerService : NotificationListenerService(), ActionFlowHost {

    private val TAG = "NotificationBlockerService"
    private lateinit var ruleStorage: RuleStorage
    private lateinit var notificationHistoryRepository: com.enlpot.notix.data.repository.NotificationHistoryRepository
    private lateinit var blockedNotificationHistoryStorage: BlockedNotificationHistoryStorage
    private lateinit var statsStorage: StatsStorage
    private lateinit var unmonitoredAppsStorage: UnmonitoredAppsStorage
    private lateinit var appInfoStorage: AppInfoStorage

    companion object {
        const val ACTION_HISTORY_UPDATED = "com.enlpot.notix.HISTORY_UPDATED"
        const val ACTION_APPLY_RULE = "com.enlpot.notix.APPLY_RULE"
        const val ACTION_RESCAN_ALL = "com.enlpot.notix.RESCAN_ALL"

        /** v8.14：调试/用户入口——恢复全部被冻结的常驻通知（un-snooze），由设置页「恢复常驻通知」或 adb 触发 */
        const val ACTION_RESTORE_SNOOZED = "com.enlpot.notix.RESTORE_SNOOZED"
        const val EXTRA_RULE_JSON = "rule_json"
        /**
         * 阶段4C-B P1-1：Action Flow 级防抖窗口——同一 notification key 在窗口内
         * （POST/UPDATE/apply/rescan）只执行一次 Action Flow，避免通知更新导致
         * CLICK_BUTTON/COPY/OPEN_NOTIFICATION/DISMISS 等副作用被重复执行。
         * 固定窗口 3s（2~5s 推荐区间内），简单明确、易于测试维护。
         */
        internal const val ACTION_FLOW_DEBOUNCE_MS = 3000L

        /** v7.24：TTS 播报防抖窗口——同一 sbn.key 在窗口内只播报一次，避免 POST+UPDATE 重复播报 */
        private const val TTS_DEBOUNCE_MS = 5000L

        /**
         * L3：环境快照缓存窗口。buildEnvironmentSnapshot 每次需 registerReceiver(BATTERY)
         * + 枚举蓝牙音频设备，开销不低；通知高频到达时重复构建会拖慢 binder 线程。
         * 缓存 10s 即可覆盖绝大多数连续到达场景，且快照字段（屏幕/充电/勿扰/蓝牙）变化不频繁，
         * env.now 仅用于分钟级时间条件判断，10s 内失真可忽略。
         */
        private const val ENV_CACHE_MS = 10_000L

        /**
         * 阶段4C-B P1-1：Action Flow 防抖时间源。生产默认系统时钟；
         * Instrumentation 测试可覆盖为可控时间推进，避免真实等待窗口。
         */
        internal var flowDebounceNow: () -> Long = { System.currentTimeMillis() }
        private val HEARTBEAT_INTERVAL_MS = TimeUnit.HOURS.toMillis(1)

        /** v7.11 重发通知使用的默认频道 */
        const val RULE_REPOST_CHANNEL_ID = "rule_repost"

        /** v7.23 前台服务常驻通知渠道 */
        const val KEEPALIVE_CHANNEL_ID = "keepalive"

        /** v7.23 常驻通知 ID */
        private const val NOTIFICATION_ID_KEEPALIVE = 0x4B41

        /** TTS 默认模板 */
        private const val DEFAULT_TTS_TEMPLATE = "收到{title}的{app}消息，{text}"
        /** TTS 兜底文本（所有字段均缺失时） */
        private const val DEFAULT_TTS_FALLBACK = "收到一条新消息"

        private val URL_REGEX = Regex("https?://\\S+|www\\.\\S+")
        private val EMOJI_REGEX = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+")
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val UNKNOWN_PLACEHOLDER_REGEX = Regex("\\{[a-zA-Z]+\\}")
        private val PUNCTUATION_CLUSTER_REGEX = Regex("\\s*[，。、,.；;]+\\s*[，。、,.；;]+\\s*")
        private val LEADING_PUNCTUATION_REGEX = Regex("^\\s*[，。、,.；;]+")
        private val TRAILING_PUNCTUATION_REGEX = Regex("[，。、,.；;]+\\s*$")
        private val ORPHAN_DE_REGEX = Regex("\\s*的(?=\\s|$)")

        private const val PREFS_SETTINGS = "settings"
        private const val KEY_LISTENER_PAUSED = "listener_paused"
        private const val KEY_EXTRACT_REMOTEVIEWS_TEXT = "extract_remoteviews_text"

        /** v8.13+：被冻结常驻通知 key 持久化（snooze 为系统级、跨 Service 重启仍有效，需落盘以便恢复） */
        private const val PREFS_SNOOZED = "snoozed_ongoing"
        private const val KEY_SNOOZED_KEYS = "snoozed_keys"

        /** v8.14：一次性迁移用的占位规则 id——旧 v8.13（StringSet 格式）落盘的 key 归入此规则，便于恢复 */
        private const val LEGACY_RULE_ID = "__legacy__"

        /**
         * v8.14：恢复冻结常驻通知用的「短时长 re-snooze」值（毫秒）。
         * Android 公开 API 无 unSnooze；实测（2026-08-24）对同一 key 再调 snoozeNotification(key, 极小值)
         * 会覆盖原到期时间，短值到期后通知自动回栏——这就是第三方 App 的「恢复」手段。
         * 100ms 足够短，恢复几乎即时。
         */
        private const val RESTORE_RESNOOZE_MS = 100L

        /** 全局暂停状态：暂停时停止处理通知监听。 */
        fun isListenerPaused(context: Context): Boolean =
            context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                .getBoolean(KEY_LISTENER_PAUSED, false)

        /**
         * v7.45：无文本通知的文字提取开关（默认关）。
         * 开启后，无 title/text 的通知会尝试提取按钮/自定义视图文字，
         * 拼入 text 参与规则匹配与历史记录；关闭时维持原有忽略行为。
         */
        fun isRemoteViewsTextExtractionEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                .getBoolean(KEY_EXTRACT_REMOTEVIEWS_TEXT, false)

        fun setRemoteViewsTextExtractionEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_EXTRACT_REMOTEVIEWS_TEXT, enabled).apply()
        }

        /**
         * 暂停通知监听：置位标记 → 停止服务 → 请求系统解绑监听。
         * 解绑后 onListenerDisconnected 检测到 paused 标记会跳过自动重绑。
         */
        fun pauseListening(context: Context) {
            context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_LISTENER_PAUSED, true).apply()
            try {
                context.stopService(Intent(context, NotificationBlockerService::class.java))
            } catch (e: Exception) {
                Log.w("NotificationBlocker", "stopService failed", e)
            }
            try {
                NotificationListenerService.requestUnbind(
                    ComponentName(context, NotificationBlockerService::class.java)
                )
            } catch (e: Exception) {
                Log.e("NotificationBlocker", "requestUnbind failed", e)
            }
        }

        /** 恢复通知监听：清除标记 → 请求系统重绑监听。 */
        fun resumeListening(context: Context) {
            context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_LISTENER_PAUSED, false).apply()
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(context, NotificationBlockerService::class.java)
                )
            } catch (e: Exception) {
                Log.e("NotificationBlocker", "requestRebind failed", e)
            }
        }

        /**
         * Sends an intent to the service to apply a newly created/updated rule to active
         * notifications. 阶段2C：载荷只传 rule.id——Service 端从 RuleStorage 读取完整 Rule
         * （含完整 actions 参数），不在 Intent 中复制整套 Rule JSON，避免
         * buttonLabel/template/durationMs/CopyMode 等参数丢失或失步。
         */
        fun requestApplyRule(context: Context, rule: BlockerRule) {
            val intent = Intent(context, NotificationBlockerService::class.java).apply {
                action = ACTION_APPLY_RULE
                putExtra(EXTRA_RULE_JSON, rule.id)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w("NotificationBlocker", "Failed to request rule application", e)
            }
        }

        /**
         * v7.26：请求服务全量重扫当前活跃通知——按现有启用规则重新匹配并执行动作
         * （规则删除/开关切换/手动重新扫描按钮均走此入口）。
         */
        fun requestRescanAll(context: Context) {
            val intent = Intent(context, NotificationBlockerService::class.java).apply {
                action = ACTION_RESCAN_ALL
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w("NotificationBlocker", "Failed to request rescan", e)
            }
        }

        /**
         * 阶段2D：仅 Instrumentation 集成测试使用——允许本应用包名通知进入 Action Flow
         * 处理链路（测试以本进程发布真实 Notification 验证 Service→Executor 真实接线）。
         * 默认 false，生产行为完全不变；同时始终跳过 rule_repost 重发通知与 keepalive
         * 常驻通知，防止 SILENT 重发/保活通知递归触发 Action Flow。
         */
        @Volatile
        internal var allowOwnPackageNotificationsForTest = false

        /**
         * 阶段2D：运行中 Service 实例引用（Instrumentation 测试专用，生产不使用）。
         * NotificationBlockerService 是系统绑定的 NotificationListenerService，
         * stopService() 无法触发真实 onDestroy（系统绑定保持存活），
         * 测试通过本入口执行与 onDestroy 相同的取消语义验证 activeFlows.cancel() 有效。
         */
        @Volatile
        internal var instance: NotificationBlockerService? = null

        /**
         * 阶段2D 测试入口：模拟 Service 销毁的取消语义——
         * isDestroyed=true + 取消全部未完成 Flow + 清空 activeFlows。
         * 与 onDestroy() 中的取消逻辑完全一致；仅测试调用，生产不调用。
         */
        internal fun cancelActiveFlowsForTest() {
            instance?.let { svc ->
                svc.isDestroyed = true
                for (flow in svc.activeFlows) {
                    try {
                        flow.cancel()
                    } catch (_: Exception) {
                    }
                }
                svc.activeFlows.clear()
                // 阶段4C-B P1-1：与 onDestroy 一致，销毁时清空 Action Flow 防抖登记
                svc.actionFlowDebounce.clear()
            }
        }
    }

    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private val heartbeatRunnable: Runnable = object : Runnable {
        override fun run() {
            SetupState.recordListenerConnected(this@NotificationBlockerService)
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
        }
    }

    /**
     * v8.14：被 snooze 冻结的常驻通知 key，按「所属规则 id」分组（同步 Map，跨线程安全）。
     * 规则删除时只恢复该规则冻结的 key，避免误恢复其它规则的通知；
     * snooze 是系统级冻结、**持久化、Android 11+ 重启不失效**，故落盘到 PREFS_SNOOZED 以便跨 Service 重启后恢复；
     * 恢复时对 key 用短时长 re-snooze（见 [restoreKeys]），对失效 key（如冻结时长已自然到期）为 no-op，不报错。
     */
    private val snoozedByRule = Collections.synchronizedMap(mutableMapOf<String, MutableSet<String>>())

    /** v7.25：TTS 播报防抖登记（sbn.key → 上次播报的 postTime 与时间戳） */
    private class TtsDebounceEntry(val postTime: Long, val speakTime: Long)
    private val ttsDebounce = mutableMapOf<String, TtsDebounceEntry>()

    /**
     * 阶段4C-B P1-1：Action Flow 级防抖登记（notification key → 上次 Flow 执行时间戳）。
     * 访问时清理过期项；Service destroy 时整体清空，防止无限增长与泄漏。
     */
    internal val actionFlowDebounce = mutableMapOf<String, Long>()
    private val historyExecutor: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>(),
        ThreadFactory { r -> Thread(r, "history-writer").apply { isDaemon = true } },
        ThreadPoolExecutor.DiscardPolicy()
    )

    /** 阶段2C：Action Flow 专用单线程执行器（动作链严格串行执行，不阻塞 binder/主线程） */
    private val actionExecutor: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>(),
        ThreadFactory { r -> Thread(r, "action-worker").apply { isDaemon = true } },
        ThreadPoolExecutor.DiscardPolicy()
    )

    /** 阶段2C：进行中的 Action Flow 集合（Service 销毁时统一 cancel，防止回调继续推进） */
    private val activeFlows = java.util.concurrent.CopyOnWriteArrayList<FlowExecution>()

    /** 阶段2C：服务销毁标记——销毁后不再启动新 Flow（阶段2D：internal 供测试恢复） */
    @Volatile
    internal var isDestroyed = false

    /** 阶段2C：Action Flow 引擎（真实执行体注入本 Service 宿主能力） */
    private val actionFlowExecutor: ActionFlowExecutor by lazy {
        ActionFlowExecutor(
            syncRunner = RealSyncActionRunner(this),
            asyncRunner = RealAsyncRunner(this),
            log = { msg -> Log.i(TAG, msg) },
            hostAlive = { !isDestroyed }
        )
    }

    /** v7.23：前台服务是否已拉起 */
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        ruleStorage = RuleStorage(this)
        notificationHistoryRepository = com.enlpot.notix.data.repository.NotificationHistoryRepository(this)
        blockedNotificationHistoryStorage = BlockedNotificationHistoryStorage(this)
        statsStorage = StatsStorage(this)
        unmonitoredAppsStorage = UnmonitoredAppsStorage(this)
        appInfoStorage = AppInfoStorage(this)
        ensureRepostChannel()
        ensureKeepAliveChannel()
        loadSnoozedKeys()

        // v8.22：启动时执行旧 JSON 历史数据到 Room 的迁移（后台线程，不阻塞监听）
        Thread {
            try {
                val migration = com.enlpot.notix.data.migration.HistoryMigration(
                    this,
                    com.enlpot.notix.data.database.AppDatabase.getInstance(this).notificationGroupDao(),
                    com.enlpot.notix.data.database.AppDatabase.getInstance(this).notificationChangeDao()
                )
                if (migration.needsMigration()) {
                    Log.i(TAG, "Starting history migration in background")
                    val result = kotlinx.coroutines.runBlocking { migration.migrate() }
                    Log.i(TAG, "History migration result: $result")
                    if (result is com.enlpot.notix.data.migration.HistoryMigration.MigrationResult.SUCCESS) {
                        sendBroadcast(Intent(ACTION_HISTORY_UPDATED))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "History migration failed", e)
            }
        }.start()
    }

    private fun ensureRepostChannel() {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                RULE_REPOST_CHANNEL_ID,
                getString(R.string.rule_repost_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.rule_repost_channel_desc)
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(channel)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create repost channel", e)
        }
    }

    /** v7.23：前台服务常驻通知渠道（低优先级，静音、不显示角标） */
    private fun ensureKeepAliveChannel() {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                KEEPALIVE_CHANNEL_ID,
                getString(R.string.keepalive_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.keepalive_channel_desc)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create keepalive channel", e)
        }
    }

    /** v7.23：拉起前台服务常驻通知，防止后台进程被系统回收导致监听断开 */
    private fun startKeepAliveForeground() {
        if (isForeground) return
        try {
            val notification = Notification.Builder(this, KEEPALIVE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_stack)
                .setContentTitle(getString(R.string.keepalive_notification_title))
                .setContentText(getString(R.string.keepalive_notification_text))
                .setOngoing(true)
                .build()
            startForeground(NOTIFICATION_ID_KEEPALIVE, notification)
            isForeground = true
            Log.i(TAG, "Foreground started (keepalive)")
        } catch (e: Exception) {
            Log.w(TAG, "startForeground failed", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // v7.35：入口兜底——回调内任何异常都不允许导致进程崩溃（记录日志后不重新抛出）
        try {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // 全局暂停：不处理任何通知（保留历史与统计，仅停止监听写入）。
        if (isListenerPaused(this)) {
            Log.i(TAG, "Listener paused — ignoring notification from ${sbn.packageName}")
            return
        }

        val packageName = sbn.packageName

        // Reentrancy guard: our own re-posted notifications must never re-enter
        // rule/history processing or we recurse infinitely.
        if (packageName == BuildConfig.APPLICATION_ID) {
            // 阶段2D：测试开关（默认 false）允许本 app 测试通知进入真实处理链路；
            // rule_repost 重发通知与 keepalive 常驻通知始终跳过（防递归/防噪声）。
            if (!allowOwnPackageNotificationsForTest ||
                sbn.notification.channelId == RULE_REPOST_CHANNEL_ID ||
                sbn.id == NOTIFICATION_ID_KEEPALIVE
            ) return
        }

        val notification = sbn.notification
        var title = notification.extras.getCharSequence("android.title")?.toString()
        var text = notification.extras.getCharSequence("android.text")?.toString()
        val currentTime = System.currentTimeMillis()

        if (title.isNullOrBlank() && text.isNullOrBlank()) {
            // v7.45：无文本通知增强版（设置开关，默认关）——尝试提取按钮/自定义视图文字
            if (isRemoteViewsTextExtractionEnabled(this)) {
                val extracted = RemoteViewsTextExtractor.extract(notification)
                if (extracted != null) {
                    Log.i(TAG, "No title/text, extracted RemoteViews text from ${sbn.packageName}: $extracted")
                    text = extracted
                } else {
                    Log.i(TAG, "Ignoring notification with no title and text from ${sbn.packageName}")
                    return
                }
            } else {
                Log.i(TAG, "Ignoring notification with no title and text from ${sbn.packageName}")
                return
            }
        }

        var appLabel = resolveAppName(this, sbn).toString()
        val savedAppName = appInfoStorage.isAppInfoSaved(packageName)

        // Save App Info if not exists
        if (savedAppName == null || savedAppName == packageName) {
            try {
                val appName = appLabel
                // v8.31：图标多级容错——getApplicationIcon → ApplicationInfo.loadIcon(多flag) → 通知smallIcon → 默认图标
                val iconDrawable = loadAppIconSafely(packageName, notification.smallIcon)
                appInfoStorage.saveAppInfo(packageName, appName, iconDrawable)
                // v8.31：保存 app_info 后，同步更新通知历史中的 app_label（修复首次保存时不更新的问题）
                historyExecutor.execute {
                    kotlinx.coroutines.runBlocking {
                        notificationHistoryRepository.updateAppLabelForPackage(packageName, appLabel)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save app info for $packageName", e)
            }
        } else {
            appLabel = savedAppName
        }

        Log.i(TAG, "Notification Received: App='${appLabel}' titleLen=${title?.length} textLen=${text?.length}")

        // v7.11 决策：来源App过滤 → 关键字匹配 → 额外条件 → 动作
        val rules = ruleStorage.getRules()
        val env = getEnvironmentSnapshot()
        val decision = RuleMatcher.planNotificationDecision(rules, packageName, title, text, env)
        val matchedRule: BlockerRule? = (decision as? RuleDecision.Apply)?.rule

        // 执行动作（binder 线程，尽量轻量；Action Flow 在 action-worker 串行执行）
        if (matchedRule != null) {
            Log.i(TAG, "Rule matched for $packageName: ${matchedRule.description ?: matchedRule.id}")
            executeActionFlow(sbn, matchedRule, appLabel, title, text, currentTime)
        }

        // 命中规则的通知计入「被规则处理」历史（原 blocked 历史）
        val isHandled = matchedRule != null
        val hitRuleIds = if (matchedRule != null) listOf(matchedRule.id) else emptyList()

        // v8.24：移除服务层防抖——依赖 Repository 层去重（同 sbnKey 同 postTime）和聚合机制。
        // 原 3s 防抖会导致聊天消息等内容变化的短时间更新被漏记录，故删除。
        // v7.15：携带 sbnKey/postTime 供存储层按"同一条通知"去重，不再按 pkg+title 误吞
        // v8.22：wasOngoing 从 notification.flags 读取真实状态，不再硬编码 false
        val isOngoing = (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val simpleNotification = SimpleNotification(
            appLabel, packageName, title, text, currentTime,
            wasOngoing = isOngoing,
            sbnKey = sbn.key,
            postTime = sbn.postTime,
            matchedRuleIds = hitRuleIds
        )

        sbn.notification.contentIntent?.let { intent ->
            simpleNotification.id?.let { id ->
                NotificationActionRepository.saveAction(id, intent)
            }
        }

        // v7.35：判活——线程池已销毁（服务销毁后残留回调）时不再提交，避免 RejectedExecutionException
        if (!historyExecutor.isShutdown) {
            historyExecutor.execute {
                try {
                    ruleStorage.incrementHitCounts(hitRuleIds)
                    if (isHandled) {
                        // v7.12：被过滤通知并入统一历史（blocked 标记），不再分流写入
                        val isNew = kotlinx.coroutines.runBlocking {
                            notificationHistoryRepository.saveNotification(simpleNotification, blocked = true)
                        }
                        if (isNew) {
                            statsStorage.incrementBlockedNotificationsCount()
                        }
                    } else {
                        if (!unmonitoredAppsStorage.isAppUnmonitored(packageName)) {
                            kotlinx.coroutines.runBlocking {
                                notificationHistoryRepository.saveNotification(simpleNotification)
                            }
                            statsStorage.recordNotification(currentTime)
                        }
                    }
                    sendBroadcast(Intent(ACTION_HISTORY_UPDATED))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save notification data", e)
                }
            }
        }
        // v7.25：顺带清理过期 TTS 防抖登记，防止 map 无限增长
        ttsDebounce.entries.removeIf { (_, entry) -> currentTime - entry.speakTime > TTS_DEBOUNCE_MS }
        } catch (e: Exception) {
            // v7.35：记录崩溃日志（CrashLogManager.logFile 写入 crash_logs.txt），不重新抛出
            Log.e(TAG, "Unexpected error in onNotificationPosted", e)
            try {
                val file = CrashLogManager.logFile(this)
                val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                val entry = "===== " + ts + " =====\n" +
                    "Thread: " + Thread.currentThread().name + "\n" +
                    "Exception: " + e.javaClass.name + ": " + (e.message ?: "") + "\n" +
                    e.stackTrace.joinToString("\n") { "    at $it" } + "\n"
                val existing = if (file.exists()) file.readText(Charsets.UTF_8) else ""
                file.writeText(entry + existing, Charsets.UTF_8)
            } catch (ignore: Exception) {
                Log.w(TAG, "Failed to write crash log", ignore)
            }
        }
    }

    /**
     * 阶段2C：正式接入 ActionFlowExecutor——创建完整 ActionContext（数据快照 + 运行时内存引用），
     * 将 [rule.actions] 完整动作链交由引擎严格串行执行。
     * History / hitCount / 广播由 historyExecutor 独立负责，Action Flow 不参与。
     */
    private fun executeActionFlow(
        sbn: StatusBarNotification,
        rule: BlockerRule,
        appLabel: String,
        title: String?,
        text: String?,
        currentTime: Long
    ) {
        if (isDestroyed || actionExecutor.isShutdown) {
            Log.w(TAG, "ActionFlow skipped (service destroyed/shutdown) rule=${rule.id}")
            return
        }
        // 阶段4C-B P1-1：Action Flow 级防抖——已确定该通知需要执行 Flow 时，
        // 以 notification key 为维度、在真正 execute 之前做短窗口防抖：
        // 同一 key 在 ACTION_FLOW_DEBOUNCE_MS 内重复 POST/UPDATE/apply/rescan 不再触发 Flow。
        // 防抖时间从“实际接受/执行 Flow”起算；访问时顺带清理过期项，防止 map 无限增长。
        val debounceNow = flowDebounceNow()
        val flowKey = sbn.key
        val lastFlowAt = actionFlowDebounce[flowKey]
        if (lastFlowAt != null && debounceNow - lastFlowAt < ACTION_FLOW_DEBOUNCE_MS) {
            Log.i(TAG, "ActionFlow debounced for key=$flowKey (window=${ACTION_FLOW_DEBOUNCE_MS}ms) rule=${rule.id}")
            return
        }
        actionFlowDebounce[flowKey] = debounceNow
        actionFlowDebounce.entries.removeIf { (_, ts) -> debounceNow - ts >= ACTION_FLOW_DEBOUNCE_MS }
        // v8.13：从规则首个 DISMISS 动作读 includeOngoing；v8.14 加读 snoozeDurationMs，一并传入 ActionContext
        val dismissParams = rule.actions
            .firstOrNull { it.type == RuleAction.DISMISS }
            ?.params
        val includeOngoing = dismissParams
            ?.takeIf { it.has("includeOngoing") }
            ?.get("includeOngoing")
            ?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        val snoozeDurationMs = dismissParams
            ?.takeIf { it.has("snoozeDurationMs") }
            ?.get("snoozeDurationMs")
            ?.takeIf { it.isJsonPrimitive }?.asLong
            ?.takeIf { it > 0L } ?: SnoozeDurations.DAY_7
        val ctx = ActionContext(
            ruleId = rule.id,
            packageName = sbn.packageName,
            appName = appLabel,
            title = title,
            text = text,
            notificationKey = sbn.key,
            postTime = sbn.postTime,
            includeOngoing = includeOngoing,
            snoozeDurationMs = snoozeDurationMs,
            sbn = sbn,
            notificationActions = sbn.notification.actions,
            contentIntent = sbn.notification.contentIntent,
        )
        Log.i(TAG, "ActionFlow start rule=${rule.id} pkg=${sbn.packageName} actions=${rule.actions.size}")
        actionExecutor.execute {
            var flow: FlowExecution? = null
            flow = actionFlowExecutor.execute(rule.actions, ctx) { result ->
                Log.i(TAG, "ActionFlow complete rule=${rule.id} status=${result.status} executed=${result.executedCount} failures=${result.failedActions.size}")
                flow?.let { activeFlows.remove(it) }
            }
            // 同步完成的 Flow 已结束无需追踪；未完成（TTS/DELAY 挂起）的登记以便 destroy 时统一 cancel
            if (flow?.isCompleted != true) activeFlows.add(flow!!)
        }
    }

    /** 单字段清洗：去 URL/emoji/多余空白并截断 */
    private fun cleanTtsPart(s: String?, maxLen: Int): String {
        if (s == null) return ""
        var v = URL_REGEX.replace(s, " ")
        v = EMOJI_REGEX.replace(v, "")
        v = v.replace(WHITESPACE_REGEX, " ").trim()
        return v.take(maxLen)
    }

    /** TTS {time}/{date} 占位符格式化（固定中文口语化格式，便于播报） */
    private fun formatTtsTime(postTime: Long): String {
        if (postTime <= 0L) return ""
        return SimpleDateFormat("HH点mm分", Locale.SIMPLIFIED_CHINESE).format(postTime)
    }

    private fun formatTtsDate(postTime: Long): String {
        if (postTime <= 0L) return ""
        return SimpleDateFormat("M月d日", Locale.SIMPLIFIED_CHINESE).format(postTime)
    }

    /** 重发一条通知（用于静音5秒 / SILENT 动作）。阶段2C：至少保留 smallIcon/largeIcon/title/text/contentIntent/ongoing */
    private fun repostNotification(
        sbn: StatusBarNotification,
        title: String?,
        text: String?,
        channelId: String,
        silent: Boolean
    ) {
        val nm = getSystemService(NotificationManager::class.java)
        val builder = if (android.os.Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        sbn.notification.smallIcon?.let { builder.setSmallIcon(it) }
        builder.setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
        // 保留点击行为：原通知 contentIntent
        sbn.notification.contentIntent?.let { builder.setContentIntent(it) }
        // 保留 ongoing：原通知 FLAG_ONGOING_EVENT
        if (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0) builder.setOngoing(true)
        // 大图标（来自缓存，避免 PackageManager 调用）
        try {
            appInfoStorage.getAppIcon(sbn.packageName)?.let { builder.setLargeIcon(it) }
        } catch (e: Exception) {
            // ignore
        }
        val id = (sbn.id and 0xFFFF) + 100000 + (currentRepostSeq++)
        nm.notify(id, builder.build())
    }

    private var currentRepostSeq = 0

    // ============ ActionFlowHost 实现（阶段2C：为 ActionFlowExecutor 真实执行体提供 Android 副作用） ============

    override fun cancelNotificationCompat(key: String) {
        cancelNotification(key)
    }

    /**
     * v8.13+：DISMISS 对常驻通知的冻结实现。
     * API 26+ 走 [snoozeNotification]（key + durationMs），冻结时长由调用方传入（用户可选）；
     * API <26 无 snoozeNotification，降级到 [cancelNotification]（常驻通知上可能无效）。
     *
     * 关键事实（2026-08-24 实测坐实，纠正旧注释）：
     * - NotificationListenerService **只有 snooze、没有 unSnooze**（公开 API 无此方法）。
     * - **snooze 是持久化的、Android 11+ 重启不失效**（写入 /data/system/notification_policy.xml）。
     * - 恢复手段 = 对同一 key 再调 snoozeNotification(key, 极小值)（见 [RESTORE_RESNOOZE_MS]），
     *   短值到期后通知自动回栏；或等冻结时长自然到期。
     * 冻结成功后把 key 归入 [ruleId] 分组并落盘，用于「规则删除时恢复 / UI 展示可恢复项」。
     *
     * @param ruleId 触发本次冻结的规则 id（来自 [ActionContext.ruleId]）；为 null 时不计入分组表。
     * @param durationMs 冻结时长（毫秒，用户可选；来自 [ActionContext.snoozeDurationMs]）。
     */
    override fun snoozeNotificationCompat(key: String, ruleId: String?, durationMs: Long) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            try {
                snoozeNotification(key, durationMs)
                if (ruleId != null) {
                    synchronized(snoozedByRule) {
                        snoozedByRule.getOrPut(ruleId) { mutableSetOf() }.add(key)
                    }
                }
                persistSnoozedKeys()
            } catch (e: Exception) {
                Log.w(TAG, "snoozeNotification failed for key=$key, fallback to cancel", e)
                cancelNotification(key)
            }
        } else {
            cancelNotification(key)
        }
    }

    /**
     * v8.14：真正恢复被冻结常驻通知——对每个 key 用 [RESTORE_RESNOOZE_MS] 短时长 re-snooze。
     * 短值到期后通知自动回到通知栏（实测有效）。同时把 key 从分组表移除并落盘。
     */
    fun restoreSnoozedByRule(ruleId: String): Int {
        val keys = synchronized(snoozedByRule) {
            snoozedByRule.remove(ruleId)?.toList().orEmpty()
        }
        if (keys.isEmpty()) return 0
        restoreKeys(keys)
        persistSnoozedKeys()
        Log.i(TAG, "Rule $ruleId deleted: restored ${keys.size} snoozed ongoing key(s)")
        return keys.size
    }

    /**
     * v8.14：恢复全部被冻结常驻通知（设置页「恢复常驻通知」按钮 / [ACTION_RESTORE_SNOOZED] 调用）。
     * @return 实际尝试恢复的 key 数。
     */
    fun restoreAllSnoozedNotifications(): Int {
        val allKeys = synchronized(snoozedByRule) {
            val list = snoozedByRule.values.flatten().toList()
            snoozedByRule.clear()
            list
        }
        if (allKeys.isEmpty()) return 0
        restoreKeys(allKeys)
        persistSnoozedKeys()
        Log.i(TAG, "Restored ${allKeys.size} snoozed ongoing key(s)")
        return allKeys.size
    }

    /** 对一批 key 执行短时长 re-snooze（恢复），单个失败不阻断后续 */
    private fun restoreKeys(keys: List<String>) {
        if (android.os.Build.VERSION.SDK_INT < 26) {
            Log.w(TAG, "restoreKeys requires API 26+; skip ${keys.size} key(s)")
            return
        }
        for (key in keys) {
            try {
                snoozeNotification(key, RESTORE_RESNOOZE_MS)
            } catch (e: Exception) {
                Log.w(TAG, "restore re-snooze failed for key=$key", e)
            }
        }
    }

    /** v8.13+：当前被冻结常驻通知 key 列表（供 UI 展示「可恢复」项）。 */
    fun getSnoozedKeys(): List<String> = synchronized(snoozedByRule) { snoozedByRule.values.flatten().toList() }

    /** v8.13+：从 PREFS_SNOOZED 载入已冻结 key 分组表（onCreate 调用；失败不影响启动） */
    private fun loadSnoozedKeys() {
        try {
            val prefs = getSharedPreferences(PREFS_SNOOZED, Context.MODE_PRIVATE)
            snoozedByRule.clear()
            val raw = prefs.getString(KEY_SNOOZED_KEYS, null)
            if (raw != null) {
                // 新格式：JSON 对象 {ruleId: [key,...]}
                parseSnoozedJson(raw)?.let { snoozedByRule.putAll(it) }
            } else {
                // 一次性迁移：旧 v8.13 用 StringSet 落盘，归入 LEGACY_RULE_ID 以便恢复
                val legacy = prefs.getStringSet(KEY_SNOOZED_KEYS, null)
                if (!legacy.isNullOrEmpty()) {
                    snoozedByRule[LEGACY_RULE_ID] = legacy.toMutableSet()
                    persistSnoozedKeys()
                    Log.i(TAG, "Migrated ${legacy.size} legacy snoozed key(s)")
                }
            }
            Log.i(TAG, "Loaded ${getSnoozedKeys().size} snoozed ongoing key(s)")
        } catch (e: Exception) {
            Log.w(TAG, "loadSnoozedKeys failed", e)
        }
    }

    /** v8.14：解析新格式 JSON 分组表；失败返回 null。 */
    private fun parseSnoozedJson(raw: String): Map<String, MutableSet<String>>? = try {
        val root = JsonParser.parseString(raw).asJsonObject
        val map = mutableMapOf<String, MutableSet<String>>()
        for ((ruleId, elem) in root.entrySet()) {
            val set = mutableSetOf<String>()
            elem.asJsonArray.forEach { set.add(it.asString) }
            map[ruleId] = set
        }
        map
    } catch (e: Exception) {
        Log.w(TAG, "parseSnoozedJson failed", e)
        null
    }

    /** v8.13+：将分组表落盘为 JSON 字符串到 PREFS_SNOOZED（失败仅记日志，不抛） */
    private fun persistSnoozedKeys() {
        try {
            val prefs = getSharedPreferences(PREFS_SNOOZED, Context.MODE_PRIVATE)
            val root = JsonObject()
            synchronized(snoozedByRule) {
                for ((ruleId, keys) in snoozedByRule) {
                    if (keys.isEmpty()) continue
                    val arr = JsonArray()
                    keys.forEach { arr.add(it) }
                    root.add(ruleId, arr)
                }
            }
            prefs.edit().putString(KEY_SNOOZED_KEYS, root.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "persistSnoozedKeys failed", e)
        }
    }

    override fun copyToClipboard(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("notix-copy", text))
    }

    /** 构建 TTS 播报文本：模板占位符替换 + 清洗（缺失片段跳过、去 emoji/URL/多余空白、正文截断约 60 字） */
    override fun buildTtsText(template: String?, app: String?, title: String?, text: String?, postTime: Long): String {
        val cleanApp = cleanTtsPart(app, 20)
        val cleanTitle = cleanTtsPart(title, 30)
        val cleanText = cleanTtsPart(text, 60)
        val cleanTime = formatTtsTime(postTime)
        val cleanDate = formatTtsDate(postTime)
        val tpl = template?.takeIf { it.contains("{") } ?: DEFAULT_TTS_TEMPLATE
        var out = tpl
            .replace("{app}", cleanApp)
            .replace("{title}", cleanTitle)
            .replace("{text}", cleanText)
            .replace("{time}", cleanTime)
            .replace("{date}", cleanDate)
            // 移除模板中残留的未识别占位符
            .replace(UNKNOWN_PLACEHOLDER_REGEX, "")
        // 清洗：URL/emoji/多余空白/孤立"的"与标点残留
        out = URL_REGEX.replace(out, " ")
        out = EMOJI_REGEX.replace(out, "")
        out = out.replace(WHITESPACE_REGEX, " ")
            .replace(PUNCTUATION_CLUSTER_REGEX, "，")
            .replace(LEADING_PUNCTUATION_REGEX, "")
            .replace(TRAILING_PUNCTUATION_REGEX, "")
            .replace(ORPHAN_DE_REGEX, "")
            .trim()
        return out.ifBlank { DEFAULT_TTS_FALLBACK }
    }

    override fun speakTts(ctx: ActionContext, text: String, onDone: (Boolean) -> Unit) {
        // v7.25：TTS 播报防抖——同 key 且 postTime 未变（同一条通知的 POST+UPDATE 双回调）5 秒内只播报一次
        val now = System.currentTimeMillis()
        val lastTts = ttsDebounce[ctx.notificationKey]
        val isSameNotification = lastTts != null && lastTts.postTime == ctx.postTime
        if (isSameNotification && now - lastTts.speakTime < TTS_DEBOUNCE_MS) {
            Log.d(TAG, "TTS debounced for ${ctx.packageName} key=${ctx.notificationKey} (same notification update)")
            onDone(false)
            return
        }
        ttsDebounce[ctx.notificationKey] = TtsDebounceEntry(ctx.postTime, now)
        TtsSpeaker.speak(this, text) { success -> onDone(success) }
        Log.d(TAG, "TTS speaking for ${ctx.packageName} (key=${ctx.notificationKey})")
    }

    /** L3：环境快照缓存——最近一次构建结果与时间戳，命中缓存窗口时直接复用 */
    @Volatile
    private var cachedEnv: EnvironmentSnapshot? = null
    @Volatile
    private var cachedEnvAt: Long = 0L

    /** L3：带缓存的环境快照获取。命中 10s 窗口返回缓存，否则重建并刷新缓存 */
    private fun getEnvironmentSnapshot(): EnvironmentSnapshot {
        val now = System.currentTimeMillis()
        val cached = cachedEnv
        if (cached != null && now - cachedEnvAt < ENV_CACHE_MS) {
            return cached
        }
        val fresh = buildEnvironmentSnapshot()
        cachedEnv = fresh
        cachedEnvAt = now
        return fresh
    }

    /** 收集环境快照：屏幕 / 充电状态 / 勿扰模式 / 蓝牙耳机 */
    private fun buildEnvironmentSnapshot(): EnvironmentSnapshot {
        var screenOn = true
        try {
            val pm = getSystemService(PowerManager::class.java)
            screenOn = pm?.isInteractive ?: true
        } catch (e: Exception) {
            Log.w(TAG, "screen state unavailable", e)
        }
        var charging = ChargingState.BATTERY
        try {
            val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val plugged = sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            charging = when {
                status != BatteryManager.BATTERY_STATUS_CHARGING &&
                    status != BatteryManager.BATTERY_STATUS_FULL -> ChargingState.BATTERY
                plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingState.WIRELESS
                else -> ChargingState.WIRED
            }
        } catch (e: Exception) {
            Log.w(TAG, "charging state unavailable", e)
        }
        // 勿扰模式：interruption filter != ALL 视为开启（API 23+，minSdk 24 满足）
        var dndOn = false
        try {
            val nm = getSystemService(NotificationManager::class.java)
            dndOn = nm?.currentInterruptionFilter != null &&
                nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } catch (e: Exception) {
            Log.w(TAG, "dnd state unavailable", e)
        }
        // 蓝牙耳机：收集输出音频设备中 A2DP / SCO 蓝牙设备名（AudioDeviceInfo.getProductName()，免权限）
        val bluetoothDeviceNames = mutableListOf<String>()
        try {
            val am = getSystemService(AudioManager::class.java)
            val devices = am?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
            devices.filter { d ->
                d.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    d.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }.forEach { d ->
                d.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
                    if (name !in bluetoothDeviceNames) bluetoothDeviceNames.add(name)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "bluetooth headset state unavailable", e)
        }
        return EnvironmentSnapshot(
            screenOn = screenOn,
            charging = charging,
            dndOn = dndOn,
            bluetoothDeviceNames = bluetoothDeviceNames,
            now = System.currentTimeMillis(),
        )
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        SetupState.recordListenerConnected(this)
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS)
        // v7.23：监听建立后拉起前台服务，保活防回收
        startKeepAliveForeground()
        Log.i(TAG, "Listener connected")
        // v8.24：防漏通知——监听重连后同步当前通知栏中已有的通知，补记录
        syncActiveNotifications()
    }

    /**
     * v8.24：同步当前通知栏中已有的通知，防止服务重启/监听断开期间漏记录。
     *
     * 场景：app 被系统杀死、崩溃、监听权限重授权后重启，通知栏中已有的通知
     * 不会触发 onNotificationPosted，导致历史记录缺失。这里主动拉取当前通知栏
     * 通知，按 sbnKey 去重后补记录。
     */
    private fun syncActiveNotifications() {
        Thread {
            try {
                val active = getActiveNotifications()
                if (active.isNullOrEmpty()) {
                    Log.i(TAG, "Sync active notifications: none in shade")
                    return@Thread
                }
                Log.i(TAG, "Sync active notifications: found  in shade, starting sync")
                var synced = 0
                var skipped = 0
                for (sbn in active) {
                    try {
                        val key = sbn.key
                        // 检查是否已在历史中（按 sbnKey 去重）
                        val exists: Boolean = kotlinx.coroutines.runBlocking {
                            notificationHistoryRepository.existsBySbnKey(key)
                        }
                        if (exists) {
                            skipped++
                            // v8.31：即使通知已在历史中，也确保 app_info 已保存（修复图标/名称显示包名的问题）
                            ensureAppInfoSaved(sbn)
                            continue
                        }
                        // 未记录则走正常处理流程
                        onNotificationPosted(sbn)
                        synced++
                    } catch (e: Exception) {
                        Log.w(TAG, "Sync notification failed for key=", e)
                    }
                }
                Log.i(TAG, "Sync active notifications complete: synced=$synced, skipped=$skipped, total=${active.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Sync active notifications failed", e)
            }
        }.start()
    }

    /**
     * v8.31：确保 app_info 已保存。如果 app_info 不存在或保存的是包名，则重新保存，
     * 并同步更新通知历史中的 app_label。用于修复已存在通知显示包名而非应用名称的问题。
     */
    private fun ensureAppInfoSaved(sbn: StatusBarNotification) {
        try {
            val packageName = sbn.packageName
            val savedAppName = appInfoStorage.isAppInfoSaved(packageName)
            if (savedAppName == null || savedAppName == packageName) {
                // 应用名称：resolveAppName 内部已包含 PackageManager + extras 兜底
                var appName = resolveAppName(this, sbn).toString()
                // 如果还是包名，再尝试从通知 extras 获取
                if (appName == packageName) {
                    val extrasName = sbn.notification.extras.getCharSequence("android.appName")?.toString()
                    if (!extrasName.isNullOrBlank() && extrasName != packageName) {
                        appName = extrasName
                    }
                }
                // 图标：多级容错
                val iconDrawable = loadAppIconSafely(packageName, sbn.notification.smallIcon)
                appInfoStorage.saveAppInfo(packageName, appName, iconDrawable)
                historyExecutor.execute {
                    kotlinx.coroutines.runBlocking {
                        notificationHistoryRepository.updateAppLabelForPackage(packageName, appName)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ensureAppInfoSaved failed for ${sbn.packageName}", e)
        }
    }

    /**
     * v8.31：安全加载应用图标，多级容错：
     * 1. PackageManager.getApplicationIcon（正常 app）
     * 2. ApplicationInfo.loadIcon（多 flag，应对 Android 14+ 特殊状态）
     * 3. 通知 smallIcon.loadDrawable（兜底）
     * 4. 系统默认图标（最终兜底）
     */
    private fun loadAppIconSafely(packageName: String, smallIcon: android.graphics.drawable.Icon?): android.graphics.drawable.Drawable {
        // 方式1：标准方式
        try {
            return packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            Log.w(TAG, "getApplicationIcon failed for $packageName, try ApplicationInfo.loadIcon", e)
        }
        // 方式2：用多 flag 获取 ApplicationInfo 后 loadIcon
        try {
            val flags = android.content.pm.PackageManager.GET_UNINSTALLED_PACKAGES or
                    android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
            val appInfo = packageManager.getApplicationInfo(packageName, flags)
            val icon = appInfo.loadIcon(packageManager)
            if (icon != null) return icon
        } catch (e: Exception) {
            Log.w(TAG, "ApplicationInfo.loadIcon failed for $packageName, try smallIcon", e)
        }
        // 方式3：通知 smallIcon
        try {
            val icon = smallIcon?.loadDrawable(this)
            if (icon != null) return icon
        } catch (e: Exception) {
            Log.w(TAG, "smallIcon loadDrawable failed for $packageName, using default", e)
        }
        // 方式4：默认图标
        return try {
            packageManager.getApplicationIcon("android")
        } catch (e: Exception) {
            android.graphics.drawable.ColorDrawable(android.graphics.Color.GRAY)
        }
    }

    /**
     * v8.26：规则创建/更新后，将规则应用到当前通知栏中已有的通知。
     *
     * 背景：规则只在 onNotificationPosted（新通知到达）时匹配执行，规则创建前已存在的通知
     * 不会自动触发规则，导致用户创建规则后还需手动清理现有通知。这里主动拉取当前通知栏
     * 通知，逐条匹配规则并执行动作（如 DISMISS 移除）。
     *
     * 实现：复用 onNotificationPosted 处理链路；全局去重（同 sbnKey+postTime）保证不会重复写入历史。
     * 后台线程执行，避免阻塞 binder/主线程。
     */
    fun applyRulesToActiveNotifications() {
        if (isDestroyed) return
        Thread {
            try {
                val active = getActiveNotifications()
                if (active.isNullOrEmpty()) {
                    Log.i(TAG, "Apply rules to active: none in shade")
                    return@Thread
                }
                Log.i(TAG, "Apply rules to active: found ${active.size} in shade, starting matching")
                var matched = 0
                for (sbn in active) {
                    try {
                        // 跳过本 app 自己的通知（防递归）
                        if (sbn.packageName == BuildConfig.APPLICATION_ID) continue
                        // 复用 onNotificationPosted 处理链路（含规则匹配+执行+历史写入）
                        // 全局去重保证已存在历史的通知不会重复写入
                        onNotificationPosted(sbn)
                        matched++
                    } catch (e: Exception) {
                        Log.w(TAG, "Apply rule to notification failed for key=${sbn.key}", e)
                    }
                }
                Log.i(TAG, "Apply rules to active complete: processed=$matched, total=${active.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Apply rules to active notifications failed", e)
            }
        }.start()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // v7.11: no stack handling; nothing to do
    }

    override fun onListenerDisconnected() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        super.onListenerDisconnected()
        if (isListenerPaused(this)) {
            Log.w(TAG, "Listener disconnected (paused by user) — skip auto rebind")
            return
        }
        Log.w(TAG, "Listener disconnected — requesting rebind")
        try {
            requestRebind(ComponentName(this, NotificationBlockerService::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "requestRebind failed", e)
        }
    }

    override fun onDestroy() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        instance = null
        // v7.23：若已在前台则移除常驻通知
        if (isForeground) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
        super.onDestroy()
        TtsSpeaker.shutdown()
        // 阶段2C：取消所有未完成的 Action Flow（含 TTS/DELAY 回调），避免 Service 销毁后回调继续执行
        isDestroyed = true
        for (flow in activeFlows) {
            try {
                flow.cancel()
            } catch (_: Exception) {
            }
        }
        activeFlows.clear()
        // 阶段4C-B P1-1：销毁时清空 Action Flow 防抖登记，避免旧生命周期防抖记录泄漏到新生命周期
        actionFlowDebounce.clear()
        // v7.35：延迟 5s 销毁线程池——服务销毁瞬间可能仍有 onNotificationPosted 回调在途，
        // 立即 shutdown 会让 execute 抛 RejectedExecutionException；延迟销毁 + 判活 + DiscardPolicy 三重保障。
        // 回调在 5s 后于主线程执行，服务此时已可正常销毁，不阻塞销毁流程。
        Handler(Looper.getMainLooper()).postDelayed({
            historyExecutor.shutdown()
            try {
                if (!historyExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    historyExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                historyExecutor.shutdownNow()
            }
            actionExecutor.shutdown()
            try {
                if (!actionExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    actionExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                actionExecutor.shutdownNow()
            }
        }, 5000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_APPLY_RULE) {
            val ruleId = intent.getStringExtra(EXTRA_RULE_JSON) ?: return START_NOT_STICKY
            try {
                // 阶段2C：ruleId → RuleStorage 读取完整 Rule（含完整 actions 参数），
                // 不再从 Intent 重建/丢失 buttonLabel/template/durationMs/CopyMode
                val rule = ruleStorage.getRules().find { it.id == ruleId }
                if (rule != null) {
                    applyRuleToActiveNotifications(rule)
                } else {
                    Log.w(TAG, "Apply rule: rule not found id=$ruleId")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply rule", e)
            }
        } else if (intent?.action == ACTION_RESCAN_ALL) {
            // v7.26：全量重扫当前活跃通知（规则删除/开关切换/手动重新扫描按钮）
            rescanActiveNotifications()
        } else if (intent?.action == ACTION_RESTORE_SNOOZED) {
            // v8.14：恢复全部冻结常驻通知（设置页「恢复常驻通知」按钮 / adb 调试触发）。
            // 对每个 key 用短时长 re-snooze（100ms）覆盖原到期时间，短值到期后通知自动回栏。
            val n = restoreAllSnoozedNotifications()
            Log.i(TAG, "ACTION_RESTORE_SNOOZED: restored $n snoozed notification(s)")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * v7.26：全量重扫通知栏当前活跃通知——用全部启用规则重新匹配并执行动作。
     * 支持全部动作（消除/静音/替换/点击按钮/打开/复制/TTS），供规则删除、开关切换
     * 与规则卡片「重新扫描通知」按钮调用。
     */
    private fun rescanActiveNotifications() {
        val active = try {
            activeNotifications?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot access active notifications", e)
            return
        }
        if (active.isEmpty()) {
            Log.d(TAG, "Rescan: no active notifications")
            return
        }
        val rules = ruleStorage.getRules().filter { it.isEnabled }
        if (rules.isEmpty()) {
            Log.d(TAG, "Rescan: no enabled rules")
            return
        }
        val env = getEnvironmentSnapshot()
        val currentTime = System.currentTimeMillis()
        for (sbn in active) {
            if (sbn.packageName == BuildConfig.APPLICATION_ID) {
                // 阶段2D：测试开关（默认 false）允许本 app 测试通知参与重扫；rule_repost/keepalive 始终跳过
                if (!allowOwnPackageNotificationsForTest ||
                    sbn.notification.channelId == RULE_REPOST_CHANNEL_ID ||
                    sbn.id == NOTIFICATION_ID_KEEPALIVE
                ) continue
            }
            val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            if (title.isNullOrBlank() && text.isNullOrBlank()) continue
            val decision = RuleMatcher.planNotificationDecision(rules, sbn.packageName, title, text, env)
            val matchedRule = (decision as? RuleDecision.Apply)?.rule ?: continue
            val appLabel = resolveAppName(this, sbn).toString()
            Log.i(TAG, "Rescan matched ${sbn.packageName}: ${matchedRule.description ?: matchedRule.id}")
            executeActionFlow(sbn, matchedRule, appLabel, title, text, currentTime)
        }
        sendBroadcast(Intent(ACTION_HISTORY_UPDATED))
    }

    /** 规则创建/编辑后，对当前活跃通知回溯应用（v7.26：支持全部动作，阶段2C：走 ActionFlowExecutor 完整 actions） */
    private fun applyRuleToActiveNotifications(rule: BlockerRule) {
        if (!rule.isEnabled) return
        val active = try {
            activeNotifications?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot access active notifications", e)
            return
        }
        val sourcePkgs = rule.sourcePackages.map { it.packageName }.toSet()
        val currentTime = System.currentTimeMillis()
        for (sbn in active) {
            if (sbn.packageName == BuildConfig.APPLICATION_ID) {
                // 阶段2D：测试开关（默认 false）允许本 app 测试通知参与回溯应用；rule_repost/keepalive 始终跳过
                if (!allowOwnPackageNotificationsForTest ||
                    sbn.notification.channelId == RULE_REPOST_CHANNEL_ID ||
                    sbn.id == NOTIFICATION_ID_KEEPALIVE
                ) continue
            }
            if (sourcePkgs.isNotEmpty() && sbn.packageName !in sourcePkgs) continue
            val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            if (RuleMatcher.matchesCondition(rule.condition, title, text)) {
                val appLabel = resolveAppName(this, sbn).toString()
                Log.i(TAG, "Retroactively applying rule ${rule.id} (${rule.actions.size} action(s)) to ${sbn.packageName}")
                executeActionFlow(sbn, rule, appLabel, title, text, currentTime)
            }
        }
    }

    fun resolveAppName(context: Context, sbn: StatusBarNotification): CharSequence {
        val extras = sbn.notification.extras
        val pkg = sbn.packageName
        return try {
            // v8.31：使用多 flag 获取 ApplicationInfo，应对 Android 14+ 特殊状态
            val flags = android.content.pm.PackageManager.GET_UNINSTALLED_PACKAGES or
                    android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
            val ai = context.packageManager.getApplicationInfo(pkg, flags)
            val label = context.packageManager.getApplicationLabel(ai).toString()
            if (label == "Android系统" || label == "Android System") {
                extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
                    ?: extras.getCharSequence("android.appName")
                    ?: label
            } else {
                label
            }
        } catch (_: Exception) {
            extras.getCharSequence("android.appName")?.toString() ?: pkg
        }
    }

}
