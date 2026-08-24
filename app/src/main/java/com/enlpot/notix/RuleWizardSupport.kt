package com.enlpot.notix

import com.google.gson.JsonObject

/**
 * An app the rule wizard can offer in its picker, merged from every source that
 * knows about a package (see [RuleWizardSupport.mergeKnownApps]).
 *
 * [isQueryableInstalled] means the package is visible to PackageManager (it is one
 * of the manifest `<queries>` entries and is installed), so a colorful launcher
 * icon and label can be loaded from it. Per v7.11 requirement, the wizard no longer
 * reads the installed-app list; this flag is derived only from the small set of
 * packages we legitimately query (history apps).
 */
data class KnownApp(
    val packageName: String,
    val appName: String?,
    val isQueryableInstalled: Boolean
)

/**
 * Pure, JVM-testable logic backing the rule-creation wizard: merging the "apps we
 * know about" list and duplicate detection. No Android imports — the wizard screen
 * gathers inputs and delegates here.
 */
object RuleWizardSupport {

    /**
     * Merges every source of known apps into one deduplicated, sorted picker list.
     *
     * Label priority (first non-blank wins): AppInfoStorage name, history appLabel,
     * PackageManager label, existing-rule appName, prebuilt-rules name.
     */
    fun mergeKnownApps(
        appInfoRows: List<Pair<String, String?>>,
        historyRows: List<Pair<String, String?>>,
        queryableInstalled: Map<String, String?>,
        prebuiltNames: Map<String, String?>,
        ruleRows: List<Pair<String, String?>>
    ): List<KnownApp> {
        val labels = mutableMapOf<String, String?>()

        fun absorb(rows: Iterable<Pair<String, String?>>) {
            for ((pkg, name) in rows) {
                if (pkg.isBlank()) continue
                if (labels[pkg].isNullOrBlank()) {
                    labels[pkg] = name?.takeIf { it.isNotBlank() }
                }
            }
        }

        absorb(appInfoRows)
        absorb(historyRows)
        absorb(queryableInstalled.map { it.key to it.value })
        absorb(ruleRows)
        for ((pkg, name) in prebuiltNames) {
            if (pkg in labels && labels[pkg].isNullOrBlank()) {
                labels[pkg] = name?.takeIf { it.isNotBlank() }
            }
        }

        return labels.map { (pkg, name) ->
            KnownApp(
                packageName = pkg,
                appName = name,
                isQueryableInstalled = pkg in queryableInstalled
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName ?: it.packageName })
    }

    /**
     * v7.37 duplicate check: same source-app set, same condition mode+keywords and same
     * Action Flow counts as a duplicate. 编辑中的规则自身除外（由调用方排除）。
     * List 顺序参与比较：[TTS,COPY] 与 [COPY,TTS] 视为不同 Action Flow。
     */
    fun isDuplicate(
        existingRules: List<BlockerRule>,
        sourcePackages: List<String>,
        condition: RuleCondition,
        actions: List<ActionSpec>
    ): Boolean = existingRules.any { rule ->
        rule.isValid &&
            actionFlowEquals(rule.actions, actions) &&
            rule.condition.mode == condition.mode &&
            rule.condition.includeKeywords == condition.includeKeywords &&
            rule.condition.excludeKeywords == condition.excludeKeywords &&
            rule.sourcePackages.map { it.packageName }.toSet() == sourcePackages.toSet()
    }

    /**
     * 动作链顺序敏感比较：type + params（JSON 文本）逐项一致。
     * 不用 data class 默认 equals——Gson 的 JsonObject 未重写 equals（引用比较），
     * 内容相同但实例不同的 params 会被误判为不等，导致重复检测失效。
     * 阶段3A 起公开给 UI 层用于「编辑态自排除」与「保存后重读一致性」校验。
     */
    fun actionFlowEquals(a: List<ActionSpec>, b: List<ActionSpec>): Boolean {
        if (a.size != b.size) return false
        for (i in a.indices) {
            val x = a[i]
            val y = b[i]
            if (x.type != y.type) return false
            if (x.params?.toString() != y.params?.toString()) return false
        }
        return true
    }

    /**
     * Loose sanity check for manually entered package names. Warn-only — some valid
     * packages are unusual, so the wizard never blocks on this.
     */
    fun looksLikePackageName(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.isNotBlank() &&
            trimmed.none { it.isWhitespace() } &&
            trimmed.contains('.') &&
            !trimmed.startsWith('.') &&
            !trimmed.endsWith('.')
    }

    // ------------------------------------------------------------------
    // 阶段3A：Action Flow 编辑纯函数
    // RuleWizardScreen 只维护一个 List<ActionSpec>，以下函数是所有
    // 状态转换的唯一实现（添加/删除/上移/下移/更新），保证
    // UI 顺序 == Rule.actions 顺序。
    // ------------------------------------------------------------------

    /** 追加一个 Action 到 Flow 末尾；无参数类型 params=null，有参数类型生成默认参数。 */
    fun actionFlowAdd(actions: List<ActionSpec>, type: RuleAction): List<ActionSpec> =
        actions + ActionSpec(type = type, params = defaultParamsFor(type))

