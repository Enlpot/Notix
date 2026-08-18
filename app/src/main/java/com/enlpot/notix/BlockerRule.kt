package com.enlpot.notix

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.math.BigDecimal

/**
 * 规则数据模型（Action Flow 阶段1 新结构）。
 *
 * v7.37 起规则从「单选 action + actionParams」切换为「actions: List<ActionSpec>」
 * 顺序动作链。旧模型（action/actionParams 字段）不再兼容，旧规则因 actions 缺失
 * 被判为无效并在加载时过滤清空（测试阶段允许）。
 */
enum class MatchMode {
    /** 包含任一 */
    CONTAINS_ANY,
    /** 包含全部 */
    CONTAINS_ALL,
    /** 不包含任一 */
    NOT_CONTAINS_ANY,
    /** 不包含全部 */
    NOT_CONTAINS_ALL,
    /** 包含A且不包含B */
    MIXED,
    /** 高级匹配（仅UI展示，暂不可用） */
    ADVANCED,
}

/** 屏幕状态额外条件 */
enum class ScreenState { ANY, SCREEN_ON, SCREEN_OFF }

/** 充电状态额外条件 */
enum class ChargingState { ANY, WIRED, WIRELESS, BATTERY }

/** 勿扰模式状态额外条件 */
enum class DndState { ANY, ON, OFF }

/** 蓝牙耳机连接状态额外条件 */
enum class BluetoothState { ANY, CONNECTED, DISCONNECTED }

/** 动作：Action Flow 7 种，按 actions 列表顺序严格执行 */
enum class RuleAction {
    /** 消除通知 */
    DISMISS,
    /** 静默显示（取消原通知 + 低打扰频道重发） */
    SILENT,
    /** 点击按钮（手动输入按钮名） */
    CLICK_BUTTON,
    /** 打开通知 */
    OPEN_NOTIFICATION,
    /** 复制内容（标题/正文/标题+正文） */
    COPY,
    /** TTS 朗读 */
    TTS,
    /** 延迟等待 */
    DELAY,
}

/** 时间日期条件（HH:mm-HH:mm + 星期多选） */
data class TimeCondition(
    val enabled: Boolean = false,
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 23,
    val endMinute: Int = 59,
    /** 1=周一 ... 7=周日；空 = 每天 */
    val weekdays: List<Int> = emptyList(),
)

/** 手机状态额外条件（可选） */
data class ExtraCondition(
    val screenState: ScreenState = ScreenState.ANY,
    val chargingState: ChargingState = ChargingState.ANY,
    val dndState: DndState = DndState.ANY,
    val bluetoothState: BluetoothState = BluetoothState.ANY,
    /** 指定蓝牙设备名（多选，任一命中即成立；空 = 不限设备） */
    val bluetoothDeviceNames: List<String> = emptyList(),
    val time: TimeCondition = TimeCondition(),
) {
    fun isEmpty(): Boolean =
        screenState == ScreenState.ANY &&
            chargingState == ChargingState.ANY &&
            dndState == DndState.ANY &&
            bluetoothState == BluetoothState.ANY &&
            bluetoothDeviceNames.orEmpty().isEmpty() &&
            !time.enabled
}

/** 关键字匹配条件 */
data class RuleCondition(
    val mode: MatchMode = MatchMode.CONTAINS_ANY,
    /** A 组关键字（包含/不包含/混合均使用） */
    val includeKeywords: List<String> = emptyList(),
    /** B 组关键字（仅 MIXED「包含A且不包含B」使用） */
    val excludeKeywords: List<String> = emptyList(),
) {
    fun isEmpty(): Boolean = includeKeywords.isEmpty() && excludeKeywords.isEmpty()
}

/** 来源 App（仅历史通知中出现过的 App） */
data class SourceApp(
    val packageName: String,
    val appName: String? = null,
)

/** COPY 复制范围 */
enum class CopyMode { TITLE, TEXT, TITLE_AND_TEXT }

/** TTS 播报参数：模板支持 {app}/{title}/{text} 占位符；空 = 默认模板 */
data class TtsParams(val template: String? = null)

