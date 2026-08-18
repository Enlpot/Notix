package com.enlpot.notix

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 阶段4C-B P1-1：Action Flow 级防抖（同一 notification key 短窗口防抖）集成测试。
 *
 * 覆盖文档要求的 5 项：
 * 1. 同 key 窗口内连续两次 → 只执行一次
 * 2. 同 key 超窗口 → 允许再次执行
 * 3. 不同 key → 各自执行
 * 4. Service destroy → 防抖状态清理，新生命周期不受旧记录影响
 * 5. SILENT 重发防循环 → 不被新防抖破坏
 *
 * 时间控制：覆盖 NotificationBlockerService.flowDebounceNow 为可控 fakeNow，
 * 不使用真实等待 5 秒；每个用例结束重置回系统时钟。
 */
@RunWith(AndroidJUnit4::class)
class ActionFlowDebounceTest : BaseActionFlowTest() {

    @After
    fun resetDebounceClock() {
        NotificationBlockerService.flowDebounceNow = { System.currentTimeMillis() }
        NotificationBlockerService.instance?.let { it.isDestroyed = false }
    }

    /** Test 1：同一 notification key 在防抖窗口内连续触发两次 → 只执行一次 */
    @Test
    fun test1_sameKeyWithinWindowExecutesOnce() {
        ruleStorage.addRules(listOf(TestRuleFactory.rule(
            listOf(TestRuleFactory.copy(CopyMode.TEXT), TestRuleFactory.dismiss),
            keywords = listOf("AFT_DEBOUNCE1")
        )))
        val id = 5020
        var fakeNow = 10_000L
        NotificationBlockerService.flowDebounceNow = { fakeNow }
        try {
            // 第一次 POST → Flow1 正常执行（COPY "first" → DISMISS）
            TestNotificationFactory.notify(
                context, id,
                TestNotificationFactory.createNotification(context, "AFT_DEBOUNCE1 title", "first")
            )
            waitForClipboard("first", 20000)
            waitForNotificationGone(id, 20000)
            clearClipboard()

            // 窗口内（fakeNow+500ms）同 key UPDATE → 应被 Flow 级防抖跳过
            fakeNow += 500
            TestNotificationFactory.notify(
                context, id,
                TestNotificationFactory.createNotification(context, "AFT_DEBOUNCE1 title", "second")
            )
            // 等待足够时间确认 DISMISS 未执行（若执行通知会消失、COPY 会写 "second"）
            SystemClock.sleep(2500)
            // 通知仍存在（第二次 Flow 被跳过，未 DISMISS）
            assertNotNull("窗口内 UPDATE 不应再次执行 Flow（通知应保持存在）", findNotification(id))
            // 剪贴板未被第二次 COPY 覆盖（clearClipboard 使用哨兵值 AFT_CLEARED，
            // 若第二次 Flow 被执行，剪贴板将被覆盖为 "second"）
            assertTrue(
                "窗口内 UPDATE 不应再次执行 COPY（剪贴板='${readClipboard()}'）",
                readClipboard() != "second",
            )
        } finally {
            NotificationBlockerService.flowDebounceNow = { System.currentTimeMillis() }
        }
    }

    /** Test 2：同一 key 超过防抖窗口后再次触发 → 允许再次执行（共两次） */
    @Test
    fun test2_sameKeyAfterWindowExecutesAgain() {
        ruleStorage.addRules(listOf(TestRuleFactory.rule(
            listOf(TestRuleFactory.copy(CopyMode.TEXT), TestRuleFactory.dismiss),
            keywords = listOf("AFT_DEBOUNCE2")
        )))
        val id = 5021
        var fakeNow = 20_000L
        NotificationBlockerService.flowDebounceNow = { fakeNow }
        try {
            // 第一次 POST → Flow1 执行
            TestNotificationFactory.notify(
                context, id,
                TestNotificationFactory.createNotification(context, "AFT_DEBOUNCE2 title", "first")
            )
            waitForClipboard("first", 20000)
            waitForNotificationGone(id, 20000)
            clearClipboard()

            // 超过窗口（fakeNow+4000 > 3000）→ 允许再次执行
            fakeNow += 4000
            TestNotificationFactory.notify(
                context, id,
                TestNotificationFactory.createNotification(context, "AFT_DEBOUNCE2 title", "second")
            )
            // Flow2 执行：COPY "second" → DISMISS
            waitForClipboard("second", 20000)
            waitForNotificationGone(id, 20000)
        } finally {
            NotificationBlockerService.flowDebounceNow = { System.currentTimeMillis() }
        }
    }

