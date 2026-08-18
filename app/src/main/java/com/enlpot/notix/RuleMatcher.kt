package com.enlpot.notix

import java.util.Calendar

/**
 * 环境快照：由服务层在通知到达时收集，用于「手机状态额外条件」判断。
 * 保持纯 JVM 可测（时间、屏幕、充电状态均作为参数传入）。
 */
data class EnvironmentSnapshot(
    val screenOn: Boolean = true,
    val charging: ChargingState = ChargingState.ANY,
    val dndOn: Boolean = false,
    /** 当前已连接的蓝牙音频设备名列表（A2DP/SCO，免权限读取）；空 = 无蓝牙音频设备连接 */
    val bluetoothDeviceNames: List<String> = emptyList(),
    val now: Long = System.currentTimeMillis(),
)

/** 通知决策结果 */
sealed interface RuleDecision {
    /** 无规则命中 → 正常放行 */
    data object Pass : RuleDecision

    /** 命中规则 → 按 rule.actions 顺序执行 */
    data class Apply(val rule: BlockerRule) : RuleDecision
}

/**
 * v7.11 规则匹配引擎。
 *
 * 执行流水线：来源App过滤 → 关键字匹配 → 额外条件判断 → 执行动作。
 * 纯 JVM，无 Android 依赖。
 */
object RuleMatcher {

    /**
     * 单条规则完整判定。
     * @param packageName 通知来源包名
     * @param title 通知标题（可空）
     * @param text 通知正文（可空）
     */
    fun evaluate(
        rule: BlockerRule,
        packageName: String,
        title: String?,
        text: String?,
        env: EnvironmentSnapshot = EnvironmentSnapshot()
    ): Boolean {
        if (!rule.isValid || !rule.isEnabled) return false
        if (rule.sourcePackages.none { it.packageName == packageName }) return false
        if (!matchesCondition(rule.condition, title, text)) return false
        if (!matchesExtra(rule.extraCondition, env)) return false
        return true
    }

    /** 关键字匹配（标题或内容任一字段） */
    fun matchesCondition(condition: RuleCondition, title: String?, text: String?): Boolean {
        if (condition.mode == MatchMode.ADVANCED) return false
        val include = condition.includeKeywords.filter { it.isNotBlank() }
        val exclude = condition.excludeKeywords.filter { it.isNotBlank() }
        if (include.isEmpty() && exclude.isEmpty()) return true // 无条件 = 全部命中

        fun hits(field: String?, keywords: List<String>): Boolean {
            if (field == null) return false
            val f = field.lowercase()
            return keywords.any { f.contains(it.lowercase()) }
        }

        return when (condition.mode) {
            MatchMode.CONTAINS_ANY -> include.any { hits(title, listOf(it)) || hits(text, listOf(it)) }
            MatchMode.CONTAINS_ALL -> include.all { hits(title, listOf(it)) || hits(text, listOf(it)) }
            MatchMode.NOT_CONTAINS_ANY -> include.none { hits(title, listOf(it)) || hits(text, listOf(it)) }
            MatchMode.NOT_CONTAINS_ALL -> include.any { !(hits(title, listOf(it)) || hits(text, listOf(it))) }
            MatchMode.MIXED ->
                include.all { hits(title, listOf(it)) || hits(text, listOf(it)) } &&
                    exclude.none { hits(title, listOf(it)) || hits(text, listOf(it)) }
            MatchMode.ADVANCED -> false
        }
    }

    /** 手机状态额外条件判断 */
    fun matchesExtra(extra: ExtraCondition, env: EnvironmentSnapshot): Boolean {
        if (extra.screenState == ScreenState.SCREEN_ON && !env.screenOn) return false
        if (extra.screenState == ScreenState.SCREEN_OFF && env.screenOn) return false
        if (extra.chargingState != ChargingState.ANY && extra.chargingState != env.charging) return false
        if (extra.dndState == DndState.ON && !env.dndOn) return false
        if (extra.dndState == DndState.OFF && env.dndOn) return false
        val hasBtDevice = env.bluetoothDeviceNames.isNotEmpty()
        if (extra.bluetoothState == BluetoothState.CONNECTED && !hasBtDevice) return false
        if (extra.bluetoothState == BluetoothState.DISCONNECTED && hasBtDevice) return false
        // 指定设备：多选任一命中即成立（与 CONNECTED/DISCONNECTED 语义叠加）
        // v7.21：orEmpty 兜底——Gson 反序列化旧规则 JSON 时该字段可能为 null
        if (extra.bluetoothDeviceNames.orEmpty().isNotEmpty() &&
            extra.bluetoothDeviceNames.orEmpty().none { it in env.bluetoothDeviceNames }
        ) return false
        if (extra.time.enabled) {
            val cal = Calendar.getInstance().apply { timeInMillis = env.now }
            val weekday = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1 // 1=周一 ... 7=周日
            if (extra.time.weekdays.isNotEmpty() && weekday !in extra.time.weekdays) return false
            if (!isTimeInRange(
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    extra.time.startHour,
                    extra.time.startMinute,
                    extra.time.endHour,
                    extra.time.endMinute
                )
            ) return false
        }
        return true
    }

    /** 时间区间判断，支持跨天（如 22:00-06:00） */
    fun isTimeInRange(
        hour: Int,
        minute: Int,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Boolean {
        val nowMin = hour * 60 + minute
        val startMin = startHour * 60 + startMinute
        val endMin = endHour * 60 + endMinute
        return if (startMin <= endMin) {
            nowMin in startMin..endMin
        } else {
            nowMin >= startMin || nowMin <= endMin
        }
    }

    /**
     * 决策入口：按 来源App过滤 → 关键字匹配 → 额外条件 顺序扫描启用规则，
     * 命中第一条即返回 [RuleDecision.Apply]，否则 [RuleDecision.Pass]。
     */
    fun planNotificationDecision(
        rules: List<BlockerRule>,
        packageName: String,
        title: String?,
        text: String?,
        env: EnvironmentSnapshot = EnvironmentSnapshot()
    ): RuleDecision {
        for (rule in rules) {
            if (evaluate(rule, packageName, title, text, env)) return RuleDecision.Apply(rule)
        }
        return RuleDecision.Pass
    }
}
