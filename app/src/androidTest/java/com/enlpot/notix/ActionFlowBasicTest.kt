package com.enlpot.notix

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 阶段2D：Action Flow 集成测试（基础场景 1-8）。
 *
 * 全部用例从 Service 入口触发（真实 Notification → onNotificationPosted →
 * RuleMatcher → ActionFlowExecutor），不绕过 Service 直接调 Executor。
 */
@RunWith(AndroidJUnit4::class)
class ActionFlowBasicTest : BaseActionFlowTest() {

    /** 1. 单 DISMISS：真实通知 → Service → 命中 → DISMISS → 原通知被取消 */
    @Test
    fun test01_singleDismiss() {
        ruleStorage.addRules(listOf(TestRuleFactory.rule(listOf(TestRuleFactory.dismiss), keywords = listOf("AFT_DISMISS"))))
        val id = 5001
        TestNotificationFactory.notify(context, id, TestNotificationFactory.createNotification(context, "AFT_DISMISS title", "body"))
        waitForNotification(id)
        // DISMISS 执行后通知从通知栏消失
        waitForNotificationGone(id, 20000)
        val logs = lastLogcat("NotificationBlockerService")
        if (logs.isNotBlank()) {
            assertTrue("日志应包含 ActionFlow start", logs.contains("ActionFlow start"))
        }
    }

    /** 2. COPY→DISMISS：顺序正确且剪贴板内容最终正确（TITLE_AND_TEXT） */
    @Test
    fun test02_copyThenDismiss() {
        ruleStorage.addRules(listOf(TestRuleFactory.rule(
            listOf(TestRuleFactory.copy(CopyMode.TITLE_AND_TEXT), TestRuleFactory.dismiss),
            keywords = listOf("AFT_COPY")
        )))
        val id = 5002
        TestNotificationFactory.notify(context, id, TestNotificationFactory.createNotification(context, "AFT_COPY 标题", "内容"))
        waitForNotificationGone(id, 20000)
        // COPY 在 DISMISS 之前执行 → 剪贴板 = 标题 + 空格 + 正文
        waitForClipboard("AFT_COPY 标题 内容", 15000)
    }

    /** 3. TTS→COPY：TTS 异步——speak() 后 COPY 不立即执行，onDone/onError 后才执行 */
    @Test
    fun test03_ttsThenCopy() {
        ruleStorage.addRules(listOf(TestRuleFactory.rule(
            listOf(TestRuleFactory.tts("{title}：{text}"), TestRuleFactory.copy(CopyMode.TEXT)),
            keywords = listOf("AFT_TTS")
        )))
        val id = 5003
        TestNotificationFactory.notify(context, id, TestNotificationFactory.createNotification(context, "AFT_TTS title", "tts body"))
        // 无论真实 TTS onDone 还是 onError，COPY 都必须在 TTS Action 结束信号后才执行
        waitForClipboard("tts body", 30000)
    }

    /** 4. TTS 防抖(模拟 onError 路径)→COPY：TTS FAILED 后 COPY 仍执行，Flow 不终止 */
    @Test
    fun test04_ttsErrorThenCopy() {
        ruleStorage.addRules(listOf(TestRuleFactory.rule(
            listOf(TestRuleFactory.tts(null), TestRuleFactory.copy(CopyMode.TEXT)),
            keywords = listOf("AFT_TTSERR")
        )))
        val id = 5004
        val n = TestNotificationFactory.createNotification(context, "AFT_TTSERR title", "first")
        TestNotificationFactory.notify(context, id, n)
        // Flow1：TTS（真实播报或 error）→ COPY
        waitForClipboard("first", 30000)
        clearClipboard()
        // 阶段4C-B P1-1：Action Flow 级防抖窗口为 3s——同 id UPDATE 若在窗口内会被
        // Flow 级防抖整体跳过。先等 3.5s 越过 Flow 防抖窗口，让 Flow2 正常进入；
        // 此时 TTS 5s 防抖（同 postTime）仍可能命中（以失败回调结束）或走真实 error 路径，
        // 两者均验证“TTS FAILED 后 COPY 仍执行、Flow 不终止”。
        SystemClock.sleep(3500)
        // 同 id UPDATE（postTime 不变）→ 5s 防抖窗口内命中 → TTS 以失败回调结束 → COPY 仍执行
        TestNotificationFactory.notify(context, id, n)
        waitForClipboard("first", 30000)
    }

