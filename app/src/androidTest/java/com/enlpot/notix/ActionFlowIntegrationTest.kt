package com.enlpot.notix

import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 阶段2D：Action Flow 集成测试（完整流程 / 并发 / 系统广播入口 9-12）。
 */
@RunWith(AndroidJUnit4::class)
class ActionFlowIntegrationTest : BaseActionFlowTest() {

    /** 9. 完整 Flow：TTS→COPY→DELAY→CLICK_BUTTON→DISMISS，从 Service 入口完整启动 */
    @Test
    fun test09_fullFlow() {
        val receiver = TestPendingIntentReceiver().register(context)
        try {
            val pi = receiver.pendingIntent(context, 6009)
            val action = TestNotificationFactory.actionButton(context, "Mark as read", pi)
            ruleStorage.addRules(listOf(TestRuleFactory.rule(
                listOf(
                    TestRuleFactory.tts("{title}"),
                    TestRuleFactory.copy(CopyMode.TEXT),
                    TestRuleFactory.delay(1000),
                    TestRuleFactory.click("Mark as read"),
                    TestRuleFactory.dismiss
                ),
                keywords = listOf("AFT_FULL")
            )))
            val id = 5009
            TestNotificationFactory.notify(context, id, TestNotificationFactory.createNotification(context, "AFT_FULL title", "full body", listOf(action)))
            waitForNotification(id)
            // COPY 先执行（TTS 结束后）
            waitForClipboard("full body", 30000)
            // CLICK_BUTTON 执行
            assertTrue("完整 Flow 中 CLICK_BUTTON 应触发 PendingIntent", receiver.await(20000))
            // DISMISS 最后执行 → 通知消失
            waitForNotificationGone(id, 20000)
        } finally {
            receiver.unregister(context)
        }
    }

    /** 10. 并发 Flow：通知 A(COPY→DELAY→DISMISS) 与 B(COPY→DISMISS) 接近同时触发，状态无串扰 */
    @Test
    fun test10_concurrentFlows() {
        ruleStorage.addRules(listOf(
            TestRuleFactory.rule(
                listOf(TestRuleFactory.copy(CopyMode.TEXT), TestRuleFactory.delay(1500), TestRuleFactory.dismiss),
                keywords = listOf("AFT_CONC_A")
            ),
            TestRuleFactory.rule(
                listOf(TestRuleFactory.copy(CopyMode.TEXT), TestRuleFactory.dismiss),
                keywords = listOf("AFT_CONC_B")
            )
        ))
        val idA = 5010
        val idB = 5011
        // 接近同时触发
        TestNotificationFactory.notify(context, idA, TestNotificationFactory.createNotification(context, "AFT_CONC_A title", "flow a body"))
        TestNotificationFactory.notify(context, idB, TestNotificationFactory.createNotification(context, "AFT_CONC_B title", "flow b body"))
        // B 的 COPY→DISMISS 更快完成：剪贴板最终为 B（COPY 覆盖，证明 B Flow 完整执行）
        waitForClipboard("flow b body", 20000)
        // A 的 DELAY 到期后 DISMISS A（不被 B 串扰），B 已消失
        waitForNotificationGone(idA, 20000)
        waitForNotificationGone(idB, 20000)
    }

    /** 11. ACTION_APPLY_RULE：只传 rule.id → Service → RuleStorage 读完整 Rule → 参数不丢失 */
    @Test
    fun test11_applyRuleRestoresFullActions() {
        val receiver = TestPendingIntentReceiver().register(context)
        try {
            val pi = receiver.pendingIntent(context, 6011)
            val buttonAction = TestNotificationFactory.actionButton(context, "Mark as read", pi)
            val rule = TestRuleFactory.rule(
                listOf(
                    TestRuleFactory.click("Mark as read"),
                    TestRuleFactory.tts("{title}：{text}"),
                    TestRuleFactory.delay(1000),
                    TestRuleFactory.copy(CopyMode.TITLE_AND_TEXT)
                ),
                keywords = listOf("AFT_APPLY")
            )
            ruleStorage.addRules(listOf(rule))
            val id = 5012
            TestNotificationFactory.notify(context, id, TestNotificationFactory.createNotification(context, "AFT_APPLY 标题", "应用正文", listOf(buttonAction)))
            waitForNotification(id)
            // 阶段4C-B P1-1：POST 时 Flow1 已执行并登记 Action Flow 防抖，
            // ACTION_APPLY_RULE 回溯若在 3s 窗口内会被整体跳过。先等 3.5s 越过窗口，
            // 确保 apply 回溯真正重新执行完整 rule.actions（验证 rule.id → 完整参数恢复）。
            SystemClock.sleep(3500)
            // 模拟真实回溯：Intent 只带 rule.id
            val intent = Intent(context, NotificationBlockerService::class.java).apply {
                action = NotificationBlockerService.ACTION_APPLY_RULE
                putExtra(NotificationBlockerService.EXTRA_RULE_JSON, rule.id)
            }
            context.startService(intent)
            // CLICK_BUTTON buttonLabel 参数恢复（非空）→ 匹配并触发
            assertTrue("ACTION_APPLY_RULE 应恢复 CLICK_BUTTON 参数并触发", receiver.await(20000))
            // COPY copyMode 参数恢复（TITLE_AND_TEXT）→ 剪贴板 = 标题 + 正文
            waitForClipboard("AFT_APPLY 标题 应用正文", 20000)
        } finally {
            receiver.unregister(context)
        }
    }

    /** 12. ACTION_RESCAN_ALL：多条规则 + 多个测试通知，执行完整 rule.actions（非 firstOrNull） */
    @Test
    fun test12_rescanAllExecutesFullActions() {
        val idA = 5013
        val idB = 5014
        // 先发布通知（此时无规则，不被处理）
        TestNotificationFactory.notify(context, idA, TestNotificationFactory.createNotification(context, "AFT_RESCAN_A title", "rescan a body"))
        TestNotificationFactory.notify(context, idB, TestNotificationFactory.createNotification(context, "AFT_RESCAN_B title", "rescan b body"))
        waitForNotification(idA)
        waitForNotification(idB)
        // 建立两条规则（均 COPY→DISMISS 两个 Action，验证 rescan 执行完整 actions 而非 firstOrNull）
        ruleStorage.addRules(listOf(
            TestRuleFactory.rule(
                listOf(TestRuleFactory.copy(CopyMode.TEXT), TestRuleFactory.dismiss),
                keywords = listOf("AFT_RESCAN_A")
            ),
            TestRuleFactory.rule(
                listOf(TestRuleFactory.copy(CopyMode.TEXT), TestRuleFactory.dismiss),
                keywords = listOf("AFT_RESCAN_B")
            )
        ))
        context.startService(Intent(context, NotificationBlockerService::class.java).apply {
            action = NotificationBlockerService.ACTION_RESCAN_ALL
        })
        // 两条规则的完整 actions 均执行：COPY（剪贴板最终为后执行者 B 的正文）→ DISMISS（两通知均消失）
        waitForClipboard("rescan b body", 20000)
        waitForNotificationGone(idA, 20000)
        waitForNotificationGone(idB, 20000)
    }
}
