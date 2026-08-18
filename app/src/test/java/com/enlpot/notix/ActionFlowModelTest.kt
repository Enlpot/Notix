package com.enlpot.notix

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Action Flow 阶段1 数据模型层 JVM 单测：
 * - Gson 序列化/反序列化（actions 完整、顺序保持）
 * - ActionSpec.isValid
 * - BlockerRule.isValid
 * - RuleImport.normalize（经 parse 间接验证）
 */
class ActionFlowModelTest {

    private val gson = Gson()

    // ---------- Gson round-trip ----------

    private fun flowRule(): BlockerRule = BlockerRule(
        id = "flow-rule-1",
        sourcePackages = listOf(SourceApp("com.tencent.mm", "微信")),
        condition = RuleCondition(includeKeywords = listOf("张三")),
        actions = listOf(
            ActionSpec(RuleAction.TTS, TtsParams("{title}：{text}").toParamsJson()),
            ActionSpec(RuleAction.COPY, CopyParams(CopyMode.TITLE_AND_TEXT).toParamsJson()),
            ActionSpec(RuleAction.DELAY, DelayParams(2000).toParamsJson()),
            ActionSpec(RuleAction.CLICK_BUTTON, ClickButtonParams("回复").toParamsJson()),
            ActionSpec(RuleAction.DISMISS, null),
        ),
    )

    @Test
    fun `gson round-trip keeps all five actions in exact order`() {
        val json = gson.toJson(flowRule())
        val back: BlockerRule = gson.fromJson(json, BlockerRule::class.java)

        assertEquals(5, back.actions.size)
        assertEquals(
            listOf(RuleAction.TTS, RuleAction.COPY, RuleAction.DELAY, RuleAction.CLICK_BUTTON, RuleAction.DISMISS),
            back.actions.map { it.type }
        )
    }

    @Test
    fun `gson round-trip keeps params values`() {
        val json = gson.toJson(flowRule())
        val back: BlockerRule = gson.fromJson(json, BlockerRule::class.java)

        val tts = back.actions[0]
        assertEquals("{title}：{text}", tts.params?.get("template")?.asString)

        val copy = back.actions[1]
        assertEquals("TITLE_AND_TEXT", copy.params?.get("mode")?.asString)

        val delay = back.actions[2]
        assertEquals(2000L, delay.params?.get("durationMs")?.asLong)

        val click = back.actions[3]
        assertEquals("回复", click.params?.get("buttonLabel")?.asString)

        assertEquals(null, back.actions[4].params)
    }

    @Test
    fun `gson round-trip through list type keeps flow`() {
        val json = gson.toJson(listOf(flowRule()))
        val type = object : TypeToken<List<BlockerRule>>() {}.type
        val back: List<BlockerRule> = gson.fromJson(json, type)

        assertEquals(1, back.size)
        assertEquals(
            listOf(RuleAction.TTS, RuleAction.COPY, RuleAction.DELAY, RuleAction.CLICK_BUTTON, RuleAction.DISMISS),
            back[0].actions.map { it.type }
        )
    }

    @Test
    fun `legacy json without actions field deserializes to invalid rule`() {
        val legacy = """
            {"id":"legacy","sourcePackages":[{"packageName":"com.a"}],
             "condition":{"mode":"CONTAINS_ANY","includeKeywords":["x"],"excludeKeywords":[]},
             "action":"DISMISS","actionParams":{},"isEnabled":true,"hitCount":0,"createdAt":0}
        """.trimIndent()
        val rule: BlockerRule = gson.fromJson(legacy, BlockerRule::class.java)
        // 旧结构：actions 缺失 → Gson 反序列化为 null → isValid 判无效（测试环境允许过滤清空）
        assertFalse(rule.isValid)
    }

    // ---------- ActionSpec.isValid ----------

