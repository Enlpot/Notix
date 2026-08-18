package com.enlpot.notix

import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ActionFlowExecutor 核心执行引擎 JVM 单测（阶段 2B）。
 *
 * 全部使用 Fake 注入（FakeSyncRunner / FakeAsyncRunner），不依赖 Android 运行时：
 * - DELAY 用 Fake 手动触发到期，不真实 sleep；
 * - TTS 用 Fake 手动触发 onDone/onError，验证等待与防重复推进；
 * - 覆盖严格串行 / 失败继续 / TTS 等待 / DELAY 等待 / 重复回调 / 并发隔离 / 取消 / params==null。
 */
class ActionFlowExecutorTest {

    // ============ Fakes ============

    private class FakeSyncRunner : SyncActionRunner {
        val calls = mutableListOf<RuleAction>()
        var clickThrows = false

        override fun dismiss(ctx: ActionContext) {
            calls.add(RuleAction.DISMISS)
        }

        override fun silent(ctx: ActionContext) {
            calls.add(RuleAction.SILENT)
        }

        override fun clickButton(ctx: ActionContext, spec: ActionSpec) {
            calls.add(RuleAction.CLICK_BUTTON)
            if (clickThrows) throw IllegalStateException("button not found")
        }

        override fun openNotification(ctx: ActionContext) {
            calls.add(RuleAction.OPEN_NOTIFICATION)
        }

        override fun copy(ctx: ActionContext, spec: ActionSpec) {
            calls.add(RuleAction.COPY)
        }
    }

    private class PendingTts(val spec: ActionSpec, val callback: (Boolean) -> Unit)
    private class FakeAsyncRunner : AsyncActionRunner {
        val ttsPending = mutableListOf<PendingTts>()
        val delayPending = mutableListOf<Pair<Long, () -> Unit>>()

        override fun runTts(ctx: ActionContext, spec: ActionSpec, onDone: (Boolean) -> Unit) {
            ttsPending.add(PendingTts(spec, onDone))
        }

        override fun runDelay(delayMs: Long, onComplete: () -> Unit) {
            delayPending.add(delayMs to onComplete)
        }

        fun fireTts(success: Boolean, index: Int = 0) {
            ttsPending[index].callback(success)
        }

        fun fireDelay(index: Int = 0) {
            delayPending[index].second()
        }
    }

    // ============ helpers ============

    private fun ctx(key: String = "key-1") = ActionContext(
        ruleId = "rule-1",
        packageName = "com.test.app",
        appName = "TestApp",
        title = "标题",
        text = "正文",
        notificationKey = key,
        postTime = 1000L,
    )

    private fun spec(type: RuleAction, params: JsonObject? = null) = ActionSpec(type, params)
    private fun ttsSpec() = spec(RuleAction.TTS, TtsParams("{title}").toParamsJson())
    private fun copySpec() = spec(RuleAction.COPY, CopyParams(CopyMode.TITLE_AND_TEXT).toParamsJson())
    private fun delaySpec(ms: Long) = spec(RuleAction.DELAY, DelayParams(ms).toParamsJson())

    private fun newExecutor(
        sync: FakeSyncRunner = FakeSyncRunner(),
        async: FakeAsyncRunner = FakeAsyncRunner(),
    ) = ActionFlowExecutor(sync, async) { }

    // ============ 用例 1-15 ============

