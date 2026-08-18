package com.enlpot.notix

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 阶段2D：Service destroy 集成测试（用例 13）。
 *
 * 在 DELAY 等待期间执行 Service 销毁取消语义：验证 activeFlows.cancel() 生效——
 * Flow 被取消、DELAY 到期后不再推进 DISMISS、无新通知操作、无崩溃。
 *
 * 说明：NotificationBlockerService 是系统绑定的 NotificationListenerService，
 * stopService() 不会触发真实 onDestroy（系统绑定保持存活，测试曾实测 DELAY 到期后
 * DISMISS 照常执行）。因此通过 companion 的 internal 测试入口
 * [NotificationBlockerService.cancelActiveFlowsForTest] 执行与 onDestroy 完全一致的
 * 取消逻辑（isDestroyed=true + activeFlows.cancel() + clear），验证取消机制本身有效。
 */
@RunWith(AndroidJUnit4::class)
class ActionFlowDestroyTest : BaseActionFlowTest() {

    @Test
    fun test13_serviceDestroyCancelsFlow() {
        // DELAY 15s（远大于测试销毁窗口）→ DISMISS
        ruleStorage.addRules(listOf(TestRuleFactory.rule(
            listOf(TestRuleFactory.delay(15000), TestRuleFactory.dismiss),
            keywords = listOf("AFT_DESTROY")
        )))
        val id = 5015
        TestNotificationFactory.notify(context, id, TestNotificationFactory.createNotification(context, "AFT_DESTROY title", "body"))
        waitForNotification(id)
        // 等待 Flow 启动并进入 DELAY 等待（3s）
        SystemClock.sleep(3000)
        try {
            // 执行 Service 销毁取消语义（真实 onDestroy 因系统绑定不可由 stopService 触发）
            NotificationBlockerService.cancelActiveFlowsForTest()
            // 等待超过 DELAY 剩余时间（15s + 余量）→ 若 cancel 无效，DISMISS 会被执行、通知消失
            SystemClock.sleep(18000)
            // 核心断言：通知仍存在 → DELAY 到期后未推进 DISMISS，证明 Flow 被取消
            assertNotNull("Service 销毁后 Flow 应被取消，通知应仍存在", findNotification(id))
            // 无崩溃：logcat 中不得出现本应用 FATAL EXCEPTION
            val crashLogs = lastLogcat("AndroidRuntime", lines = 400)
            if (crashLogs.isNotBlank() && crashLogs.contains("FATAL EXCEPTION") && crashLogs.contains("notix")) {
                fail("Service 销毁过程中发生崩溃:\n${crashLogs.takeLast(1200)}")
            }
        } finally {
            // 恢复 Service 可接收新通知（后续用例继续使用）
            NotificationBlockerService.instance?.let { it.isDestroyed = false }
        }
    }
}
