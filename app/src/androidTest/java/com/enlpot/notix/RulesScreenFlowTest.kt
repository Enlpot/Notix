package com.enlpot.notix

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import com.enlpot.notix.ui.screens.RulesScreen
import com.enlpot.notix.ui.screens.RuleWizardScreen
import org.junit.Rule
import org.junit.Test

/**
 * 阶段3B：RulesScreen 完整 Action Flow 展示 + RuleWizardScreen 编辑恢复
 * （Compose UI 测试，复用项目已有 compose-ui-test 依赖；项目为 Instrumentation
 * 集成测试基建，无独立 UI 测试基类，直接用 createComposeRule 渲染目标 Screen）。
 */
class RulesScreenFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private val ttsNoTemplate = TestRuleFactory.tts(null)
    private val copyTitleAndText = TestRuleFactory.copy(CopyMode.TITLE_AND_TEXT)
    private val ttsWithTemplate = TestRuleFactory.tts("{title}：{text}")
    private val delay2s = TestRuleFactory.delay(2000L)
    private val clickMarkAsRead = TestRuleFactory.click("Mark as read")
    private val dismiss = TestRuleFactory.dismiss

    // Test 6：规则 TTS→COPY→DISMISS 进 RulesScreen，验证完整 Flow 摘要存在（不能只显示 TTS）
    @Test
    fun test6_rulesScreen_shows_full_flow_tts_copy_dismiss() {
        val rule = TestRuleFactory.rule(
            actions = listOf(ttsNoTemplate, copyTitleAndText, dismiss),
            keywords = listOf("AFT-flow"),
        )
        compose.setContent {
            RulesScreen(
                rules = listOf(rule),
                onRuleClick = {},
                onCreateRuleClick = {},
                onToggleAllRules = {},
            )
        }

        // 完整 Flow 摘要：播报通知标题和正文 → 标题 + 正文 → 消除通知
        compose.onNodeWithText("播报通知标题和正文 → 标题 + 正文 → 消除通知").assertExists()
    }

    // Test 7：规则 TTS→COPY→DELAY→CLICK_BUTTON→DISMISS 验证 RulesScreen 体现多 Action Flow
    @Test
    fun test7_rulesScreen_shows_multi_action_flow_with_truncation_and_count() {
        val rule = TestRuleFactory.rule(
            actions = listOf(ttsNoTemplate, copyTitleAndText, delay2s, clickMarkAsRead, dismiss),
            keywords = listOf("AFT-flow5"),
        )
        compose.setContent {
            RulesScreen(
                rules = listOf(rule),
                onRuleClick = {},
                onCreateRuleClick = {},
                onToggleAllRules = {},
            )
        }

        // 长 Flow 截断：前 3 个 Action + "…"
        compose.onNodeWithText("播报通知标题和正文 → 标题 + 正文 → 等待 2 秒 → …").assertExists()
        // 总数提示
        compose.onNodeWithText("共 5 个动作").assertExists()
    }

    // Test 8：进入 Rule 编辑，验证 RuleWizardScreen 完整恢复 5 个 Action（数量 + 摘要 + 参数）
    @Test
    fun test8_ruleWizard_restores_five_action_flow() {
        val rule = TestRuleFactory.rule(
            actions = listOf(
                ttsWithTemplate,
                copyTitleAndText,
                delay2s,
                clickMarkAsRead,
                dismiss,
            ),
            keywords = listOf("AFT-edit5"),
        )
        compose.setContent {
            RuleWizardScreen(
                existingRules = emptyList(),
                pastNotifications = emptyList(),
                onClose = {},
                onCreateRule = {},
                editingRule = rule,
            )
        }

        // 5 个 Action 卡片，每张都有"编辑"按钮
        compose.onAllNodesWithText("编辑").assertCountEquals(5)
        // 每个 Action 的参数摘要均恢复（类型 + 参数）
        compose.onNodeWithText("播报：{title}：{text}").assertExists()
        compose.onNodeWithText("标题 + 正文").assertExists()
        compose.onNodeWithText("等待 2 秒").assertExists()
        compose.onNodeWithText("点击：Mark as read").assertExists()
        // 卡片名称与摘要均为"消除通知"（阶段4B 统一后），断言至少存在
        compose.onAllNodesWithText("消除通知").onFirst().assertExists()
    }
}