/** COPY 复制参数 */
data class CopyParams(val mode: CopyMode = CopyMode.TITLE_AND_TEXT)

/** DELAY 延迟参数（毫秒，必须 > 0） */
data class DelayParams(val durationMs: Long = 1000L)

/** CLICK_BUTTON 点击按钮参数：按钮名（PendingIntent 为运行时对象，不落 JSON） */
data class ClickButtonParams(val buttonLabel: String = "")

/**
 * 动作规格：type + 参数。
 *
 * - List 本身定义顺序，不保存 order 字段：actions[0] 最先执行。
 * - [params] 采用 Gson 原生 [JsonObject]，规避 sealed class 在 Gson 反射下的
 *   反序列化难题；各动作参数 data class 通过 [toParamsJson]/[asParams] 互转。
 * - DISMISS / SILENT / OPEN_NOTIFICATION 无参数（params = null）。
 */
data class ActionSpec(
    val type: RuleAction,
    val params: JsonObject? = null,
) {
    /** 参数合法性：CLICK_BUTTON 要求 buttonLabel 非空、DELAY 要求 durationMs > 0，其余默认 true */
    val isValid: Boolean
        get() = when (type) {
            RuleAction.CLICK_BUTTON -> params?.get("buttonLabel")?.asString?.isNotBlank() == true
            RuleAction.DELAY -> {
                // 阶段 4C-C-B P2-3：安全解析，非法 durationMs 返回 false 而非抛 NFE
                // （超 Long 数字 / 非数字字符串 / 0 / 负数 / 非原始值 一律视为非法）
                val el = params?.get("durationMs")
                if (el == null || !el.isJsonPrimitive) {
                    false
                } else {
                    val prim = el.asJsonPrimitive
                    val duration = when {
                        // Gson 对超 Long 整数字面量可能解析为 Double（asLong 会饱和为 MAX_VALUE）
                        // 或 LazilyParsedNumber/BigDecimal；统一按 BigDecimal 校验 Long 范围，防 NFE
                        prim.isNumber -> runCatching {
                            val bd = prim.asBigDecimal
                            if (bd > BigDecimal.valueOf(Long.MAX_VALUE) || bd < BigDecimal.valueOf(Long.MIN_VALUE)) {
                                null
                            } else {
                                bd.longValueExact()
                            }
                        }.getOrNull()
                        prim.isString -> prim.asString.trim().toLongOrNull()
                        else -> null
                    }
                    duration != null && duration > 0
                }
            }
            else -> true
        }
}

// Gson 互转扩展（执行层/UI 层共用）：data class ⇄ JsonObject
// @PublishedApi internal：public inline 函数 asParams 需要可访问
@PublishedApi
internal val paramsGson = Gson()

/** data class → JsonObject */
fun Any.toParamsJson(): JsonObject = paramsGson.toJsonTree(this).asJsonObject

/** JsonObject → data class */
inline fun <reified T> JsonObject?.asParams(): T =
    paramsGson.fromJson(this ?: JsonObject(), T::class.java)

/**
 * 规则（Action Flow 新结构）。
 *
 * 执行流水线：来源App过滤 → 关键字匹配 → 额外条件判断 → 顺序执行 actions。
 */
data class BlockerRule(
    val id: String = "",
    val description: String? = null,
    val isEnabled: Boolean = true,
    val hitCount: Int = 0,
    /** 来源 App 列表（多选） */
    val sourcePackages: List<SourceApp> = emptyList(),
    /** 关键字匹配条件 */
    val condition: RuleCondition = RuleCondition(),
    /** 手机状态额外条件 */
    val extraCondition: ExtraCondition = ExtraCondition(),
    /** 动作链（严格按顺序执行） */
    val actions: List<ActionSpec> = emptyList(),
    val createdAt: Long = 0L,
) {
    /**
     * 新模型合法性：至少一个来源 App 且动作链非空且全部有效。
     * 旧模型规则（缺 actions 字段，Gson 反序列化后为 null）将被判为无效并作废。
     */
    val isValid: Boolean
        get() = !sourcePackages.isNullOrEmpty() &&
            actions.orEmpty().isNotEmpty() &&
            actions.orEmpty().all { it.isValid }
}