    /** 按 index 删除；越界返回原列表。 */
    fun actionFlowRemoveAt(actions: List<ActionSpec>, index: Int): List<ActionSpec> {
        if (index !in actions.indices) return actions
        return actions.filterIndexed { i, _ -> i != index }
    }

    /** 上移；已是第一项（或越界）返回原列表。 */
    fun actionFlowMoveUp(actions: List<ActionSpec>, index: Int): List<ActionSpec> {
        if (index !in actions.indices || index == 0) return actions
        val list = actions.toMutableList()
        val item = list.removeAt(index)
        list.add(index - 1, item)
        return list
    }

    /** 下移；已是最后一项（或越界）返回原列表。 */
    fun actionFlowMoveDown(actions: List<ActionSpec>, index: Int): List<ActionSpec> {
        if (index !in actions.indices || index == actions.lastIndex) return actions
        val list = actions.toMutableList()
        val item = list.removeAt(index)
        list.add(index + 1, item)
        return list
    }

    /** 只更新指定 index 的 Action，其余保持不动；编辑不会创建新 Action。 */
    fun actionFlowUpdate(actions: List<ActionSpec>, index: Int, spec: ActionSpec): List<ActionSpec> {
        if (index !in actions.indices) return actions
        return actions.mapIndexed { i, s -> if (i == index) spec else s }
    }

    /** 拖动排序：将 from 处动作移动到 to 处（保持其余相对顺序）。 */
    fun actionFlowMove(actions: List<ActionSpec>, from: Int, to: Int): List<ActionSpec> {
        if (from !in actions.indices || to !in actions.indices || from == to) return actions
        val list = actions.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        return list
    }

    /** 上移按钮可用性：index > 0（首项禁用，不越界）。 */
    fun canMoveUp(index: Int, size: Int): Boolean = index > 0 && index < size

    /** 下移按钮可用性：index < size-1（末项禁用，不越界）。 */
    fun canMoveDown(index: Int, size: Int): Boolean = index in 0 until size - 1

    /**
     * Flow 可保存条件：非空 且 全部 Action 均合法。
     * 阶段4C-B P1-2：仅判断非空会放行 isValid=false 的 Action（如 CLICK_BUTTON 空 label、
     * DELAY durationMs<=0），导致 Rule.isValid=false 永不命中、重启后静默消失。
     * 检查整个 actions List，存在任何非法 Action 即禁止保存。
     */
    fun canSaveFlow(actions: List<ActionSpec>): Boolean =
        actions.isNotEmpty() && actions.all { it.isValid }

    /** 该类型是否有参数需要编辑（有则添加后自动进入参数编辑状态）。 */
    fun hasActionParams(type: RuleAction): Boolean = when (type) {
        RuleAction.CLICK_BUTTON, RuleAction.COPY, RuleAction.TTS, RuleAction.DELAY, RuleAction.STRONG_REMIND, RuleAction.POSTPONE -> true
        // v8.13：DISMISS 有「包括常驻通知」可选参数
        RuleAction.DISMISS -> true
        else -> false
    }

    /** 各 Action 类型的默认参数；无参数类型返回 null。
     *  v8.13：DISMISS 默认 params=null（保持向后兼容、行为 = 不含常驻）；
     *  仅当用户在弹窗勾选「包括常驻通知」时，dismissSpec(true) 才会写入 DismissParams。 */
    fun defaultParamsFor(type: RuleAction): JsonObject? = when (type) {
        RuleAction.CLICK_BUTTON -> ClickButtonParams(buttonLabel = "").toParamsJson()
        RuleAction.COPY -> CopyParams(mode = CopyMode.TITLE_AND_TEXT).toParamsJson()
        RuleAction.TTS -> TtsParams(template = null).toParamsJson()
        RuleAction.DELAY -> DelayParams(durationMs = 1000L).toParamsJson()
        // v8.10 新增
        RuleAction.STRONG_REMIND -> StrongRemindParams().toParamsJson()
        RuleAction.POSTPONE -> PostponeParams().toParamsJson()
        // v8.13 新增：DISMISS 默认 null（includeOngoing=false），勾选后由 dismissSpec(true) 写入
        RuleAction.DISMISS -> null
        else -> null
    }

    /** 构建参数类 Action 的 ActionSpec（UI 编辑面板提交用）。 */
    fun clickButtonSpec(buttonLabel: String): ActionSpec =
        ActionSpec(RuleAction.CLICK_BUTTON, ClickButtonParams(buttonLabel.trim()).toParamsJson())

    fun copySpec(mode: CopyMode): ActionSpec =
        ActionSpec(RuleAction.COPY, CopyParams(mode).toParamsJson())

    fun ttsSpec(template: String?): ActionSpec =
        ActionSpec(RuleAction.TTS, TtsParams(template?.trim()?.ifBlank { null }).toParamsJson())

