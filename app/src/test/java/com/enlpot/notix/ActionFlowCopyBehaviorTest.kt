package com.enlpot.notix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P2-5（阶段 4C-C-B）：COPY 空内容行为固化测试。
 *
 * 仅固化既有产品语义，不修改 COPY 执行逻辑：
 * - 有内容就复制（TITLE / TEXT / TITLE_AND_TEXT）
 * - 全空时不写剪贴板
 * - Action 仍 SUCCESS
 * - 后续 Action 继续
 *
 * 使用生产执行体 RealSyncActionRunner + FakeHost（COPY 仅依赖 host.copyToClipboard，
 * 不触碰 Android 运行时），ActionFlowExecutor 走真实 SyncActionRunner 验证组合链路。
 */
class ActionFlowCopyBehaviorTest {

    // ---------- Fakes ----------

    private class FakeHost : ActionFlowHost {
        val copiedTexts = mutableListOf<String>()
        val dismissedKeys = mutableListOf<String>()
        val snoozedKeys = mutableListOf<String>()

        override fun cancelNotificationCompat(key: String) {
            dismissedKeys.add(key)
        }

        override fun snoozeNotificationCompat(key: String) {
            snoozedKeys.add(key)
        }

        override fun copyToClipboard(text: String) {
            copiedTexts.add(text)
        }

        override fun buildTtsText(template: String?, app: String?, title: String?, text: String?, postTime: Long): String =
            template ?: ""

        override fun speakTts(ctx: ActionContext, text: String, onDone: (Boolean) -> Unit) {
            onDone(true)
        }
    }

    private class FakeAsyncRunner : AsyncActionRunner {
        val delayPending = mutableListOf<Pair<Long, () -> Unit>>()

        override fun runTts(ctx: ActionContext, spec: ActionSpec, onDone: (Boolean) -> Unit) {
            throw UnsupportedOperationException("not used in this test")
        }

        override fun runDelay(delayMs: Long, onComplete: () -> Unit) {
            delayPending.add(delayMs to onComplete)
        }

        fun fireDelay(index: Int = 0) {
            delayPending[index].second()
        }
    }

    // ---------- helpers ----------

    private fun ctx(title: String?, text: String?, key: String = "key-1") = ActionContext(
        ruleId = "rule-copy",
        packageName = "com.test.app",
        appName = "TestApp",
        title = title,
        text = text,
        notificationKey = key,
        postTime = 1000L,
    )

    private fun copySpec(mode: CopyMode) = ActionSpec(
        RuleAction.COPY, CopyParams(mode).toParamsJson()
    )

    private fun runFlow(
        host: FakeHost,
        actions: List<ActionSpec>,
        title: String?,
        text: String?,
        key: String = "key-1",
    ): FlowExecution {
        val sync = RealSyncActionRunner(host)
        val async = FakeAsyncRunner()
        val ex = ActionFlowExecutor(sync, async, log = {}, hostAlive = { true })
        return ex.execute(actions, ctx(title, text, key))
    }

    /** TestA：TITLE 模式，title 非空 / text 空 → 复制 title */
    @Test
    fun testA_titleModeCopiesTitleOnly() {
        val host = FakeHost()
        val flow = runFlow(host, listOf(copySpec(CopyMode.TITLE)), title = "标题", text = null)

        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flow.result?.status)
        assertEquals(listOf("标题"), host.copiedTexts)
    }

    /** TestB：TEXT 模式，title 空 / text 非空 → 复制 text */
    @Test
    fun testB_textModeCopiesTextOnly() {
        val host = FakeHost()
        val flow = runFlow(host, listOf(copySpec(CopyMode.TEXT)), title = null, text = "正文")

        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flow.result?.status)
        assertEquals(listOf("正文"), host.copiedTexts)
    }

    /** TestC：TITLE_AND_TEXT 双非空 → 正常拼接（title + 空格 + text） */
    @Test
    fun testC_titleAndTextBothNonEmpty() {
        val host = FakeHost()
        val flow = runFlow(host, listOf(copySpec(CopyMode.TITLE_AND_TEXT)), title = "标题", text = "正文")

        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flow.result?.status)
        assertEquals(listOf("标题 正文"), host.copiedTexts)
    }

    /** TestD：TITLE_AND_TEXT 双空 → 不写剪贴板、仍 SUCCESS、Flow 正常结束 */
    @Test
    fun testD_titleAndTextBothEmpty_noWriteStillSuccess() {
        val host = FakeHost()
        val flow = runFlow(host, listOf(copySpec(CopyMode.TITLE_AND_TEXT)), title = null, text = null)

        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flow.result?.status)
        assertEquals(0, flow.failedActions.size)
        assertTrue("全空时不得调用剪贴板写入", host.copiedTexts.isEmpty())
    }

    /** TestE：COPY → DISMISS 组合，空内容 → 不写剪贴板且 DISMISS 仍执行 */
    @Test
    fun testE_copyThenDismiss_emptyContent_dismissStillRuns() {
        val host = FakeHost()
        val flow = runFlow(
            host,
            listOf(copySpec(CopyMode.TITLE_AND_TEXT), ActionSpec(RuleAction.DISMISS)),
            title = null,
            text = null,
        )

        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flow.result?.status)
        assertTrue(host.copiedTexts.isEmpty())
        assertEquals(listOf("key-1"), host.dismissedKeys)
    }

    /** TestF：COPY → DELAY 组合，空内容 → 不写剪贴板、DELAY 正常进入等待并完成 */
    @Test
    fun testF_copyThenDelay_emptyContent_delayStillRuns() {
        val host = FakeHost()
        val sync = RealSyncActionRunner(host)
        val async = FakeAsyncRunner()
        val ex = ActionFlowExecutor(sync, async, log = {}, hostAlive = { true })
        val flow = ex.execute(
            listOf(copySpec(CopyMode.TITLE_AND_TEXT), ActionSpec(RuleAction.DELAY, DelayParams(1000).toParamsJson())),
            ctx(null, null),
        )

        assertTrue("COPY 空内容不阻塞 DELAY 进入等待", host.copiedTexts.isEmpty())
        assertEquals(1, async.delayPending.size)
        assertEquals(1000L, async.delayPending[0].first)
        assertFalse(flow.isCompleted)

        async.fireDelay()
        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flow.result?.status)
        assertTrue(host.copiedTexts.isEmpty())
    }
}
