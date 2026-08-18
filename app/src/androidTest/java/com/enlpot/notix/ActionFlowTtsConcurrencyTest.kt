package com.enlpot.notix

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P2-1 / P2-2（阶段 4C-C-B）：TTS 并发隔离集成测试。
 *
 * P2-1：utteranceId 已改为 AtomicLong 自增（tts-1/tts-2/...），两个 Flow 并发 speak
 *       时 callback 不再可能互相覆盖。此处通过两条通知几乎同时发布制造并发窗口：
 *       - Flow1 = TTS → COPY → DISMISS
 *       - Flow2 = TTS → COPY
 *       若 utteranceId 碰撞导致某 Flow callback 被覆盖，该 Flow 将永久等待，
 *       对应断言（通知消失 / 剪贴板更新）会超时失败。
 *
 * P2-2：doSpeak catch 已改为仅移除当前 utteranceId 的 callback（不再 clear() 全部）。
 *       引擎层 error 路径隔离由 JVM 测试覆盖（concurrentTtsErrorAndSuccessAreIsolated /
 *       concurrentTtsSuccessAndErrorAreIsolated）；本测试在真实 TTS + 并发场景验证
 *       两个 Flow 均能各自结束（无论 TTS 成功或 error，FAILED→continue 均继续执行）。
 */
@RunWith(AndroidJUnit4::class)
class ActionFlowTtsConcurrencyTest : BaseActionFlowTest() {

    /** 两 Flow 并发 TTS：A(success/error 均可) → COPY → DISMISS；B(success/error 均可) → COPY */
    @Test
    fun test01_twoConcurrentTtsFlowsBothComplete() {
        ruleStorage.addRules(listOf(
            TestRuleFactory.rule(
                actions = listOf(
                    TestRuleFactory.tts("{text}"),
                    TestRuleFactory.copy(CopyMode.TEXT),
                    TestRuleFactory.dismiss,
                ),
                keywords = listOf("AFT_TTSCON1"),
            ),
            TestRuleFactory.rule(
                actions = listOf(
                    TestRuleFactory.tts("{text}"),
                    TestRuleFactory.copy(CopyMode.TEXT),
                ),
                keywords = listOf("AFT_TTSCON2"),
            ),
        ))
        val id1 = 5061
        val id2 = 5062
        // 两条通知几乎同时发布，两个 Flow 的 TTS speak 落在同一毫秒窗口的概率最大化
        TestNotificationFactory.notify(
            context, id1,
            TestNotificationFactory.createNotification(context, "AFT_TTSCON1 title", "con1 body"),
        )
        TestNotificationFactory.notify(
            context, id2,
            TestNotificationFactory.createNotification(context, "AFT_TTSCON2 title", "con2 body"),
        )
        waitForNotification(id1)
        waitForNotification(id2)

        // Flow1 完整结束：TTS → COPY → DISMISS，通知1 消失（DISMISS 是最后一个动作，只有 Flow1 结束才会执行）
        waitForNotificationGone(id1, 30000)

        // Flow2 完整结束：TTS → COPY，剪贴板最终为 con2 body（Flow2 COPY 后写）。
        // 若 Flow2 的 TTS callback 被 Flow1 覆盖/清除，Flow2 永久等待，剪贴板不会更新
        waitForClipboard("con2 body", 30000)
    }
}
