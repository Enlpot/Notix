package com.enlpot.notix

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 阶段3B：Rule 导入 / 导出完整验证（JVM 纯测试）。
 *
 * 覆盖：
 * - Export 完整 Flow（5 个 Action 全存在，逐项 type + params，不只查 size）
 * - Import 完整 Flow（数量 / 顺序 / 类型 / 参数全部恢复）
 * - Round-trip（Export → Import → Compare，actions 完全一致）
 * - 顺序保持（TTS→COPY 不变 COPY→TTS）
 * - 参数保持（TTS.template / CopyMode / Delay.durationMs / ClickButton.buttonLabel）
 * - params=null 问题实测：应用自己导出的 JSON 中无参数 Action 被 Gson 省略字段
 *   （而非 "params": null），再 Import 完全正常
 */
class RuleImportExportRoundTripTest {

    /** 文档规定的完整测试 Rule：TTS → COPY → DELAY 2000 → CLICK_BUTTON "Mark as read" → DISMISS */
    private fun fiveActionRule(): BlockerRule = BlockerRule(
        id = "export-rule",
        sourcePackages = listOf(SourceApp("com.test.app", "Test")),
        condition = RuleCondition(includeKeywords = listOf("k")),
        actions = listOf(
            ActionSpec(RuleAction.TTS, TtsParams("{title}：{text}").toParamsJson()),
            ActionSpec(RuleAction.COPY, CopyParams(CopyMode.TITLE_AND_TEXT).toParamsJson()),
            ActionSpec(RuleAction.DELAY, DelayParams(2000L).toParamsJson()),
            ActionSpec(RuleAction.CLICK_BUTTON, ClickButtonParams("Mark as read").toParamsJson()),
            ActionSpec(RuleAction.DISMISS, null),
        ),
    )

    /** 完整导出 → 导入，返回导入后的 Rule（id 会被 RuleImport 重铸，actions 必须保持）。 */
    private fun exportThenImport(rule: BlockerRule): BlockerRule {
        val json = RuleExportSerializer.toJson(RuleExport(rules = listOf(rule)))
        val result = RuleImport.parse(json)
        assertTrue("import should succeed, got $result", result is ImportResult.Success)
        return (result as ImportResult.Success).rules.single()
    }

    // Test 1：Export 完整 Flow —— 5 个 Action 全存在，逐项验证 type 与 params
    @Test
    fun `test1 export five action flow keeps all actions and params`() {
        val json = RuleExportSerializer.toJson(RuleExport(rules = listOf(fiveActionRule())))
        val obj = JsonParser.parseString(json).asJsonObject
        val rulesArr = obj.getAsJsonArray("rules")
        assertEquals(1, rulesArr.size())
        val actionsArr = rulesArr[0].asJsonObject.getAsJsonArray("actions")
        assertEquals(5, actionsArr.size())

        val a0 = actionsArr[0].asJsonObject
        assertEquals("TTS", a0.get("type").asString)
        assertEquals("{title}：{text}", a0.getAsJsonObject("params").get("template").asString)

        val a1 = actionsArr[1].asJsonObject
        assertEquals("COPY", a1.get("type").asString)
        assertEquals("TITLE_AND_TEXT", a1.getAsJsonObject("params").get("mode").asString)

        val a2 = actionsArr[2].asJsonObject
        assertEquals("DELAY", a2.get("type").asString)
        assertEquals(2000L, a2.getAsJsonObject("params").get("durationMs").asLong)

        val a3 = actionsArr[3].asJsonObject
        assertEquals("CLICK_BUTTON", a3.get("type").asString)
        assertEquals("Mark as read", a3.getAsJsonObject("params").get("buttonLabel").asString)

        val a4 = actionsArr[4].asJsonObject
        assertEquals("DISMISS", a4.get("type").asString)
    }

