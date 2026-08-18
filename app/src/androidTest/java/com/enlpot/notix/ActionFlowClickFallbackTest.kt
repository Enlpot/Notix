package com.enlpot.notix

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * P2-4（阶段 4C-C-B）：CLICK_BUTTON contains fallback 行为固化测试。
 *
 * 仅固化既有产品行为，不修改匹配算法。当前算法：
 * 1. 精确匹配 ignoreCase
 * 2. contains fallback（按通知按钮顺序取第一个匹配）
 * 3. 无命中 FAILED → continue
 *
 * TestA：按钮 ["Mark as read","Mark"] label="Mark" → 精确命中第二个按钮
 * TestB：按钮 ["删除并报告","删除全部"] label="删除" → 无精确匹配，contains 按既有顺序命中第一个
 * TestC：按钮 ["删除并报告","删除"] label="删除" → 精确匹配优先，命中"删除"
 * TestD：不存在按钮 → FAILED → continue（后续 COPY 仍执行）
 */
@RunWith(AndroidJUnit4::class)
class ActionFlowClickFallbackTest : BaseActionFlowTest() {

    private class ClickProbeReceiver {
        val latch = CountDownLatch(1)
        @Volatile
        var receivedAction: String? = null
            private set

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                receivedAction = intent?.action
                latch.countDown()
            }
        }

        fun register(context: Context): ClickProbeReceiver {
            ContextCompat.registerReceiver(
                context, receiver,
                IntentFilter(ACTION_A).apply { addAction(ACTION_B) },
                ContextCompat.RECEIVER_EXPORTED
            )
            return this
        }

        fun unregister(context: Context) = runCatching { context.unregisterReceiver(receiver) }

        fun await(timeoutMs: Long): Boolean = latch.await(timeoutMs, TimeUnit.MILLISECONDS)

        fun pendingIntent(context: Context, requestCode: Int, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context, requestCode, Intent(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        companion object {
            const val ACTION_A = "com.enlpot.notix.test.CLICK_A"
            const val ACTION_B = "com.enlpot.notix.test.CLICK_B"
        }
    }

    private fun runClickFlow(
        probe: ClickProbeReceiver,
        buttons: List<Pair<String, String>>,
        label: String,
        id: Int,
        keyword: String,
    ) {
        val piA = probe.pendingIntent(context, id, ClickProbeReceiver.ACTION_A)
        val piB = probe.pendingIntent(context, id + 1000, ClickProbeReceiver.ACTION_B)
        val actions = buttons.map { (title, target) ->
            TestNotificationFactory.actionButton(context, title, if (target == "A") piA else piB)
        }
        ruleStorage.addRules(listOf(TestRuleFactory.rule(
            actions = listOf(TestRuleFactory.click(label), TestRuleFactory.copy(CopyMode.TEXT)),
            keywords = listOf(keyword),
        )))
        TestNotificationFactory.notify(
            context, id,
            TestNotificationFactory.createNotification(context, "$keyword title", "$keyword body", actions),
        )
        waitForNotification(id)
    }

    /** TestA：精确匹配存在（["Mark as read","Mark"] label="Mark" → 命中 "Mark"）→ 点击成功 */
    @Test
    fun testA_preciseMatchClickSucceeds() {
        val probe = ClickProbeReceiver().register(context)
        try {
            runClickFlow(
                probe,
                buttons = listOf("Mark as read" to "A", "Mark" to "B"),
                label = "Mark",
                id = 5051,
                keyword = "AFT_CLICKA",
            )
            assertTrue("CLICK 应命中按钮并触发 receiver", probe.await(20000))
            assertEquals(ClickProbeReceiver.ACTION_B, probe.receivedAction)
            // 后续 COPY 继续执行
            waitForClipboard("AFT_CLICKA body", 15000)
        } finally {
            probe.unregister(context)
        }
    }

    /** TestB：无精确匹配，contains fallback 按既有顺序命中第一个（固化行为，非期望修复） */
    @Test
    fun testB_containsFallbackHitsFirstByOrder() {
        val probe = ClickProbeReceiver().register(context)
        try {
            runClickFlow(
                probe,
                buttons = listOf("删除并报告" to "A", "删除全部" to "B"),
                label = "删除",
                id = 5052,
                keyword = "AFT_CLICKB",
            )
            assertTrue(probe.await(20000))
            // 当前算法：精确无命中 → contains 按按钮顺序取第一个匹配项
            assertEquals(ClickProbeReceiver.ACTION_A, probe.receivedAction)
            waitForClipboard("AFT_CLICKB body", 15000)
        } finally {
            probe.unregister(context)
        }
    }

    /** TestC：精确匹配优先于 contains（["删除并报告","删除"] label="删除" → 命中 "删除"） */
    @Test
    fun testC_preciseMatchTakesPriorityOverContains() {
        val probe = ClickProbeReceiver().register(context)
        try {
            runClickFlow(
                probe,
                buttons = listOf("删除并报告" to "A", "删除" to "B"),
                label = "删除",
                id = 5053,
                keyword = "AFT_CLICKC",
            )
            assertTrue(probe.await(20000))
            assertEquals(ClickProbeReceiver.ACTION_B, probe.receivedAction)
            waitForClipboard("AFT_CLICKC body", 15000)
        } finally {
            probe.unregister(context)
        }
    }

    /** TestD：不存在按钮 → 无命中 → FAILED → continue（后续 COPY 仍执行、无按钮被点击） */
    @Test
    fun testD_noMatchFailsAndContinues() {
        val probe = ClickProbeReceiver().register(context)
        try {
            runClickFlow(
                probe,
                buttons = listOf("Reply" to "A"),
                label = "删除",
                id = 5054,
                keyword = "AFT_CLICKD",
            )
            // 按钮未被点击：probe 在超时内不应触发
            assertFalse("无匹配按钮不应触发任何 CLICK", probe.await(3000))
            // FAILED → continue：后续 COPY 仍执行
            waitForClipboard("AFT_CLICKD body", 15000)
        } finally {
            probe.unregister(context)
        }
    }
}
