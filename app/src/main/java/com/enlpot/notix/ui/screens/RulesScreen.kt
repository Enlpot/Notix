package com.enlpot.notix.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.enlpot.notix.ui.theme.NotixCorner
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.enlpot.notix.BlockerRule
import com.enlpot.notix.MatchMode
import com.enlpot.notix.NotificationColorEngine
import com.enlpot.notix.NotificationColors
import com.enlpot.notix.R
import com.enlpot.notix.RuleCondition
import com.enlpot.notix.RuleWizardSupport
import com.enlpot.notix.ui.components.EmptyState
import com.enlpot.notix.ui.components.NotixConfirmDialog
import com.enlpot.notix.ui.components.RealAppIcon

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        // 主标题「规则」
        Text(
            text = stringResource(R.string.rules_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 副标题「共 N 条规则」
        Text(
            text = stringResource(R.string.rules_count, rules.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        // 顶部横幅：主题色圆角「+ 新建规则」
        Button(
            onClick = onCreateRuleClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = rules.size,
                    key = { rules[it].id }
                ) { index ->
                    RuleCard(
                        rule = rules[index],
                        onRuleClick = onRuleClick,
                        onDeleteRule = { ruleToDelete = it },
                        onToggleRule = { enabled -> onToggleRule(rules[index], enabled) },
                        onResetHitCount = { onResetHitCount(rules[index]) },
                        onRescan = onRescanRule
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
            confirmText = stringResource(R.string.confirm_delete)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RuleCard(
    rule: BlockerRule,
    onRuleClick: (BlockerRule) -> Unit,
    onDeleteRule: (BlockerRule) -> Unit,
    onToggleRule: (Boolean) -> Unit,
    onResetHitCount: () -> Unit,
    onRescan: () -> Unit
) {
    // v7.11：来源 App 列表（v7.13：orEmpty 兜底，旧数据 sourcePackages 可能为 null）
    val sourceApps = rule.sourcePackages.orEmpty()
    val primary = sourceApps.firstOrNull()
    val appName = primary?.appName?.takeIf { it.isNotBlank() } ?: primary?.packageName ?: ""
    val sourceSummary = if (sourceApps.size > 1) {
        stringResource(R.string.rule_sources_count, appName, sourceApps.size)
    } else {
        appName
    }

    // v7.12：与「按应用」tab 一致，NotificationColorEngine 按 App 图标动态取色
    val context = LocalContext.current
    val colors by produceState<NotificationColors?>(initialValue = null, key1 = primary?.packageName) {
        value = NotificationColorEngine.getNotificationColors(context, primary?.packageName.orEmpty())
    }
    val cardBg = colors?.backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val headerFg = colors?.primaryTextColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    val accent = colors?.accentColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    // v7.14：可点击元素（删除/重置）统一用 cardBg 对比度前景色，避免浅色 accent 在亮底上不可读
    val clickableFg = remember(cardBg) { Color(NotificationColorEngine.chooseTextColor(cardBg.toArgb())) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (rule.isEnabled) 1f else 0.5f)
            // v7.50：长按卡片弹出删除确认（替换顶部 Delete 按钮）
            .combinedClickable(
                onClick = { onRuleClick(rule) },
                onLongClick = { onDeleteRule(rule) },
            ),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        shape = NotixCorner.Card
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Top row: app icon + name + toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RealAppIcon(
                    packageName = primary?.packageName ?: "",
                    appName = appName,
                    size = 28.dp,
                    shape = CircleShape,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = sourceSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = headerFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // v7.26/v7.50：重新扫描按钮 + 开关（删除改长按卡片触发）
                IconButton(
                    onClick = onRescan,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = clickableFg
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.notification_rescan),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleRule,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Condition description
            val conditionText = buildConditionDescription(rule)
            if (conditionText.isNotEmpty()) {
                Text(
                    text = conditionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = headerFg.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // Extra condition description
            val extraText = buildExtraDescription(rule)
            if (extraText.isNotEmpty()) {
                Text(
                    text = extraText,
                    style = MaterialTheme.typography.labelSmall,
                    color = headerFg.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Action Flow 完整摘要（复用 RuleWizardSupport.actionFlowSummary，全 App 唯一摘要来源；
            // 长 Flow 截断前 3 个 + "…"，并显示"共 N 个动作"，不撑高卡片）
            val flowActions = rule.actions.orEmpty()
            val flowText = RuleWizardSupport.actionFlowSummaryFlow(flowActions)
            val flowCountText = if (flowActions.size > 3) {
                stringResource(R.string.rule_flow_total_actions, flowActions.size)
            } else {
                ""
            }
            val hitText = if (rule.hitCount > 0) {
                stringResource(R.string.hits_count, rule.hitCount)
            } else {
                stringResource(R.string.no_hits)
            }
            if (flowText.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = flowText,
                        style = MaterialTheme.typography.labelSmall,
                        color = headerFg.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = " · $hitText",
                        style = MaterialTheme.typography.labelSmall,
                        color = headerFg.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
                if (flowCountText.isNotEmpty()) {
                    Text(
                        text = flowCountText,
                        style = MaterialTheme.typography.labelSmall,
                        color = headerFg.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            } else if (rule.hitCount > 0) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = hitText,
                        style = MaterialTheme.typography.labelSmall,
                        color = headerFg.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }
            // Reset hit count button (always shown when hitCount > 0)
            if (rule.hitCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onResetHitCount,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = clickableFg,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.reset_hit_counters),
                            style = MaterialTheme.typography.labelSmall,
                            color = clickableFg,
                        )
                    }
                }
            }
        }
    }
}

private fun buildConditionDescription(rule: BlockerRule): String {
    // v7.13：空安全兜底——旧数据可能残留 null 字段
    val cond = rule.condition ?: RuleCondition()
    val a = cond.includeKeywords.filter { it.isNotBlank() }
    val b = cond.excludeKeywords.filter { it.isNotBlank() }
    if (a.isEmpty() && b.isEmpty()) return ""
    val modeText = when (cond.mode) {
        MatchMode.CONTAINS_ANY -> "包含任一"
        MatchMode.CONTAINS_ALL -> "包含全部"
        MatchMode.NOT_CONTAINS_ANY -> "不包含任一"
        MatchMode.NOT_CONTAINS_ALL -> "不包含全部"
        MatchMode.MIXED -> "包含A且不包含B"
        MatchMode.ADVANCED -> "高级匹配"
    }
    return when (cond.mode) {
        MatchMode.MIXED -> "${a.joinToString("」") { "「$it」" }}，且不包含${b.joinToString("」") { "「$it」" }}"
        MatchMode.NOT_CONTAINS_ANY, MatchMode.NOT_CONTAINS_ALL ->
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
        val t = extra.time
        val weekText = if (t.weekdays.isEmpty()) "每天" else t.weekdays.sorted().joinToString(",") { "$it" }
        parts.add("${t.startHour.toString().padStart(2, '0')}:${t.startMinute.toString().padStart(2, '0')}-${t.endHour.toString().padStart(2, '0')}:${t.endMinute.toString().padStart(2, '0')} $weekText")
    }
    return parts.joinToString("，")
}

