package com.enlpot.notix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onRescanRule: () -> Unit = {}
) {
    var ruleToDelete by remember { mutableStateOf<BlockerRule?>(null) }

    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing
    val lay = MaterialTheme.notixLayout

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = lay.screenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(sp.lg))
        // 主标题「规则」
        Text(
            text = stringResource(R.string.rules_title),
            style = t.screenTitle,
            color = c.contentPrimary,
        )
        Spacer(modifier = Modifier.height(sp.xs))
        // 副标题「共 N 条规则」
        Text(
            text = stringResource(R.string.rules_count, rules.size),
            style = t.bodySecondary,
            color = c.contentSecondary,
        )
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
                    val colors by produceState<NotificationColors?>(initialValue = null, key1 = primary?.packageName) {
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
                        appName = appName,
                        packageName = primary?.packageName ?: "",
                        conditionText = buildConditionDescription(rule),
                        extraConditionText = buildExtraDescription(rule),
                        actionText = actionText,
                        hitCount = rule.hitCount,
                        enabled = rule.isEnabled,
                        accent = accent,
                        onAccent = onAccent,
                        onClick = { onRuleClick(rule) },
                        onLongClick = { ruleToDelete = rule },
                        onToggle = { enabled -> onToggleRule(rule, enabled) },
                        onRescan = onRescanRule,
                        onResetHitCount = { onResetHitCount(rule) },
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
        com.enlpot.notix.MatchMode.MIXED -> "${a.joinToString("」") { "「$it」" }}，且不包含${b.joinToString("」") { "「$it」" }}"
        com.enlpot.notix.MatchMode.NOT_CONTAINS_ANY, com.enlpot.notix.MatchMode.NOT_CONTAINS_ALL ->
            "${modeText}：${a.joinToString("」") { "「$it」" }}"
        else -> "${modeText}：${a.joinToString("」") { "「$it」" }}"
    }
}

private fun buildExtraDescription(rule: BlockerRule): String {
    val parts = mutableListOf<String>()
    val extra = rule.extraCondition
    when (extra.screenState) {
        com.enlpot.notix.ScreenState.SCREEN_ON -> parts.add("亮屏时")
        com.enlpot.notix.ScreenState.SCREEN_OFF -> parts.add("熄屏时")
        else -> {}
    }
    when (extra.chargingState) {
        com.enlpot.notix.ChargingState.WIRED -> parts.add("有线充电时")
        com.enlpot.notix.ChargingState.WIRELESS -> parts.add("无线充电时")
        com.enlpot.notix.ChargingState.BATTERY -> parts.add("电池供电时")
        else -> {}
    }
    if (extra.time.enabled) {
        val time = extra.time
        val weekText = if (time.weekdays.isEmpty()) "每天" else time.weekdays.sorted().joinToString(",") { "$it" }
        parts.add("${time.startHour.toString().padStart(2, '0')}:${time.startMinute.toString().padStart(2, '0')}-${time.endHour.toString().padStart(2, '0')}:${time.endMinute.toString().padStart(2, '0')} $weekText")
    }
    return parts.joinToString("，")
}
