package com.enlpot.notix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleWizardSupportTest {

    // --- mergeKnownApps ---

    @Test
    fun `merge dedupes packages across sources`() {
        val result = RuleWizardSupport.mergeKnownApps(
            appInfoRows = listOf("com.a" to "App A"),
            historyRows = listOf("com.a" to "App A History", "com.b" to "App B"),
            queryableInstalled = mapOf("com.a" to "App A PM", "com.c" to "App C"),
            prebuiltNames = emptyMap(),
            ruleRows = listOf("com.b" to "App B Rule")
        )
        assertEquals(listOf("com.a", "com.b", "com.c"), result.map { it.packageName }.sorted())
    }

    @Test
    fun `prebuilt names fill labels but never add packages`() {
        val result = RuleWizardSupport.mergeKnownApps(
            appInfoRows = listOf("com.known" to null),
            historyRows = emptyList(),
            queryableInstalled = emptyMap(),
            prebuiltNames = mapOf("com.known" to "Known App", "com.notinstalled" to "Ghost App"),
            ruleRows = emptyList()
        )
        assertEquals(listOf("com.known"), result.map { it.packageName })
        assertEquals("Known App", result.single().appName)
    }

    @Test
    fun `label priority prefers appInfo over later sources`() {
        val result = RuleWizardSupport.mergeKnownApps(
            appInfoRows = listOf("com.a" to "From AppInfo"),
            historyRows = listOf("com.a" to "From History"),
            queryableInstalled = mapOf("com.a" to "From PM"),
            prebuiltNames = mapOf("com.a" to "From Prebuilt"),
            ruleRows = listOf("com.a" to "From Rule")
        )
        assertEquals("From AppInfo", result.single().appName)
    }

    @Test
    fun `blank label upgrades to first non-blank from a later source`() {
        val result = RuleWizardSupport.mergeKnownApps(
            appInfoRows = listOf("com.a" to null),
            historyRows = listOf("com.a" to ""),
            queryableInstalled = mapOf("com.a" to "From PM"),
            prebuiltNames = emptyMap(),
            ruleRows = emptyList()
        )
        assertEquals("From PM", result.single().appName)
    }

    @Test
    fun `label stays null when no source knows a name`() {
        val result = RuleWizardSupport.mergeKnownApps(
            appInfoRows = emptyList(),
            historyRows = listOf("com.a" to null),
            queryableInstalled = emptyMap(),
            prebuiltNames = emptyMap(),
            ruleRows = emptyList()
        )
        assertNull(result.single().appName)
    }

    @Test
    fun `queryable installed flag set only for PM-visible packages`() {
        val result = RuleWizardSupport.mergeKnownApps(
            appInfoRows = listOf("com.a" to "A", "com.b" to "B"),
            historyRows = emptyList(),
            queryableInstalled = mapOf("com.b" to "B"),
            prebuiltNames = emptyMap(),
            ruleRows = emptyList()
        )
        assertFalse(result.first { it.packageName == "com.a" }.isQueryableInstalled)
        assertTrue(result.first { it.packageName == "com.b" }.isQueryableInstalled)
    }

    @Test
    fun `sorted case-insensitively by name with package fallback`() {
        val result = RuleWizardSupport.mergeKnownApps(
            appInfoRows = listOf(
                "com.zebra" to "zebra",
                "com.apple" to "Apple",
                "com.banana" to null
            ),
            historyRows = emptyList(),
            queryableInstalled = emptyMap(),
            prebuiltNames = emptyMap(),
            ruleRows = emptyList()
        )
        // "Apple" < "com.banana" (package fallback) < "zebra", case-insensitive
        assertEquals(listOf("com.apple", "com.banana", "com.zebra"), result.map { it.packageName })
    }

    @Test
    fun `blank package names are dropped`() {
        val result = RuleWizardSupport.mergeKnownApps(
            appInfoRows = listOf("" to "Nameless"),
            historyRows = emptyList(),
            queryableInstalled = emptyMap(),
            prebuiltNames = emptyMap(),
            ruleRows = emptyList()
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `all sources empty yields empty list`() {
        val result = RuleWizardSupport.mergeKnownApps(
            emptyList(), emptyList(), emptyMap(), emptyMap(), emptyList()
        )
        assertTrue(result.isEmpty())
    }

    // --- isDuplicate ---

    private val existing = listOf(
        BlockerRule(
            sourcePackages = listOf(SourceApp("com.a", null)),
            condition = RuleCondition(includeKeywords = listOf("Promo")),
            actions = listOf(ActionSpec(RuleAction.DISMISS)),
        )
    )

    @Test
    fun `exact same rule is a duplicate`() {
        assertTrue(
            RuleWizardSupport.isDuplicate(
                existing,
                listOf("com.a"),
                RuleCondition(includeKeywords = listOf("Promo")),
                listOf(ActionSpec(RuleAction.DISMISS)),
            )
        )
    }

    @Test
    fun `different keyword is not a duplicate`() {
        assertFalse(
            RuleWizardSupport.isDuplicate(
                existing,
                listOf("com.a"),
                RuleCondition(includeKeywords = listOf("Sale")),
                listOf(ActionSpec(RuleAction.DISMISS)),
            )
        )
    }

    @Test
    fun `different package is not a duplicate`() {
        assertFalse(
            RuleWizardSupport.isDuplicate(
                existing,
                listOf("com.b"),
                RuleCondition(includeKeywords = listOf("Promo")),
                listOf(ActionSpec(RuleAction.DISMISS)),
            )
        )
    }

    @Test
    fun `same keywords but different action is not a duplicate`() {
        assertFalse(
            RuleWizardSupport.isDuplicate(
                existing,
                listOf("com.a"),
                RuleCondition(includeKeywords = listOf("Promo")),
                listOf(ActionSpec(RuleAction.TTS)),
            )
        )
    }

    @Test
    fun `same actions in different order is not a duplicate`() {
        val existingMulti = listOf(
            BlockerRule(
                sourcePackages = listOf(SourceApp("com.a", null)),
                condition = RuleCondition(includeKeywords = listOf("Promo")),
                actions = listOf(
                    ActionSpec(RuleAction.TTS, TtsParams("t").toParamsJson()),
                    ActionSpec(RuleAction.COPY, CopyParams(CopyMode.TEXT).toParamsJson()),
                ),
            )
        )
        // [COPY,TTS] 与 [TTS,COPY] 顺序不同 → 视为不同 Action Flow
        assertFalse(
            RuleWizardSupport.isDuplicate(
                existingMulti,
                listOf("com.a"),
                RuleCondition(includeKeywords = listOf("Promo")),
                listOf(
                    ActionSpec(RuleAction.COPY, CopyParams(CopyMode.TEXT).toParamsJson()),
                    ActionSpec(RuleAction.TTS, TtsParams("t").toParamsJson()),
                ),
            )
        )
    }

    // --- looksLikePackageName ---

    @Test
    fun `typical package name looks valid`() {
        assertTrue(RuleWizardSupport.looksLikePackageName("com.example.app"))
        assertTrue(RuleWizardSupport.looksLikePackageName("  com.example.app  "))
    }

    @Test
    fun `inputs without dots or with whitespace look invalid`() {
        assertFalse(RuleWizardSupport.looksLikePackageName(""))
        assertFalse(RuleWizardSupport.looksLikePackageName("myapp"))
        assertFalse(RuleWizardSupport.looksLikePackageName("com.example app"))
        assertFalse(RuleWizardSupport.looksLikePackageName(".com.example"))
        assertFalse(RuleWizardSupport.looksLikePackageName("com.example."))
    }

    // --- actionFlowSummary (4B 文案统一) ---

    @Test
    fun `delay summary uses wait wording`() {
        assertEquals("等待 1 秒", RuleWizardSupport.actionFlowSummary(RuleWizardSupport.delaySpec(1000)))
        assertEquals("等待 2 秒", RuleWizardSupport.actionFlowSummary(RuleWizardSupport.delaySpec(2000)))
        assertEquals("等待 1500 毫秒", RuleWizardSupport.actionFlowSummary(RuleWizardSupport.delaySpec(1500)))
    }

    @Test
    fun `dismiss summary is 移除通知`() {
        assertEquals("移除通知", RuleWizardSupport.actionFlowSummary(ActionSpec(RuleAction.DISMISS)))
    }

    @Test
    fun `flow summary keeps strict top-down order`() {
        val flow = listOf(
            RuleWizardSupport.ttsSpec("播报模板"),
            RuleWizardSupport.copySpec(CopyMode.TITLE_AND_TEXT),
            RuleWizardSupport.delaySpec(2000),
            RuleWizardSupport.clickButtonSpec("标记为已读"),
            ActionSpec(RuleAction.DISMISS),
        )
        assertEquals(
            "TTS 播报：播报模板 → 标题 + 正文 → 等待 2 秒 → 点击：标记为已读 → 移除通知",
            RuleWizardSupport.actionFlowSummaryFlow(flow, maxShown = 10)
        )
    }

    // --- canSaveFlow（阶段4C-B P1-2：存在任何非法 Action 即禁止保存） ---

    @Test
    fun `empty flow cannot save`() {
        assertFalse(RuleWizardSupport.canSaveFlow(emptyList()))
    }

    @Test
    fun `click button with blank label cannot save`() {
        val flow = listOf(ActionSpec(RuleAction.CLICK_BUTTON, ClickButtonParams("").toParamsJson()))
        assertFalse(RuleWizardSupport.canSaveFlow(flow))
        assertFalse(flow.all { it.isValid })
    }

    @Test
    fun `copy then click button blank label cannot save whole flow`() {
        val flow = listOf(
            ActionSpec(RuleAction.COPY, CopyParams(CopyMode.TEXT).toParamsJson()),
            ActionSpec(RuleAction.CLICK_BUTTON, ClickButtonParams("").toParamsJson()),
        )
        // COPY 本身合法，但整个 Flow 含非法 CLICK_BUTTON → 禁止保存
        assertFalse(RuleWizardSupport.canSaveFlow(flow))
    }

    @Test
    fun `click button with label then dismiss can save`() {
        val flow = listOf(
            ActionSpec(RuleAction.CLICK_BUTTON, ClickButtonParams("Mark as read").toParamsJson()),
            ActionSpec(RuleAction.DISMISS),
        )
        assertTrue(RuleWizardSupport.canSaveFlow(flow))
        assertTrue(flow.all { it.isValid })
    }

    @Test
    fun `five legal actions can save`() {
        val flow = listOf(
            ActionSpec(RuleAction.TTS, TtsParams("{title}：{text}").toParamsJson()),
            ActionSpec(RuleAction.COPY, CopyParams(CopyMode.TITLE_AND_TEXT).toParamsJson()),
            ActionSpec(RuleAction.DELAY, DelayParams(2000L).toParamsJson()),
            ActionSpec(RuleAction.CLICK_BUTTON, ClickButtonParams("Mark as read").toParamsJson()),
            ActionSpec(RuleAction.DISMISS),
        )
        assertTrue(RuleWizardSupport.canSaveFlow(flow))
        assertTrue(flow.all { it.isValid })
    }
}
