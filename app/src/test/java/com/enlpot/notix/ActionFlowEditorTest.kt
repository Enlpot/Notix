package com.enlpot.notix

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 阶段3A：Action Flow 编辑器 JVM 单测。
 *
 * UI 维护唯一 List<ActionSpec>，所有转换走 RuleWizardSupport 纯函数；
 * “保存 / 重新打开”通过 Gson round-trip（与 RuleStorage 持久化同一序列化链路）
 * 验证 UI 状态与最终 Rule.actions 的 顺序 / 参数 100% 一致。
 */
class ActionFlowEditorTest {

    private val gson = Gson()

    /** 模拟「保存 Rule」：BlockerRule.actions = UI 的 list，再序列化。 */
    private fun saveRule(actions: List<ActionSpec>): String {
        val rule = BlockerRule(
            id = "ui-rule",
            sourcePackages = listOf(SourceApp("com.test.app", "Test")),
            condition = RuleCondition(includeKeywords = listOf("k")),
            actions = actions,
        )
        return gson.toJson(rule)
    }

    /** 模拟「从 RuleStorage 重新读取」：反序列化 List<BlockerRule>（与 RuleStorage 相同链路）。 */
    private fun reloadRule(json: String): BlockerRule {
        val type = object : TypeToken<BlockerRule>() {}.type
        return gson.fromJson(json, type)
    }

