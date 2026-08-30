package com.enlpot.notix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.enlpot.notix.BlockerRule
import com.enlpot.notix.NotificationColorEngine
import com.enlpot.notix.NotificationColors
import com.enlpot.notix.R
import com.enlpot.notix.RuleCondition
import com.enlpot.notix.RuleWizardSupport
import com.enlpot.notix.ui.components.EmptyState
import com.enlpot.notix.ui.components.NotixConfirmDialog
import com.enlpot.notix.ui.components.RuleCard
import com.enlpot.notix.ui.theme.*

@Composable
fun RulesScreen(
    rules: List<BlockerRule>,
    onRuleClick: (BlockerRule) -> Unit,
    onCreateRuleClick: () -> Unit,
    onToggleAllRules: (Boolean) -> Unit,
    onDeleteRule: (BlockerRule) -> Unit = {},
    onToggleRule: (BlockerRule, Boolean) -> Unit = { _, _ -> },
    onResetHitCount: (BlockerRule) -> Unit = {},
    onRescanRule: () -> Unit = {},
    scrollToTopTrigger: Int = 0
) {
    var ruleToDelete by remember { mutableStateOf<BlockerRule?>(null) }
    var ruleToReset by remember { mutableStateOf<BlockerRule?>(null) }
    val listState = rememberLazyListState()

    // v8.33：底部规则tab单击回到顶部
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
        }
    }

    // v8.35：未命名规则按创建顺序编号，显示为「未命名规则N」
    val ruleDisplayNames = remember(rules) {
        val nameMap = mutableMapOf<String, String>()
        var unnamedIndex = 1
        rules.sortedBy { it.createdAt }.forEach { rule ->
            val explicitName = rule.name?.takeIf { it.isNotBlank() }
                ?: rule.description?.takeIf { it.isNotBlank() }
            if (explicitName != null) {
                nameMap[rule.id] = explicitName
            } else {
                nameMap[rule.id] = "未命名规则$unnamedIndex"
                unnamedIndex++
            }
        }
        nameMap
    }

    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing
    val lay = MaterialTheme.notixLayout

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = lay.screenHorizontal)
    ) {
        // v8.16：标题顶部间距与设置页一致（4dp，原 sp.lg=16dp 过大）
        Spacer(modifier = Modifier.height(4.dp))
        // 主标题「规则」+ 副标题「共 N 条规则」同行布局（与通知历史页一致，基线对齐）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = stringResource(R.string.rules_title),
                style = t.display,
                color = c.contentPrimary,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(modifier = Modifier.width(sp.sm).alignByBaseline())
            Text(
                text = stringResource(R.string.rules_count, rules.size),
                style = MaterialTheme.typography.bodySmall,
                color = c.contentSecondary,
                modifier = Modifier.alignByBaseline()
            )
        }
        Spacer(modifier = Modifier.height(sp.lg))
        // 顶部横幅：主题色圆角「+ 新建规则」
        Button(
            onClick = onCreateRuleClick,
            modifier = Modifier.fillMaxWidth(),
            shape = NotixCorner.Control,
            colors = ButtonDefaults.buttonColors(
                containerColor = c.primary,
                contentColor = c.onPrimary,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.add_new_rule),
                style = t.button,
            )
        }
        Spacer(modifier = Modifier.height(sp.lg))

        if (rules.isEmpty()) {
            // 空态：标题与横幅按钮仍在顶部，下方仅显示 EmptyState 图标+文案（不显示新建动作按钮）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.Rule,
                    title = stringResource(R.string.no_rules_created),
                    description = stringResource(R.string.no_rules_created_desc),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                verticalArrangement = Arrangement.spacedBy(sp.sm)
            ) {
                items(
                    count = rules.size,
                    key = { rules[it].id }
                ) { index ->
                    val rule = rules[index]
                    // v7.11：来源 App 列表（v7.13：orEmpty 兜底，旧数据 sourcePackages 可能为 null）
                    val sourceApps = rule.sourcePackages.orEmpty()
                    val primary = sourceApps.firstOrNull()
                    val name = primary?.appName?.takeIf { it.isNotBlank() } ?: primary?.packageName ?: ""
                    // 多 App 来源摘要由 appName 参数承载（"appName + N" 形态）
                    val appName = if (sourceApps.size > 1) {
                        stringResource(R.string.rule_sources_count, name, sourceApps.size)
                    } else {
                        name
                    }
                    // v7.12：与「按应用」tab 一致，NotificationColorEngine 按 App 图标动态取色
                    val context = LocalContext.current
                    val colors by produceState<NotificationColors?>(initialValue = null, key1 = primary?.packageName to NotificationColorEngine.colorVersion) {
                        value = NotificationColorEngine.getNotificationColors(context, primary?.packageName.orEmpty())
                    }
                    val accent = colors?.backgroundColor?.let { Color(it) }
                    val onAccent = colors?.primaryTextColor?.let { Color(it) }
                    // 动作流摘要（复用 RuleWizardSupport，全 App 唯一摘要来源；长 Flow 截断前 3 + "…" + "共 N 个动作"）
                    val flowActions = rule.actions.orEmpty()
                    val baseFlow = RuleWizardSupport.actionFlowSummaryFlow(flowActions)
                    val actionText = if (baseFlow.isNotEmpty() && flowActions.size > 3) {
                        "$baseFlow · ${stringResource(R.string.rule_flow_total_actions, flowActions.size)}"
                    } else {
                        baseFlow
                    }
                    RuleCard(
                        ruleName = ruleDisplayNames[rule.id] ?: rule.name ?: "未命名规则",
                        appName = appName,
                        packageName = primary?.packageName ?: "",
                        keywordSummary = buildConditionDescription(rule),
                        phoneStateSummary = buildPhoneStateDescription(rule),
                        timeSummary = buildTimeDescription(rule),
                        actionText = actionText,
                        hitCount = rule.hitCount,
                        enabled = rule.isEnabled,
                        accent = accent,
                        onAccent = onAccent,
                        onClick = { onRuleClick(rule) },
                        onLongClick = { ruleToDelete = rule },
                        onToggle = { enabled -> onToggleRule(rule, enabled) },
                        onResetHitCount = { ruleToReset = rule },
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // Delete confirmation dialog
    ruleToDelete?.let { rule ->
        NotixConfirmDialog(
            onDismiss = { ruleToDelete = null },
            onConfirm = {
                onDeleteRule(rule)
                ruleToDelete = null
            },
            title = stringResource(R.string.confirm_delete_rule_title),
            body = stringResource(R.string.confirm_delete_rule_message),
            confirmText = stringResource(R.string.confirm_delete),
        )
    }

    // Reset hit count confirmation dialog
    ruleToReset?.let { rule ->
        NotixConfirmDialog(
            onDismiss = { ruleToReset = null },
            onConfirm = {
                onResetHitCount(rule)
                ruleToReset = null
            },
            title = "重置命中次数",
            body = "确定要重置规则「${ruleDisplayNames[rule.id] ?: rule.name}」的命中次数吗？此操作不可撤销。",
            confirmText = "重置",
        )
    }
}

private fun buildConditionDescription(rule: BlockerRule): String {
    // v7.13：空安全兜底——旧数据可能残留 null 字段
    val cond = rule.condition ?: RuleCondition()
    val a = cond.includeKeywords.filter { it.isNotBlank() }
    val b = cond.excludeKeywords.filter { it.isNotBlank() }
    if (a.isEmpty() && b.isEmpty()) return ""
    val modeText = when (cond.mode) {
        com.enlpot.notix.MatchMode.CONTAINS_ANY -> "包含任一"
        com.enlpot.notix.MatchMode.CONTAINS_ALL -> "包含全部"
        com.enlpot.notix.MatchMode.NOT_CONTAINS_ANY -> "不包含任一"
        com.enlpot.notix.MatchMode.NOT_CONTAINS_ALL -> "不包含全部"
        com.enlpot.notix.MatchMode.MIXED -> "包含A且不包含B"
        com.enlpot.notix.MatchMode.ADVANCED -> "高级匹配"
    }
    return when (cond.mode) {
        com.enlpot.notix.MatchMode.MIXED -> "${a.joinToString("、") { "「$it」" }}，且不包含${b.joinToString("、") { "「$it」" }}"
        com.enlpot.notix.MatchMode.NOT_CONTAINS_ANY, com.enlpot.notix.MatchMode.NOT_CONTAINS_ALL ->
            "${modeText}：${a.joinToString("、") { "「$it」" }}"
        else -> "${modeText}：${a.joinToString("、") { "「$it」" }}"
    }
}

private fun buildPhoneStateDescription(rule: BlockerRule): String {
    val parts = mutableListOf<String>()
    val extra = rule.extraCondition
    when (extra.screenState) {
        com.enlpot.notix.ScreenState.SCREEN_ON -> parts.add("亮屏")
        com.enlpot.notix.ScreenState.SCREEN_OFF -> parts.add("熄屏")
        else -> {}
    }
    when (extra.chargingState) {
        com.enlpot.notix.ChargingState.WIRED -> parts.add("有线充电")
        com.enlpot.notix.ChargingState.WIRELESS -> parts.add("无线充电")
        com.enlpot.notix.ChargingState.BATTERY -> parts.add("电池供电")
        else -> {}
    }
    return parts.joinToString(" · ")
}

private fun buildTimeDescription(rule: BlockerRule): String {
    val extra = rule.extraCondition
    if (!extra.time.enabled) return ""
    val time = extra.time
    val weekText = if (time.weekdays.isEmpty()) "每天" else {
        val weekNames = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        time.weekdays.sorted().joinToString("、") { weekNames.getOrElse(it) { "周$it" } }
    }
    return "${time.startHour.toString().padStart(2, '0')}:${time.startMinute.toString().padStart(2, '0')}-${time.endHour.toString().padStart(2, '0')}:${time.endMinute.toString().padStart(2, '0')} · $weekText"
}







