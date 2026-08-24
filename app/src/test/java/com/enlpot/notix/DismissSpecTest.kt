package com.enlpot.notix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * v8.13 新增、v8.14 扩展：DISMISS 工厂 + 默认参数契约测试。
 *
 * 关键契约：
 * - `defaultParamsFor(DISMISS)` 返回 null（保持向后兼容，行为 = 不含常驻通知）
 * - `dismissSpec(includeOngoing=false)` 返回 params=null 的 spec（与默认一致）
 * - `dismissSpec(includeOngoing=true)` 返回含 DismissParams(includeOngoing=true) 的 JSON
 * - `dismissSpec(includeOngoing=true, snoozeDurationMs=...)` 写入自定义冻结时长；缺省默认 7 天
 *
 * 目的：保证既有的 ActionFlowEditorTest 契约（assertEquals(null, params)）不被破坏，
 * 同时为 includeOngoing=true 路径提供独立可验证的 spec 形态。
 */
class DismissSpecTest {

    @Test
    fun `defaultParamsFor DISMISS returns null to keep backward compat`() {
        assertNull(RuleWizardSupport.defaultParamsFor(RuleAction.DISMISS))
    }

    @Test
    fun `dismissSpec with includeOngoing false yields null params`() {
        val spec = RuleWizardSupport.dismissSpec(includeOngoing = false)
        assertEquals(RuleAction.DISMISS, spec.type)
        assertNull(spec.params)
    }

    @Test
    fun `dismissSpec with includeOngoing true writes DismissParams json`() {
        val spec = RuleWizardSupport.dismissSpec(includeOngoing = true)
        assertEquals(RuleAction.DISMISS, spec.type)
        val params = spec.params
            ?: error("dismissSpec(true) must produce non-null params")
        val flag = params.get("includeOngoing")
            ?: error("DismissParams JSON must contain 'includeOngoing'")
        assertEquals(true, flag.asBoolean)
    }

    @Test
    fun `dismissSpec with includeOngoing true defaults snooze duration to 7 days`() {
        val spec = RuleWizardSupport.dismissSpec(includeOngoing = true)
        val params = spec.params ?: error("params must be non-null")
        assertEquals(SnoozeDurations.DAY_7, params.get("snoozeDurationMs")?.asLong)
    }

    @Test
    fun `dismissSpec writes custom snooze duration`() {
        val spec = RuleWizardSupport.dismissSpec(includeOngoing = true, snoozeDurationMs = SnoozeDurations.DAY_30)
        val params = spec.params ?: error("params must be non-null")
        assertEquals(SnoozeDurations.DAY_30, params.get("snoozeDurationMs")?.asLong)
    }

    @Test
    fun `hasActionParams DISMISS is true so the parameter dialog opens on add`() {
        // 弹窗要触发，用户才有入口勾选「包括常驻通知」。
        assertEquals(true, RuleWizardSupport.hasActionParams(RuleAction.DISMISS))
    }
}