    /** 1. 空 Flow：不执行、正常结束（EMPTY） */
    @Test
    fun emptyFlowCompletesNormally() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)
        val flow = ex.execute(emptyList(), ctx())

        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.EMPTY, flow.result?.status)
        assertEquals(0, flow.result?.executedCount)
        assertTrue(flow.failedActions.isEmpty())
        assertTrue(sync.calls.isEmpty())
        assertEquals(0, async.ttsPending.size)
        assertEquals(0, async.delayPending.size)
    }

    /** 2. 单个同步 Action（DISMISS） */
    @Test
    fun singleSyncAction() {
        val sync = FakeSyncRunner()
        val ex = newExecutor(sync)
        val flow = ex.execute(listOf(spec(RuleAction.DISMISS)), ctx())

        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flow.result?.status)
        assertEquals(listOf(RuleAction.DISMISS), sync.calls)
        assertEquals(1, flow.result?.executedCount)
    }

    /** 3. 多个同步 Action 顺序：COPY 先于 DISMISS */
    @Test
    fun syncActionsRunInOrder() {
        val sync = FakeSyncRunner()
        val ex = newExecutor(sync)
        val flow = ex.execute(listOf(copySpec(), spec(RuleAction.DISMISS)), ctx())

        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flow.result?.status)
        assertEquals(listOf(RuleAction.COPY, RuleAction.DISMISS), sync.calls)
    }

    /** 4. 严格顺序：Fake 记录 [A,B,C] */
    @Test
    fun strictOrderOfThreeSyncActions() {
        val sync = FakeSyncRunner()
        val ex = newExecutor(sync)
        val flow = ex.execute(
            listOf(
                spec(RuleAction.OPEN_NOTIFICATION),
                copySpec(),
                spec(RuleAction.DISMISS),
            ),
            ctx()
        )

        assertTrue(flow.isCompleted)
        assertEquals(
            listOf(RuleAction.OPEN_NOTIFICATION, RuleAction.COPY, RuleAction.DISMISS),
            sync.calls
        )
        assertTrue(flow.failedActions.isEmpty())
    }

    /** 5. 中间 Action 失败：A成功 → B失败 → C仍执行 */
    @Test
    fun middleFailureDoesNotBlockLaterActions() {
        val sync = FakeSyncRunner().apply { clickThrows = true }
        val ex = newExecutor(sync)
        val flow = ex.execute(
            listOf(copySpec(), spec(RuleAction.CLICK_BUTTON, ClickButtonParams("回复").toParamsJson()), spec(RuleAction.DISMISS)),
            ctx()
        )

        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.PARTIAL_FAILURE, flow.result?.status)
        assertEquals(
            listOf(RuleAction.COPY, RuleAction.CLICK_BUTTON, RuleAction.DISMISS),
            sync.calls
        )
        assertEquals(1, flow.failedActions.size)
        val failure = flow.failedActions[0]
        assertEquals(1, failure.index)
        assertEquals(RuleAction.CLICK_BUTTON, failure.type)
        assertTrue(failure.reason.isNotBlank())
    }

    /** 6. TTS onDone 前 COPY 不执行，onDone 后才执行 */
    @Test
    fun ttsBlocksNextUntilOnDone() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)
        val flow = ex.execute(listOf(ttsSpec(), copySpec()), ctx())

        // TTS 挂起：COPY 未执行
        assertEquals(1, async.ttsPending.size)
        assertFalse(flow.isCompleted)
        assertTrue(sync.calls.isEmpty())

        // onDone 后 COPY 执行
        async.fireTts(true)
        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flow.result?.status)
        assertEquals(listOf(RuleAction.COPY), sync.calls)
    }

    /** 7. TTS onError 后 COPY 仍执行（失败继续） */
    @Test
    fun ttsErrorStillContinues() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)
        val flow = ex.execute(listOf(ttsSpec(), copySpec()), ctx())

        async.fireTts(false)
        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.PARTIAL_FAILURE, flow.result?.status)
        assertEquals(listOf(RuleAction.COPY), sync.calls)
        assertEquals(1, flow.failedActions.size)
        assertEquals(RuleAction.TTS, flow.failedActions[0].type)
    }

    /** 8. TTS 重复回调（onDone+onError / onDone+onDone）：下一 Action 只执行一次 */
    @Test
    fun ttsDuplicateCallbacksAdvanceOnce() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)
        val flow = ex.execute(listOf(ttsSpec(), copySpec()), ctx())

        async.fireTts(true)
        async.fireTts(false)
        async.fireTts(true)

        assertTrue(flow.isCompleted)
        assertEquals(listOf(RuleAction.COPY), sync.calls)
        assertEquals(FlowStatus.SUCCESS, flow.result?.status)
        assertEquals(0, flow.failedActions.size)
    }

    /** 8b. 重复回调不产生多余失败记录 */
    @Test
    fun ttsDuplicateCallbacksKeepSingleFailureWhenErrorFirst() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)
        val flow = ex.execute(listOf(ttsSpec(), copySpec()), ctx())

        async.fireTts(false)
        async.fireTts(false)
        async.fireTts(true)

        assertEquals(listOf(RuleAction.COPY), sync.calls)
        assertEquals(1, flow.failedActions.size)
        assertEquals(FlowStatus.PARTIAL_FAILURE, flow.result?.status)
    }

    /** 9. DELAY 到期前 COPY 不执行，到期后执行（Fake 手动触发，不真实 sleep） */
    @Test
    fun delayBlocksNextUntilExpired() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)
        val flow = ex.execute(listOf(delaySpec(2000), copySpec()), ctx())

        assertEquals(1, async.delayPending.size)
        assertEquals(2000L, async.delayPending[0].first)
        assertFalse(flow.isCompleted)
        assertTrue(sync.calls.isEmpty())

        async.fireDelay()
        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flow.result?.status)
        assertEquals(listOf(RuleAction.COPY), sync.calls)
    }

    /** 10. DELAY durationMs=0 → FAILED 且 COPY 仍执行 */
    @Test
    fun delayZeroFailsAndContinues() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)
        val flow = ex.execute(listOf(delaySpec(0), copySpec()), ctx())

        assertEquals(0, async.delayPending.size) // 未进入真实等待
        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.PARTIAL_FAILURE, flow.result?.status)
        assertEquals(listOf(RuleAction.COPY), sync.calls)
        assertEquals(1, flow.failedActions.size)
        assertEquals(RuleAction.DELAY, flow.failedActions[0].type)
    }

    /** 11. CLICK_BUTTON 找不到按钮（抛异常）→ FAILED 且 DISMISS 仍执行 */
    @Test
    fun clickButtonFailureContinuesToDismiss() {
        val sync = FakeSyncRunner().apply { clickThrows = true }
        val ex = newExecutor(sync)
        val flow = ex.execute(
            listOf(spec(RuleAction.CLICK_BUTTON, ClickButtonParams("回复").toParamsJson()), spec(RuleAction.DISMISS)),
            ctx()
        )

        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.PARTIAL_FAILURE, flow.result?.status)
        assertEquals(listOf(RuleAction.CLICK_BUTTON, RuleAction.DISMISS), sync.calls)
        assertEquals(1, flow.failedActions.size)
        assertEquals(RuleAction.CLICK_BUTTON, flow.failedActions[0].type)
    }

    /** 12. params==null：各 Action 不崩溃；需要参数的 FAILED 继续、不需要的正常执行 */
    @Test
    fun paramsNullNeverCrashes() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)
        val flow = ex.execute(
            listOf(
                spec(RuleAction.DISMISS, null),
                spec(RuleAction.SILENT, null),
                spec(RuleAction.OPEN_NOTIFICATION, null),
                spec(RuleAction.COPY, null),
                spec(RuleAction.TTS, null),
                spec(RuleAction.DELAY, null),
                spec(RuleAction.CLICK_BUTTON, null),
            ),
            ctx()
        )

        assertTrue(flow.isCompleted)
        assertEquals(FlowStatus.PARTIAL_FAILURE, flow.result?.status)
        // 不需要参数的动作正常执行
        assertEquals(
            listOf(RuleAction.DISMISS, RuleAction.SILENT, RuleAction.OPEN_NOTIFICATION),
            sync.calls
        )
        // 需要参数的动作全部 FAILED
        assertEquals(
            listOf(RuleAction.COPY, RuleAction.TTS, RuleAction.DELAY, RuleAction.CLICK_BUTTON),
            flow.failedActions.map { it.type }
        )
        assertEquals(0, async.ttsPending.size)
        assertEquals(0, async.delayPending.size)
    }

    /** 13. 两个 Flow 并发：index/状态互不污染 */
    @Test
    fun concurrentFlowsAreIsolated() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)

        val flow1 = ex.execute(listOf(ttsSpec(), spec(RuleAction.DISMISS)), ctx("key-1"))
        val flow2 = ex.execute(listOf(ttsSpec(), copySpec()), ctx("key-2"))

        // 两个 Flow 都挂在 TTS，无同步动作执行
        assertEquals(2, async.ttsPending.size)
        assertFalse(flow1.isCompleted)
        assertFalse(flow2.isCompleted)

        // 只完成 flow1 的 TTS → 仅 flow1 推进
        async.fireTts(true, 0)
        assertEquals(listOf(RuleAction.DISMISS), sync.calls)
        assertTrue(flow1.isCompleted)
        assertFalse(flow2.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flow1.result?.status)

        // 完成 flow2 的 TTS → 仅 flow2 推进
        async.fireTts(true, 1)
        assertEquals(listOf(RuleAction.DISMISS, RuleAction.COPY), sync.calls)
        assertTrue(flow2.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flow2.result?.status)
        assertTrue(flow2.failedActions.isEmpty())
    }

    /** 14. 取消 Flow（DELAY 期间 cancel，DISMISS 不执行） */
    @Test
    fun cancelDuringDelayStopsRemainingActions() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)
        val flow = ex.execute(listOf(delaySpec(1000), spec(RuleAction.DISMISS)), ctx())

        assertEquals(1, async.delayPending.size)
        assertFalse(flow.isCancelled)

        flow.cancel()
        assertTrue(flow.isCancelled)
        assertEquals(FlowStatus.CANCELLED, flow.result?.status)

        // 到期回调触发也不推进
        async.fireDelay()
        assertTrue(sync.calls.isEmpty())
        assertFalse(flow.isCompleted)
    }

    /** 15. Service destroy/cancel 后（TTS 挂起中取消）后续 Action 不再执行 */
    @Test
    fun cancelledAfterTtsDoesNotContinue() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)
        val flow = ex.execute(listOf(ttsSpec(), spec(RuleAction.DISMISS)), ctx())

        flow.cancel()
        async.fireTts(true)

        assertTrue(flow.isCancelled)
        assertEquals(FlowStatus.CANCELLED, flow.result?.status)
        assertTrue(sync.calls.isEmpty())
        assertFalse(flow.isCompleted)
    }

    /** 15b. 取消后 DELAY 到期也不推进后续（TTS 不会启动、DISMISS 不执行） */
    @Test
    fun cancelBeforeAnyAsyncCompletionStopsEverything() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)
        val flow = ex.execute(listOf(delaySpec(500), ttsSpec(), spec(RuleAction.DISMISS)), ctx())

        flow.cancel()
        async.fireDelay()

        // DELAY 到期回调被忽略：不再推进到 TTS / DISMISS
        assertEquals(0, async.ttsPending.size)
        assertTrue(sync.calls.isEmpty())
        assertEquals(0, flow.result?.executedCount)
    }

    // ---------- P2-1/P2-2：TTS utteranceId 独立性 / catch 定向移除（阶段 4C-C-B） ----------
    // 引擎层验证：两个并发 Flow 的 TTS callback 互不覆盖、各自独立结束。
    // 与既有 concurrentFlowsAreIsolated（A success / B success）互补，覆盖 error 路径。

    /** A error / B success：A 失败继续，B 仍收到 callback 正常结束（B 不因 A 异常永久等待） */
    @Test
    fun concurrentTtsErrorAndSuccessAreIsolated() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)

        val flowA = ex.execute(listOf(ttsSpec(), spec(RuleAction.DISMISS)), ctx("key-a"))
        val flowB = ex.execute(listOf(ttsSpec(), copySpec()), ctx("key-b"))

        // 两个 Flow 都挂在 TTS
        assertEquals(2, async.ttsPending.size)
        assertFalse(flowA.isCompleted)
        assertFalse(flowB.isCompleted)

        // A 的 TTS error → 仅 A 失败继续（DISMISS 执行），B 的 callback 不受影响
        async.fireTts(false, 0)
        assertTrue(flowA.isCompleted)
        assertEquals(FlowStatus.PARTIAL_FAILURE, flowA.result?.status)
        assertEquals(listOf(RuleAction.DISMISS), sync.calls)
        assertFalse(flowB.isCompleted)

        // B 仍能收到自己的 callback 并正常结束
        async.fireTts(true, 1)
        assertTrue(flowB.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flowB.result?.status)
        assertEquals(listOf(RuleAction.DISMISS, RuleAction.COPY), sync.calls)
        assertTrue(flowB.failedActions.isEmpty())
    }

    /** A success / B error：A 正常结束，B 失败继续，互不覆盖 */
    @Test
    fun concurrentTtsSuccessAndErrorAreIsolated() {
        val sync = FakeSyncRunner()
        val async = FakeAsyncRunner()
        val ex = newExecutor(sync, async)

        val flowA = ex.execute(listOf(ttsSpec(), spec(RuleAction.DISMISS)), ctx("key-a"))
        val flowB = ex.execute(listOf(ttsSpec(), copySpec()), ctx("key-b"))

        assertEquals(2, async.ttsPending.size)

        // A success → 仅 A 推进
        async.fireTts(true, 0)
        assertEquals(listOf(RuleAction.DISMISS), sync.calls)
        assertTrue(flowA.isCompleted)
        assertEquals(FlowStatus.SUCCESS, flowA.result?.status)
        assertFalse(flowB.isCompleted)

        // B error → B 失败继续，callback 未被 A 覆盖
        async.fireTts(false, 1)
        assertEquals(listOf(RuleAction.DISMISS, RuleAction.COPY), sync.calls)
        assertTrue(flowB.isCompleted)
        assertEquals(FlowStatus.PARTIAL_FAILURE, flowB.result?.status)
        assertEquals(1, flowB.failedActions.size)
        assertEquals(RuleAction.TTS, flowB.failedActions[0].type)
    }
}
