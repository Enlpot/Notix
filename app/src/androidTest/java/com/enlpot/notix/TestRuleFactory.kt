package com.enlpot.notix

import com.google.gson.JsonObject
import java.util.UUID

/**
 * 阶段2D：快速构造 BlockerRule / ActionSpec 的测试工厂。
 *
 * 规则 sourcePackages 固定为本应用（com.enlpot.notix）——测试通知由本进程
 * NotificationManager 发布，包名即本应用；关键字使用 AFT_ 前缀，与系统/keepalive
 * 通知天然隔离。
 */
object TestRuleFactory {

    /** 规则共用关键字前缀：所有集成测试通知标题/正文必须包含对应关键字才能命中 */
    const val KEYWORD_PREFIX = "AFT"

    fun action(type: RuleAction, params: JsonObject? = null): ActionSpec =
        ActionSpec(type, params)

    fun tts(template: String? = null): ActionSpec =
        action(RuleAction.TTS, TtsParams(template).toParamsJson())

    fun copy(mode: CopyMode = CopyMode.TITLE_AND_TEXT): ActionSpec =
        action(RuleAction.COPY, CopyParams(mode).toParamsJson())

    fun delay(durationMs: Long): ActionSpec =
        action(RuleAction.DELAY, DelayParams(durationMs).toParamsJson())

    fun click(buttonLabel: String): ActionSpec =
        action(RuleAction.CLICK_BUTTON, ClickButtonParams(buttonLabel).toParamsJson())

    val dismiss: ActionSpec = action(RuleAction.DISMISS)

    val silent: ActionSpec = action(RuleAction.SILENT)

    fun rule(
        actions: List<ActionSpec>,
        keywords: List<String>,
        isEnabled: Boolean = true
    ): BlockerRule = BlockerRule(
        id = UUID.randomUUID().toString(),
        description = "AFT test rule",
        isEnabled = isEnabled,
        hitCount = 0,
        sourcePackages = listOf(SourceApp(BuildConfig.APPLICATION_ID, "测试")),
        condition = RuleCondition(
            mode = MatchMode.CONTAINS_ANY,
            includeKeywords = keywords,
            excludeKeywords = emptyList()
        ),
        extraCondition = ExtraCondition(),
        actions = actions,
        createdAt = System.currentTimeMillis()
    )
}