    fun delaySpec(durationMs: Long): ActionSpec =
        ActionSpec(RuleAction.DELAY, DelayParams(durationMs).toParamsJson())

    // v8.10 新增动作工厂
    fun strongRemindSpec(sound: Boolean = true, vibrate: Boolean = true): ActionSpec =
        ActionSpec(RuleAction.STRONG_REMIND, StrongRemindParams(sound, vibrate).toParamsJson())

    fun postponeSpec(delayMs: Long): ActionSpec =
        ActionSpec(RuleAction.POSTPONE, PostponeParams(delayMs).toParamsJson())

    // v8.13 新增：DISMISS 工厂——includeOngoing=false 时保持 params=null（向后兼容），
    // 仅 includeOngoing=true 时才写入 DismissParams（含可自定义冻结时长，v8.14 起）。
    fun dismissSpec(includeOngoing: Boolean, snoozeDurationMs: Long = SnoozeDurations.DAY_7): ActionSpec =
        if (includeOngoing) {
            ActionSpec(
                RuleAction.DISMISS,
                DismissParams(includeOngoing = true, snoozeDurationMs = snoozeDurationMs).toParamsJson()
            )
        } else {
            ActionSpec(RuleAction.DISMISS, null)
        }

    /** 从 ActionSpec 生成卡片参数摘要（只用于帮助用户快速理解）。 */
    fun actionFlowSummary(spec: ActionSpec): String {
        val params = spec.params
        return when (spec.type) {
            // v8.13：DISMISS 区分是否含常驻通知；v8.14 起显示冻结时长
            RuleAction.DISMISS -> {
                val includeOngoing = params?.get("includeOngoing")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
                if (includeOngoing) {
                    val ms = params?.get("snoozeDurationMs")?.takeIf { it.isJsonPrimitive }?.asLong
                        ?: SnoozeDurations.DAY_7
                    "移除通知（含常驻，冻结 ${formatSnoozeDuration(ms)}）"
                } else {
                    "移除通知"
                }
            }
            RuleAction.OPEN_NOTIFICATION -> "打开通知对应页面"
            RuleAction.COPY -> {
                val mode = runCatching {
                    CopyMode.valueOf(params?.get("mode")?.asString ?: "")
                }.getOrDefault(CopyMode.TITLE_AND_TEXT)
                when (mode) {
                    CopyMode.TITLE -> "复制标题"
                    CopyMode.TEXT -> "复制正文"
                    CopyMode.TITLE_AND_TEXT -> "标题 + 正文"
                }
            }
            RuleAction.TTS -> {
                val template = params?.get("template")
                    ?.takeIf { it.isJsonPrimitive }?.asString
                if (template.isNullOrBlank()) "TTS 播报通知标题和正文" else "TTS 播报：$template"
            }
            RuleAction.STRONG_REMIND -> "强提醒（heads-up + 响铃 + 震动）"
            RuleAction.DELAY -> {
                val ms = params?.get("durationMs")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0L
                if (ms > 0L && ms % 1000L == 0L) "等待 ${ms / 1000L} 秒"
                else if (ms > 0L) "等待 $ms 毫秒"
                else "等待 1 秒"
            }
            RuleAction.POSTPONE -> {
                val ms = params?.get("delayMs")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0L
                when {
                    ms <= 0L -> "延迟 1 分钟后重发"
                    ms % 60_000L == 0L -> "延迟 ${ms / 60_000L} 分钟后重发"
                    ms % 1000L == 0L -> "延迟 ${ms / 1000L} 秒后重发"
                    else -> "延迟 $ms 毫秒后重发"
                }
            }
            RuleAction.CLICK_BUTTON -> {
                val label = params?.get("buttonLabel")
                    ?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                "点击：${label.ifBlank { "未设置" }}"
            }
        }
    }

    /**
     * 组合完整 Action Flow 摘要（RulesScreen 卡片展示用）：按顺序用 " → " 连接
     * 每个 Action 的 [actionFlowSummary]，过长时只显示前 [maxShown] 个并追加
     * "…"。全 App 的 Flow 组合摘要只此一个来源，调用方不得另写一套。
     */
    fun actionFlowSummaryFlow(actions: List<ActionSpec>, maxShown: Int = 3): String {
        if (actions.isEmpty()) return ""
        val shown = actions.take(maxShown)
        val base = shown.joinToString(" → ") { actionFlowSummary(it) }
        return if (actions.size > shown.size) "$base → …" else base
    }

    /** v8.14：冻结时长的人类可读文案（供 DISMISS 摘要与 UI 档位展示共用） */
    fun formatSnoozeDuration(ms: Long): String = when (ms) {
        SnoozeDurations.HOUR_1 -> "1 小时"
        SnoozeDurations.DAY_1 -> "1 天"
        SnoozeDurations.DAY_7 -> "7 天"
        SnoozeDurations.DAY_30 -> "30 天"
        SnoozeDurations.YEAR_1 -> "1 年"
        else -> if (ms > 0 && ms % 86_400_000L == 0L) "${ms / 86_400_000L} 天" else "${ms} 毫秒"
    }
}