    /** Test 3：不同 notification key 互不影响，各自执行 Flow */
    @Test
    fun test3_differentKeysExecuteIndependently() {
        ruleStorage.addRules(listOf(TestRuleFactory.rule(
            listOf(TestRuleFactory.copy(CopyMode.TEXT), TestRuleFactory.dismiss),
            keywords = listOf("AFT_DEBOUNCE3")
        )))
        val idA = 5022
        val idB = 5023
        var fakeNow = 30_000L
        NotificationBlockerService.flowDebounceNow = { fakeNow }
        try {
            // key-A 与 key-B 在同一时间点触发
            TestNotificationFactory.notify(
                context, idA,
                TestNotificationFactory.createNotification(context, "AFT_DEBOUNCE3 title", "a body")
            )
            TestNotificationFactory.notify(
                context, idB,
                TestNotificationFactory.createNotification(context, "AFT_DEBOUNCE3 title", "b body")
            )
            // 两个 Flow 都执行（均 DISMISS 消失）
            waitForNotificationGone(idA, 20000)
            waitForNotificationGone(idB, 20000)
            // 剪贴板为后执行者（a body 或 b body 均可，证明两个 COPY 都执行过）
            val clip = readClipboard()
            assertTrue("不同 key 应各自执行 COPY，实际剪贴板='$clip'", clip == "a body" || clip == "b body")
        } finally {
            NotificationBlockerService.flowDebounceNow = { System.currentTimeMillis() }
        }
    }

    /** Test 4：Service destroy 清空防抖状态，新生命周期不受旧记录影响 */
    @Test
    fun test4_destroyClearsDebounceState() {
        ruleStorage.addRules(listOf(TestRuleFactory.rule(
            listOf(TestRuleFactory.copy(CopyMode.TEXT), TestRuleFactory.dismiss),
            keywords = listOf("AFT_DEBOUNCE4")
        )))
        val id = 5024
        var fakeNow = 40_000L
        NotificationBlockerService.flowDebounceNow = { fakeNow }
        try {
            // 旧生命周期：POST → Flow1 执行并登记防抖
            TestNotificationFactory.notify(
                context, id,
                TestNotificationFactory.createNotification(context, "AFT_DEBOUNCE4 title", "first")
            )
            waitForClipboard("first", 20000)
            waitForNotificationGone(id, 20000)
            clearClipboard()

            // 模拟 Service destroy（与 onDestroy 一致的取消语义，含防抖清空）
            NotificationBlockerService.cancelActiveFlowsForTest()

            // 新生命周期恢复
            NotificationBlockerService.instance?.isDestroyed = false

            // 同 key、fakeNow 未推进（仍在 3s 窗口内），但防抖 map 已清空 → 应执行
            TestNotificationFactory.notify(
                context, id,
                TestNotificationFactory.createNotification(context, "AFT_DEBOUNCE4 title", "after")
            )
            waitForClipboard("after", 20000)
            waitForNotificationGone(id, 20000)
        } finally {
            NotificationBlockerService.flowDebounceNow = { System.currentTimeMillis() }
        }
    }

    /** Test 5：SILENT 重发防循环不被新增 Flow 防抖破坏 */
    @Test
    fun test5_silentRepostNotBrokenByDebounce() {
        ruleStorage.addRules(listOf(TestRuleFactory.rule(
            listOf(TestRuleFactory.silent, TestRuleFactory.copy(CopyMode.TITLE_AND_TEXT)),
            keywords = listOf("AFT_DEBOUNCE_SILENT")
        )))
        val id = 5025
        var fakeNow = 50_000L
        NotificationBlockerService.flowDebounceNow = { fakeNow }
        try {
            TestNotificationFactory.notify(
                context, id,
                TestNotificationFactory.createNotification(context, "AFT_DEBOUNCE_SILENT 标题", "静默正文")
            )
            // SILENT：原通知取消
            waitForNotificationGone(id, 20000)
            // 重发到 RULE_REPOST_CHANNEL_ID（防递归：repost 通知跳过规则处理）
            waitForRepostNotification("AFT_DEBOUNCE_SILENT 标题", 15000)
            // SILENT 后 COPY 仍执行
            waitForClipboard("AFT_DEBOUNCE_SILENT 标题 静默正文", 15000)
        } finally {
            NotificationBlockerService.flowDebounceNow = { System.currentTimeMillis() }
        }
    }
}
