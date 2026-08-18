package com.enlpot.notix

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.enlpot.notix.ui.screens.RuleWizardScreen
import org.junit.Rule
import org.junit.Test

/**
 * P2-6（阶段 4C-C-B）：OPEN_NOTIFICATION → DISMISS 动态警告 UI 测试。
 *
 * 显示条件表（仅当某个 OPEN_NOTIFICATION 之后存在 DISMISS 时显示）：
 * OPEN→DISMISS 显示 / OPEN→COPY 不显示 / 单独 OPEN 不显示 /
 * COPY→OPEN→DISMISS 显示 / OPEN→TTS→DISMISS 显示 / DISMISS→OPEN 不显示 /
 * CLICK→DISMISS 保留现有警告 / CLICK→OPEN→DISMISS 中 OPEN→DISMISS 警告正常存在。
 * 样式复用 CLICK→DISMISS 警告（不重新设计 UI）。
 */
class RulesScreenFlowWarningTest {

    @get:Rule
    val compose = createComposeRule()

    private val openDismissWarning = "注意：如果无法打开通知对应的页面，后续“消除通知”仍会执行。"
    private val clickDismissWarning = "注意：如果找不到匹配按钮，点击动作会失败，但后续的“消除通知”仍会执行。"

    private fun render(actions: List<ActionSpec>) {
        compose.setContent {
            RuleWizardScreen(
                existingRules = emptyList(),
                pastNotifications = emptyList(),
                onClose = {},
                onCreateRule = {},
                editingRule = TestRuleFactory.rule(actions = actions, keywords = listOf("AFT-warn")),
            )
        }
    }

    // Test 1：OPEN_NOTIFICATION → DISMISS → 显示 OPEN 警告
    @Test
    fun test1_openThenDismiss_showsWarning() {
        render(listOf(
            TestRuleFactory.action(RuleAction.OPEN_NOTIFICATION),
            TestRuleFactory.dismiss,
        ))
        compose.onNodeWithText(openDismissWarning).assertIsDisplayed()
    }

    // Test 2：OPEN_NOTIFICATION → COPY → 不显示 OPEN 警告
    @Test
    fun test2_openThenCopy_noWarning() {
        render(listOf(
            TestRuleFactory.action(RuleAction.OPEN_NOTIFICATION),
            TestRuleFactory.copy(CopyMode.TEXT),
        ))
        compose.onNodeWithText(openDismissWarning).assertDoesNotExist()
    }

    // Test 3：单独 OPEN_NOTIFICATION → 不显示
    @Test
    fun test3_singleOpen_noWarning() {
        render(listOf(TestRuleFactory.action(RuleAction.OPEN_NOTIFICATION)))
        compose.onNodeWithText(openDismissWarning).assertDoesNotExist()
    }

    // Test 4：COPY → OPEN_NOTIFICATION → DISMISS → 显示
    @Test
    fun test4_copyOpenDismiss_showsWarning() {
        render(listOf(
            TestRuleFactory.copy(CopyMode.TEXT),
            TestRuleFactory.action(RuleAction.OPEN_NOTIFICATION),
            TestRuleFactory.dismiss,
        ))
        compose.onNodeWithText(openDismissWarning).assertIsDisplayed()
    }

    // Test 5：OPEN_NOTIFICATION → TTS → DISMISS → 显示（跨异步 Action）
    @Test
    fun test5_openTtsDismiss_showsWarning() {
        render(listOf(
            TestRuleFactory.action(RuleAction.OPEN_NOTIFICATION),
            TestRuleFactory.tts("{title}"),
            TestRuleFactory.dismiss,
        ))
        compose.onNodeWithText(openDismissWarning).assertIsDisplayed()
    }

    // Test 6：DISMISS → OPEN_NOTIFICATION → 不因为 OPEN 后无 DISMISS 而显示
    @Test
    fun test6_dismissThenOpen_noWarning() {
        render(listOf(
            TestRuleFactory.dismiss,
            TestRuleFactory.action(RuleAction.OPEN_NOTIFICATION),
        ))
        compose.onNodeWithText(openDismissWarning).assertDoesNotExist()
    }

    // Test 7：CLICK_BUTTON → DISMISS → 现有 CLICK 警告保留
    @Test
    fun test7_clickThenDismiss_clickWarningStillShown() {
        render(listOf(
            TestRuleFactory.click("Mark as read"),
            TestRuleFactory.dismiss,
        ))
        compose.onNodeWithText(clickDismissWarning).assertIsDisplayed()
    }

    // Test 8：CLICK_BUTTON → OPEN_NOTIFICATION → DISMISS：OPEN 警告正常存在
    // （CLICK 后也存在 DISMISS，按既有 CLICK 警告语义同样显示；文档只要求 OPEN 警告存在）
    @Test
    fun test8_clickOpenDismiss_openWarningShown() {
        render(listOf(
            TestRuleFactory.click("Mark"),
            TestRuleFactory.action(RuleAction.OPEN_NOTIFICATION),
            TestRuleFactory.dismiss,
        ))
        compose.onNodeWithText(openDismissWarning).assertIsDisplayed()
    }

    // Test 9：OPEN_NOTIFICATION → DISMISS 为合法组合 → 保存正常
    @Test
    fun test9_openDismiss_canSave() {
        render(listOf(
            TestRuleFactory.action(RuleAction.OPEN_NOTIFICATION),
            TestRuleFactory.dismiss,
        ))
        compose.onNodeWithContentDescription("保存").assertIsEnabled()
    }
}