    @Test
    fun `click button requires non-blank label`() {
        assertFalse(ActionSpec(RuleAction.CLICK_BUTTON, null).isValid)
        assertFalse(ActionSpec(RuleAction.CLICK_BUTTON, ClickButtonParams("").toParamsJson()).isValid)
        assertFalse(ActionSpec(RuleAction.CLICK_BUTTON, ClickButtonParams("  ").toParamsJson()).isValid)
        assertTrue(ActionSpec(RuleAction.CLICK_BUTTON, ClickButtonParams("回复").toParamsJson()).isValid)
    }

    @Test
    fun `delay requires positive duration`() {
        assertFalse(ActionSpec(RuleAction.DELAY, null).isValid)
        assertFalse(ActionSpec(RuleAction.DELAY, DelayParams(0).toParamsJson()).isValid)
        assertFalse(ActionSpec(RuleAction.DELAY, DelayParams(-5).toParamsJson()).isValid)
        assertTrue(ActionSpec(RuleAction.DELAY, DelayParams(1000).toParamsJson()).isValid)
    }

    @Test
    fun `parameterless actions are always valid`() {
        assertTrue(ActionSpec(RuleAction.DISMISS, null).isValid)
        assertTrue(ActionSpec(RuleAction.SILENT, null).isValid)
        assertTrue(ActionSpec(RuleAction.OPEN_NOTIFICATION, null).isValid)
        assertTrue(ActionSpec(RuleAction.TTS, null).isValid)
        assertTrue(ActionSpec(RuleAction.COPY, null).isValid)
    }

    // ---------- BlockerRule.isValid ----------

    @Test
    fun `rule invalid when actions empty`() {
        val rule = BlockerRule(
            sourcePackages = listOf(SourceApp("com.a")),
            actions = emptyList(),
        )
        assertFalse(rule.isValid)
    }

    @Test
    fun `rule invalid when any action invalid`() {
        val rule = BlockerRule(
            sourcePackages = listOf(SourceApp("com.a")),
            actions = listOf(
                ActionSpec(RuleAction.TTS, null),
                ActionSpec(RuleAction.CLICK_BUTTON, ClickButtonParams("").toParamsJson()),
            ),
        )
        assertFalse(rule.isValid)
    }

    @Test
    fun `rule valid when sources and actions present and valid`() {
        assertTrue(flowRule().isValid)
    }

    // ---------- RuleImport.normalize（经 parse 间接验证） ----------

    private fun importJson(actionsJson: String): String = """
        [
          {
            "id": "imp-1",
            "sourcePackages": [{"packageName": "com.a", "appName": "A"}],
            "condition": {"mode": "CONTAINS_ANY", "includeKeywords": ["x"], "excludeKeywords": []},
            "extraCondition": {"screenState": "ANY", "chargingState": "ANY", "dndState": "ANY",
              "bluetoothState": "ANY", "bluetoothDeviceNames": [], "time": {"enabled": false,
              "startHour": 0, "startMinute": 0, "endHour": 23, "endMinute": 59, "weekdays": []}},
            "actions": $actionsJson,
            "createdAt": 0
          }
        ]
    """.trimIndent()

    @Test
    fun `import drops rule with unknown action type`() {
        // 无参数 action 在 Gson 序列化时省略 params 字段，导入 JSON 用缺省 params 形式（与 RuleExport 输出一致）
        val result = RuleImport.parse(importJson("""[{"type":"MUTE_5S"}]"""))
        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(0, success.rules.size)
        assertEquals(1, success.droppedCount)
    }

    @Test
    fun `import drops rule with blank click button label`() {
        val result = RuleImport.parse(
            importJson("""[{"type":"CLICK_BUTTON","params":{"buttonLabel":"  "}}]""")
        )
        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(0, success.rules.size)
        assertEquals(1, success.droppedCount)
    }

    @Test
    fun `import drops rule with non-positive delay`() {
        val result = RuleImport.parse(
            importJson("""[{"type":"DELAY","params":{"durationMs":0}}]""")
        )
        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(0, success.rules.size)
        assertEquals(1, success.droppedCount)
    }