    // Test 2：Import 完整 Flow —— 5 个 Action 全部恢复（数量 / 顺序 / 类型 / 参数）
    @Test
    fun `test2 import five action flow restores all actions and params`() {
        val imported = exportThenImport(fiveActionRule())

        assertEquals(5, imported.actions.size)
        assertEquals(
            listOf(
                RuleAction.TTS, RuleAction.COPY, RuleAction.DELAY,
                RuleAction.CLICK_BUTTON, RuleAction.DISMISS,
            ),
            imported.actions.map { it.type }
        )
        assertEquals("{title}：{text}", imported.actions[0].params?.get("template")?.asString)
        assertEquals("TITLE_AND_TEXT", imported.actions[1].params?.get("mode")?.asString)
        assertEquals(2000L, imported.actions[2].params?.get("durationMs")?.asLong)
        assertEquals("Mark as read", imported.actions[3].params?.get("buttonLabel")?.asString)
        assertNull(imported.actions[4].params)
    }

    // Test 3：Round-trip —— Export → Import → Compare，actions 完全一致
    @Test
    fun `test3 round trip actions fully identical`() {
        val original = fiveActionRule()
        val imported = exportThenImport(original)

        assertTrue(
            "original.actions must equal imported.actions (order+type+params)",
            RuleWizardSupport.actionFlowEquals(original.actions, imported.actions)
        )
        // 反向也相等（顺序敏感比较是对称的）
        assertTrue(RuleWizardSupport.actionFlowEquals(imported.actions, original.actions))
    }

    // Test 4：顺序保持 —— TTS→COPY 不能变成 COPY→TTS
    @Test
    fun `test4 order preserved tts copy not flipped`() {
        val rule = BlockerRule(
            id = "order-rule",
            sourcePackages = listOf(SourceApp("com.test.app", "Test")),
            condition = RuleCondition(includeKeywords = listOf("k")),
            actions = listOf(
                ActionSpec(RuleAction.TTS, TtsParams("{title}").toParamsJson()),
                ActionSpec(RuleAction.COPY, CopyParams(CopyMode.TEXT).toParamsJson()),
                ActionSpec(RuleAction.DISMISS, null),
            ),
        )
        val imported = exportThenImport(rule)

        assertEquals(3, imported.actions.size)
        assertEquals(
            listOf(RuleAction.TTS, RuleAction.COPY, RuleAction.DISMISS),
            imported.actions.map { it.type }
        )
        assertTrue(RuleWizardSupport.actionFlowEquals(rule.actions, imported.actions))
    }

    // Test 5：参数保持 —— TTS.template / CopyMode / Delay.durationMs / ClickButton.buttonLabel
    @Test
    fun `test5 params preserved template mode duration and button label`() {
        val imported = exportThenImport(fiveActionRule())

        assertEquals("{title}：{text}", imported.actions[0].params?.get("template")?.asString)
        assertEquals("TITLE_AND_TEXT", imported.actions[1].params?.get("mode")?.asString)
        assertEquals(2000L, imported.actions[2].params?.get("durationMs")?.asLong)
        assertEquals("Mark as read", imported.actions[3].params?.get("buttonLabel")?.asString)
    }

    // Test 6：params=null 问题实测 —— 应用导出不会出现 "params": null 字面量，导入正常
    @Test
    fun `test6 app exported json has no literal params null and imports fine`() {
        val json = RuleExportSerializer.toJson(RuleExport(rules = listOf(fiveActionRule())))
        // Gson 默认 omitNull：无参数 Action（DISMISS）的 params 字段被省略，而非 "params": null
        assertFalse("export must not contain literal params null", json.contains("\"params\": null"))
        assertFalse("export must not contain literal params null with space", json.contains("\"params\":null"))

        val result = RuleImport.parse(json)
        assertTrue("import must succeed for app's own export", result is ImportResult.Success)
        val imported = (result as ImportResult.Success).rules.single()
        assertEquals(5, imported.actions.size)
        assertNull(imported.actions[4].params)
        assertTrue(
            RuleWizardSupport.actionFlowEquals(fiveActionRule().actions, imported.actions)
        )
    }
}