    // Test 1：创建多 Action
    @Test
    fun `test1 create multi action tts copy dismiss saves size 3 in order`() {
        var flow = emptyList<ActionSpec>()
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.TTS)
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.COPY)
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.DISMISS)

        assertEquals(3, flow.size)
        assertEquals(
            listOf(RuleAction.TTS, RuleAction.COPY, RuleAction.DISMISS),
            flow.map { it.type }
        )
        assertTrue(flow.all { it.type != null && it.isValid })

        // 保存后 Rule.actions 与 UI 顺序一致
        val rule = reloadRule(saveRule(flow))
        assertEquals(3, rule.actions.size)
        assertEquals(
            listOf(RuleAction.TTS, RuleAction.COPY, RuleAction.DISMISS),
            rule.actions.map { it.type }
        )
    }

    // Test 2：删除 Action
    @Test
    fun `test2 delete copy leaves tts dismiss unchanged`() {
        var flow = emptyList<ActionSpec>()
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.TTS)
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.COPY)
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.DISMISS)

        flow = RuleWizardSupport.actionFlowRemoveAt(flow, 1)

        assertEquals(2, flow.size)
        assertEquals(
            listOf(RuleAction.TTS, RuleAction.DISMISS),
            flow.map { it.type }
        )
        // 其余 Action 的类型/参数不变
        assertTrue(flow[1].type == RuleAction.DISMISS && flow[1].params == null)
    }

    // Test 3：上移
    @Test
    fun `test3 move copy up to first position`() {
        var flow = emptyList<ActionSpec>()
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.TTS)
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.COPY)
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.DISMISS)

        assertTrue(RuleWizardSupport.canMoveUp(1, flow.size))
        flow = RuleWizardSupport.actionFlowMoveUp(flow, 1)

        assertEquals(
            listOf(RuleAction.COPY, RuleAction.TTS, RuleAction.DISMISS),
            flow.map { it.type }
        )
    }

    // Test 4：下移
    @Test
    fun `test4 move tts down becomes copy tts dismiss`() {
        var flow = emptyList<ActionSpec>()
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.TTS)
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.COPY)
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.DISMISS)

        assertTrue(RuleWizardSupport.canMoveDown(0, flow.size))
        flow = RuleWizardSupport.actionFlowMoveDown(flow, 0)

        assertEquals(
            listOf(RuleAction.COPY, RuleAction.TTS, RuleAction.DISMISS),
            flow.map { it.type }
        )
    }

    // Test 5：编辑 DELAY 1000 → 2000，保存重开 durationMs == 2000
    @Test
    fun `test5 edit delay 1000 to 2000 persists on reopen`() {
        var flow = RuleWizardSupport.actionFlowAdd(emptyList(), RuleAction.DELAY)
        assertEquals(1000L, flow[0].params?.get("durationMs")?.asLong)

        flow = RuleWizardSupport.actionFlowUpdate(flow, 0, RuleWizardSupport.delaySpec(2000L))
        // 编辑只更新 index 0，不复制出第二个 Action
        assertEquals(1, flow.size)
        assertEquals(2000L, flow[0].params?.get("durationMs")?.asLong)

        val rule = reloadRule(saveRule(flow))
        assertEquals(1, rule.actions.size)
        assertEquals(RuleAction.DELAY, rule.actions[0].type)
        assertEquals(2000L, rule.actions[0].params?.get("durationMs")?.asLong)
    }

    // Test 6：CLICK_BUTTON buttonLabel="Mark as read" 保存重开一致
    @Test
    fun `test6 click button label persists on reopen`() {
        var flow = RuleWizardSupport.actionFlowAdd(emptyList(), RuleAction.CLICK_BUTTON)
        flow = RuleWizardSupport.actionFlowUpdate(
            flow, 0, RuleWizardSupport.clickButtonSpec("Mark as read")
        )
        assertEquals("Mark as read", flow[0].params?.get("buttonLabel")?.asString)

        val rule = reloadRule(saveRule(flow))
        assertEquals("Mark as read", rule.actions[0].params?.get("buttonLabel")?.asString)
    }

    // Test 7：TTS template 修改保存重开完全一致
    @Test
    fun `test7 tts template persists exactly on reopen`() {
        var flow = RuleWizardSupport.actionFlowAdd(emptyList(), RuleAction.TTS)
        flow = RuleWizardSupport.actionFlowUpdate(
            flow, 0, RuleWizardSupport.ttsSpec("{app} 来消息了：{text}")
        )
        val template = flow[0].params?.get("template")?.asString
        assertEquals("{app} 来消息了：{text}", template)

        val rule = reloadRule(saveRule(flow))
        assertEquals("{app} 来消息了：{text}", rule.actions[0].params?.get("template")?.asString)
    }

    // Test 8：删除全部 → actions.isEmpty() 且保存按钮 disabled
    @Test
    fun `test8 delete all actions empty and cannot save`() {
        var flow = emptyList<ActionSpec>()
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.TTS)
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.DELAY)
        flow = RuleWizardSupport.actionFlowRemoveAt(flow, 0)
        flow = RuleWizardSupport.actionFlowRemoveAt(flow, 0)

        assertTrue(flow.isEmpty())
        assertFalse(RuleWizardSupport.canSaveFlow(flow))
    }

    // Test 9：编辑已有多 Action Rule（TTS→COPY→DELAY→CLICK_BUTTON→DISMISS）全部恢复且参数正确
    @Test
    fun `test9 editing existing five action rule restores all actions and params`() {
        val saved = saveRule(
            listOf(
                ActionSpec(RuleAction.TTS, TtsParams("{title}：{text}").toParamsJson()),
                ActionSpec(RuleAction.COPY, CopyParams(CopyMode.TITLE).toParamsJson()),
                ActionSpec(RuleAction.DELAY, DelayParams(1500L).toParamsJson()),
                ActionSpec(RuleAction.CLICK_BUTTON, ClickButtonParams("回复").toParamsJson()),
                ActionSpec(RuleAction.DISMISS, null),
            )
        )
        // 打开编辑：从 Rule.actions 完整恢复（UI 初始化即 Rule.actions）
        var flow = reloadRule(saved).actions

        assertEquals(5, flow.size)
        assertEquals(
            listOf(
                RuleAction.TTS, RuleAction.COPY, RuleAction.DELAY,
                RuleAction.CLICK_BUTTON, RuleAction.DISMISS,
            ),
            flow.map { it.type }
        )
        assertEquals("{title}：{text}", flow[0].params?.get("template")?.asString)
        assertEquals("TITLE", flow[1].params?.get("mode")?.asString)
        assertEquals(1500L, flow[2].params?.get("durationMs")?.asLong)
        assertEquals("回复", flow[3].params?.get("buttonLabel")?.asString)
        assertEquals(null, flow[4].params)

        // 修改其中一个（DELAY 1500→3000）不影响其他 Action
        flow = RuleWizardSupport.actionFlowUpdate(flow, 2, RuleWizardSupport.delaySpec(3000L))
        assertEquals(5, flow.size)
        assertEquals(3000L, flow[2].params?.get("durationMs")?.asLong)
        assertEquals("{title}：{text}", flow[0].params?.get("template")?.asString)
        assertEquals("回复", flow[3].params?.get("buttonLabel")?.asString)

        // 保存再打开：顺序和参数完全一致
        val reopened = reloadRule(saveRule(flow))
        assertEquals(
            listOf(
                RuleAction.TTS, RuleAction.COPY, RuleAction.DELAY,
                RuleAction.CLICK_BUTTON, RuleAction.DISMISS,
            ),
            reopened.actions.map { it.type }
        )
        assertEquals(3000L, reopened.actions[2].params?.get("durationMs")?.asLong)
        assertEquals("{title}：{text}", reopened.actions[0].params?.get("template")?.asString)
        assertEquals("TITLE", reopened.actions[1].params?.get("mode")?.asString)
        assertEquals("回复", reopened.actions[3].params?.get("buttonLabel")?.asString)
    }

    // Test 10：COPY→DELAY→TTS→DISMISS 保存后重新读取顺序与参数完全一致
    @Test
    fun `test10 save copy delay tts dismiss then reload from storage keeps order and params`() {
        var flow = emptyList<ActionSpec>()
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.COPY)
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.DELAY)
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.TTS)
        flow = RuleWizardSupport.actionFlowAdd(flow, RuleAction.DISMISS)
        flow = RuleWizardSupport.actionFlowUpdate(flow, 1, RuleWizardSupport.delaySpec(2500L))
        flow = RuleWizardSupport.actionFlowUpdate(
            flow, 2, RuleWizardSupport.ttsSpec("新消息：{title}")
        )
        flow = RuleWizardSupport.actionFlowUpdate(flow, 0, RuleWizardSupport.copySpec(CopyMode.TEXT))

        // 保存 → 从 RuleStorage 语义重新读取（Gson List round-trip）
        val json = gson.toJson(listOf(
            BlockerRule(
                id = "storage-rule",
                sourcePackages = listOf(SourceApp("com.test.app", "Test")),
                condition = RuleCondition(includeKeywords = listOf("k")),
                actions = flow,
            )
        ))
        val type = object : TypeToken<List<BlockerRule>>() {}.type
        val loaded: List<BlockerRule> = gson.fromJson(json, type)
        val actions = loaded.single().actions

        assertEquals(4, actions.size)
        assertEquals(
            listOf(RuleAction.COPY, RuleAction.DELAY, RuleAction.TTS, RuleAction.DISMISS),
            actions.map { it.type }
        )
        assertEquals("TEXT", actions[0].params?.get("mode")?.asString)
        assertEquals(2500L, actions[1].params?.get("durationMs")?.asLong)
        assertEquals("新消息：{title}", actions[2].params?.get("template")?.asString)
        assertEquals(null, actions[3].params)
    }
}