    /** 5. DELAY→DISMISS：DELAY 2000ms 完成前原通知仍存在、完成后 DISMISS 执行 */
    @Test
    fun test05_delayThenDismiss() {
        ruleStorage.addRules(listOf(TestRuleFactory.rule(
            listOf(TestRuleFactory.delay(2000), TestRuleFactory.dismiss),
            keywords = listOf("AFT_DELAY")
        )))
        val id = 5005
        TestNotificationFactory.notify(context, id, TestNotificationFactory.createNotification(context, "AFT_DELAY title", "body"))
        waitForNotification(id)
        // DELAY 完成前（1.2s < 2s）通知应仍存在
        SystemClock.sleep(1200)
        assertNotNull("DELAY 完成前通知应仍存在", findNotification(id))
        // DELAY 到期后 DISMISS 执行 → 通知消失
        waitForNotificationGone(id, 10000)
    }

    /** 6. CLICK_BUTTON 命中：匹配真实 Notification.Action，PendingIntent.send() 被触发，Flow 继续 */
    @Test
    fun test06_clickButtonHit() {
        val receiver = TestPendingIntentReceiver().register(context)
        try {
            val pi = receiver.pendingIntent(context, 6006)
            val action = TestNotificationFactory.actionButton(context, "Mark as read", pi)
            ruleStorage.addRules(listOf(TestRuleFactory.rule(
                listOf(TestRuleFactory.click("Mark as read"), TestRuleFactory.copy(CopyMode.TEXT)),
                keywords = listOf("AFT_CLICK")
            )))
            val id = 5006
            TestNotificationFactory.notify(context, id, TestNotificationFactory.createNotification(context, "AFT_CLICK title", "clicked body", listOf(action)))
            // CLICK_BUTTON 匹配并 send() → BroadcastReceiver 收到
            assertTrue("CLICK_BUTTON 应触发 PendingIntent", receiver.await(20000))
            // Flow 继续下一 Action：COPY 执行
            waitForClipboard("clicked body", 15000)
        } finally {
            receiver.unregister(context)
        }
    }

    /** 7. CLICK_BUTTON 不存在：按钮匹配失败 → FAILED 记录 → COPY 仍执行，Flow 不终止 */
    @Test
    fun test07_clickButtonMissing() {
        val receiver = TestPendingIntentReceiver().register(context)
        try {
            val pi = receiver.pendingIntent(context, 6007)
            val replyAction = TestNotificationFactory.actionButton(context, "Reply", pi)
            ruleStorage.addRules(listOf(TestRuleFactory.rule(
                listOf(TestRuleFactory.click("Mark as read"), TestRuleFactory.copy(CopyMode.TEXT)),
                keywords = listOf("AFT_CLICKMISS")
            )))
            val id = 5007
            TestNotificationFactory.notify(context, id, TestNotificationFactory.createNotification(context, "AFT_CLICKMISS title", "no click body", listOf(replyAction)))
            // "Mark as read" 不存在 → 不应触发 Reply 的 PendingIntent
            assertFalse("不应触发未匹配按钮的 PendingIntent", receiver.await(3000))
            // CLICK_BUTTON FAILED 后 COPY 仍执行
            waitForClipboard("no click body", 15000)
        } finally {
            receiver.unregister(context)
        }
    }

    /** 8. SILENT→COPY：原通知取消、rule_repost 低打扰频道重发、重发后 COPY 执行 */
    @Test
    fun test08_silentThenCopy() {
        ruleStorage.addRules(listOf(TestRuleFactory.rule(
            listOf(TestRuleFactory.silent, TestRuleFactory.copy(CopyMode.TITLE_AND_TEXT)),
            keywords = listOf("AFT_SILENT")
        )))
        val id = 5008
        TestNotificationFactory.notify(context, id, TestNotificationFactory.createNotification(context, "AFT_SILENT 标题", "静默正文"))
        // SILENT 第一步：原通知被取消
        waitForNotificationGone(id, 20000)
        // 重发通知出现（低打扰 rule_repost 频道，标题保留）
        waitForRepostNotification("AFT_SILENT 标题", 15000)
        // SILENT 后 COPY 执行
        waitForClipboard("AFT_SILENT 标题 静默正文", 15000)
    }
}
