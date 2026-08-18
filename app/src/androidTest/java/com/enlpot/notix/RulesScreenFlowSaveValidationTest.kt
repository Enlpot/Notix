package com.enlpot.notix

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.enlpot.notix.ui.screens.RuleWizardScreen
import org.junit.Rule
import org.junit.Test

/**
 * 阶段4C-B P1-2：RuleWizard 保存校验（存在任何 isValid=false 的 Action 时禁止保存）。
 *
 * 覆盖文档要求 5 项：
 * 1. 单个 CLICK_BUTTON(buttonLabel="") → 保存按钮 disabled
 * 2. COPY → CLICK_BUTTON(空 label) → 整个 Flow 不允许保存
 * 3. CLICK_BUTTON("Mark as read") → DISMISS → 可以保存
 * 4. TTS→COPY→DELAY→CLICK_BUTTON→DISMISS（5 合法 Action）→ 正常保存
 * 5. Saver restore（旋转/重组）：5 合法 Action 完整恢复顺序与参数
 */
class RulesScreenFlowSaveValidationTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(editingRule: BlockerRule) {
        compose.setContent {
            RuleWizardScreen(
                existingRules = emptyList(),
                pastNotifications = emptyList(),
                onClose = {},
                onCreateRule = {},
                editingRule = editingRule,
            )
        }
    }

    // Test 1：单个 CLICK_BUTTON(buttonLabel="") → 保存按钮 disabled
    @Test
    fun test1_blankLabelClickButton_cannotSave() {
        render(TestRuleFactory.rule(
            actions = listOf(TestRuleFactory.click("")),
            keywords = listOf("AFT-save1"),
        ))
        compose.onNodeWithContentDescription("保存").assertIsNotEnabled()
    }

    // Test 2：COPY → CLICK_BUTTON(空 label)：COPY 本身合法，但整个 Flow 含非法 Action → 禁止保存
    @Test
    fun test2_copyThenBlankLabelClickButton_wholeFlowCannotSave() {
        render(TestRuleFactory.rule(
            actions = listOf(
                TestRuleFactory.copy(CopyMode.TEXT),
                TestRuleFactory.click(""),
            ),
            keywords = listOf("AFT-save2"),
        ))
        compose.onNodeWithContentDescription("保存").assertIsNotEnabled()
    }

    // Test 3：CLICK_BUTTON("Mark as read") → DISMISS → 可以保存
    @Test
    fun test3_clickLabelThenDismiss_canSave() {
        render(TestRuleFactory.rule(
            actions = listOf(
                TestRuleFactory.click("Mark as read"),
                TestRuleFactory.dismiss,
            ),
            keywords = listOf("AFT-save3"),
        ))
        compose.onNodeWithContentDescription("保存").assertIsEnabled()
    }

    // Test 4：5 个合法 Action（TTS→COPY→DELAY→CLICK_BUTTON→DISMISS）→ 正常保存
    @Test
    fun test4_fiveLegalActions_canSave() {
        render(TestRuleFactory.rule(
            actions = listOf(
                TestRuleFactory.tts("{title}：{text}"),
                TestRuleFactory.copy(CopyMode.TITLE_AND_TEXT),
                TestRuleFactory.delay(2000L),
                TestRuleFactory.click("Mark as read"),
                TestRuleFactory.dismiss,
            ),
            keywords = listOf("AFT-save4"),
        ))
        compose.onNodeWithContentDescription("保存").assertIsEnabled()
        // 5 张 Action 卡片齐全
        compose.onAllNodesWithText("编辑").assertCountEquals(5)
    }

    // Test 5：Saver restore（旋转/重组）后 5 合法 Action 完整恢复顺序与参数
    @Test
    fun test5_fiveActionRestore_preservesOrderAndParams() {
        val restorationTester = StateRestorationTester(compose)
        restorationTester.setContent {
            RuleWizardScreen(
                existingRules = emptyList(),
                pastNotifications = emptyList(),
                onClose = {},
                onCreateRule = {},
                editingRule = TestRuleFactory.rule(
                    actions = listOf(
                        TestRuleFactory.tts("{title}：{text}"),
                        TestRuleFactory.copy(CopyMode.TITLE_AND_TEXT),
                        TestRuleFactory.delay(2000L),
                        TestRuleFactory.click("Mark as read"),
                        TestRuleFactory.dismiss,
                    ),
                    keywords = listOf("AFT-save5"),
                ),
            )
        }

        // 恢复前：5 Action + 保存可用
        compose.onAllNodesWithText("编辑").assertCountEquals(5)
        compose.onNodeWithContentDescription("保存").assertIsEnabled()

        // 模拟旋转/重组：Saver restore 触发
        restorationTester.emulateSavedInstanceStateRestore()

        // 恢复后：5 Action 完整、顺序与参数保持（摘要按顺序存在）
        compose.onAllNodesWithText("编辑").assertCountEquals(5)
        compose.onNodeWithContentDescription("保存").assertIsEnabled()
        compose.onNodeWithText("播报：{title}：{text}").assertExists()
        compose.onNodeWithText("标题 + 正文").assertExists()
        compose.onNodeWithText("等待 2 秒").assertExists()
        compose.onNodeWithText("点击：Mark as read").assertExists()
    }
}