    @Test
    fun `import keeps valid actions and drops invalid ones`() {
        val result = RuleImport.parse(
            importJson(
                """
                [
                  {"type":"TTS","params":{"template":"{title}"}},
                  {"type":"CLICK_BUTTON","params":{"buttonLabel":""}},
                  {"type":"DELAY","params":{"durationMs":-1}},
                  {"type":"DISMISS"}
                ]
                """.trimIndent()
            )
        )
        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(1, success.rules.size)
        val actions = success.rules[0].actions
        assertEquals(2, actions.size)
        assertEquals(listOf(RuleAction.TTS, RuleAction.DISMISS), actions.map { it.type })
    }

    @Test
    fun `import keeps five-action flow order`() {
        val result = RuleImport.parse(
            importJson(
                """
                [
                  {"type":"TTS","params":{"template":"{title}：{text}"}},
                  {"type":"COPY","params":{"mode":"TITLE_AND_TEXT"}},
                  {"type":"DELAY","params":{"durationMs":2000}},
                  {"type":"CLICK_BUTTON","params":{"buttonLabel":"回复"}},
                  {"type":"DISMISS"}
                ]
                """.trimIndent()
            )
        )
        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(1, success.rules.size)
        assertEquals(0, success.droppedCount)
        assertEquals(
            listOf(RuleAction.TTS, RuleAction.COPY, RuleAction.DELAY, RuleAction.CLICK_BUTTON, RuleAction.DISMISS),
            success.rules[0].actions.map { it.type }
        )
    }

    // ---------- P2-3：DELAY durationMs 安全解析（阶段 4C-C-B） ----------

    @Test
    fun `delay max long is valid`() {
        assertTrue(ActionSpec(RuleAction.DELAY, DelayParams(Long.MAX_VALUE).toParamsJson()).isValid)
    }

    @Test
    fun `delay over-long number invalid without crash`() {
        // Gson 将超 Long 整数字面量解析为 BigDecimal；asLong 会抛 NFE，安全解析必须返回 false
        val spec: ActionSpec = gson.fromJson(
            """{"type":"DELAY","params":{"durationMs":99999999999999999999}}""",
            ActionSpec::class.java
        )
        assertFalse(spec.isValid)
    }

    @Test
    fun `delay non-numeric string invalid without crash`() {
        val spec: ActionSpec = gson.fromJson(
            """{"type":"DELAY","params":{"durationMs":"abc"}}""",
            ActionSpec::class.java
        )
        assertFalse(spec.isValid)
    }

    @Test
    fun `delay numeric string accepted`() {
        val spec: ActionSpec = gson.fromJson(
            """{"type":"DELAY","params":{"durationMs":"2000"}}""",
            ActionSpec::class.java
        )
        assertTrue(spec.isValid)
    }

    @Test
    fun `import keeps valid actions and drops illegal duration ones`() {
        val result = RuleImport.parse(
            importJson(
                """
                [
                  {"type":"TTS","params":{"template":"{title}"}},
                  {"type":"DELAY","params":{"durationMs":99999999999999999999}},
                  {"type":"DELAY","params":{"durationMs":0}},
                  {"type":"DELAY","params":{"durationMs":2000}},
                  {"type":"DISMISS"}
                ]
                """.trimIndent()
            )
        )
        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(1, success.rules.size)
        assertEquals(0, success.droppedCount)
        val actions = success.rules[0].actions
        assertEquals(listOf(RuleAction.TTS, RuleAction.DELAY, RuleAction.DISMISS), actions.map { it.type })
        assertEquals(2000L, actions[1].params?.get("durationMs")?.asLong)
    }

    @Test
    fun `import single illegal duration does not fail whole import`() {
        val result = RuleImport.parse(
            importJson("""[{"type":"DELAY","params":{"durationMs":99999999999999999999}}]""")
        )
        assertTrue("超 Long durationMs 不应导致整个 Import 失败", result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(0, success.rules.size)
        assertEquals(1, success.droppedCount)
    }
}
