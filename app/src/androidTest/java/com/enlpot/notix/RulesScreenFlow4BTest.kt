package com.enlpot.notix

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.enlpot.notix.ui.screens.RuleWizardScreen
import org.junit.Rule
import org.junit.Test

/**
 * 阶段4B：UI 文案统一与用户误解风险提示的 Compose UI 验证。
 * 复用 RulesScreenFlowTest 的 createComposeRule 基建；只验证 UI 展示层，
 * 不修改任何执行逻辑。
 */
class RulesScreenFlow4BTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(rule: BlockerRule) {
        compose.setContent {
            RuleWizardScreen(
                existingRules = emptyList(),
                pastNotifications = emptyList(),
                onClose = {},
                onCreateRule = {},
                editingRule = rule,
            )
        }
    }

    // 1. DELAY 参数区显示 ms / "1000 ms = 1 秒" / "等待指定时长后再执行下一个动作"
    @Test
    fun test1_delay_param_area_shows_ms_and_wait_hints() {
        render(TestRuleFactory.rule(actions = listOf(TestRuleFactory.delay(2000L)), keywords = listOf("4B-delay")))
        compose.onNodeWithText("编辑").performClick()
        compose.onNodeWithText("等待 2 秒").assertExists()
        compose.onNodeWithText("ms").assertExists()
        compose.onNodeWithText("等待指定时长后再执行下一个动作").assertExists()
        compose.onNodeWithText("例如：1000 ms = 1 秒").assertExists()
    }

    // 2. TTS 参数区显示"播报完成后才会继续下一个动作" + 失败继续提示
    @Test
    fun test2_tts_param_area_shows_wait_and_fail_hints() {
        render(TestRuleFactory.rule(actions = listOf(TestRuleFactory.tts(null)), keywords = listOf("4B-tts")))
        compose.onNodeWithText("编辑").performClick()
        compose.onNodeWithText("播报完成后才会继续下一个动作").assertExists()
        compose.onNodeWithText("播报失败也会继续执行后续动作").assertExists()
    }

    // 3. CLICK_BUTTON 参数区显示失败继续提示
    @Test
    fun test3_click_button_param_area_shows_fail_hint() {
        render(TestRuleFactory.rule(actions = listOf(TestRuleFactory.click("Mark as read")), keywords = listOf("4B-click")))
        compose.onNodeWithText("编辑").performClick()
        compose.onNodeWithText("找不到匹配按钮时，此动作会标记为失败，但仍会继续执行后续动作。").assertExists()
    }

    // 4. CLICK_BUTTON → DISMISS 显示组合风险提示
    @Test
    fun test4_click_then_dismiss_shows_risk_warning() {
        render(
            TestRuleFactory.rule(
                actions = listOf(TestRuleFactory.click("Mark as read"), TestRuleFactory.dismiss),
                keywords = listOf("4B-click-dismiss"),
            )
        )
        compose.onNodeWithText("注意：如果找不到匹配按钮，点击动作会失败，但后续的“消除通知”仍会执行。").assertExists()
    }

    // 5. CLICK_BUTTON → COPY 不显示 DISMISS 风险提示
    @Test
    fun test5_click_then_copy_has_no_dismiss_warning() {
        render(
            TestRuleFactory.rule(
                actions = listOf(TestRuleFactory.click("Mark as read"), TestRuleFactory.copy(CopyMode.TITLE)),
                keywords = listOf("4B-click-copy"),
            )
        )
        compose.onNodeWithText("注意：如果找不到匹配按钮", substring = true).assertDoesNotExist()
    }

    // 6. SILENT 显示"静默重显"及描述
    @Test
    fun test6_silent_shows_name_and_desc() {
        render(TestRuleFactory.rule(actions = listOf(TestRuleFactory.silent), keywords = listOf("4B-silent")))
        // 卡片名称 + 卡片摘要均为"静默重显"
        compose.onAllNodesWithText("静默重显").assertCountEquals(2)
        compose.onNodeWithText("编辑").performClick()
        compose.onNodeWithText("取消原通知，并以低打扰方式重新显示。").assertExists()
    }

    // 7. DISMISS 统一显示"消除通知"，不再出现"清除通知"
    @Test
    fun test7_dismiss_unified_label() {
        render(TestRuleFactory.rule(actions = listOf(TestRuleFactory.dismiss), keywords = listOf("4B-dismiss")))
        compose.onAllNodesWithText("消除通知").assertCountEquals(2)
        compose.onAllNodesWithText("清除通知").assertCountEquals(0)
    }

    // 8. OPEN_NOTIFICATION 描述改为"打开通知对应的页面。"（阶段 4C-C-B P3-4 文案调整）
    @Test
    fun test8_open_notification_desc() {
        render(
            TestRuleFactory.rule(
                actions = listOf(TestRuleFactory.action(RuleAction.OPEN_NOTIFICATION)),
                keywords = listOf("4B-open"),
            )
        )
        compose.onNodeWithText("编辑").performClick()
        compose.onNodeWithText("打开通知对应的页面。").assertExists()
    }

    // 9. Action Flow 区显示顺序提示
    @Test
    fun test9_action_flow_order_hint() {
        render(
            TestRuleFactory.rule(
                actions = listOf(TestRuleFactory.dismiss, TestRuleFactory.copy(CopyMode.TITLE)),
                keywords = listOf("4B-order"),
            )
        )
        compose.onNodeWithText("动作将按从上到下的顺序依次执行").assertExists()
    }

    // 10. ActionPickerDialog 显示统一后的 7 个 Action 名称
    @Test
    fun test10_picker_shows_unified_action_names() {
        render(TestRuleFactory.rule(actions = listOf(TestRuleFactory.dismiss), keywords = listOf("4B-picker")))
        compose.onNodeWithText("+ 添加动作").performClick()
        compose.onAllNodesWithText("消除通知").onFirst().assertExists()
        compose.onNodeWithText("静默重显").assertExists()
        compose.onNodeWithText("点击按钮").assertExists()
        compose.onNodeWithText("打开通知").assertExists()
        compose.onNodeWithText("复制内容").assertExists()
        compose.onNodeWithText("播报").assertExists()
        compose.onNodeWithText("等待").assertExists()
    }
}
