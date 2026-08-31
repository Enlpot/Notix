package com.enlpot.notix.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.enlpot.notix.ui.components.NotixSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.enlpot.notix.R
import com.enlpot.notix.ui.theme.*

/**
 * 规则卡片（v8.35 重构）。
 *
 * 布局结构（5行）：
 * - 第一行：规则名称（左，粗体主题色） + 开关（右）
 * - 第二行：app图标 + app名称（左） + 重置计数按钮（右）
 * - 分割线
 * - 第三行：规则内容（匹配条件+附加条件，可换行）
 * - 第四行：动作流（可换行）
 * - 第五行：命中次数
 *
 * 禁用态整体 alpha = 0.5。
 *
 * 动态底色（产品特色）：`accent` / `onAccent` 非 null 时用 App 图标动态色；
 * `accent` / `onAccent` 为 null 时回退中性 surfaceElevated。
 *
 * 纯展示：App 图标走 [RealAppIcon]；数据由参数注入。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RuleCard(
    ruleName: String,
    appName: String,
    keywordSummary: String,
    phoneStateSummary: String,
    timeSummary: String,
    actionText: String,
    hitCount: Int,
    modifier: Modifier = Modifier,
    packageName: String? = null,
    enabled: Boolean = true,
    accent: Color? = null,
    onAccent: Color? = null,
    onLongClick: () -> Unit = {},
    onToggle: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
    onResetHitCount: () -> Unit = {},
) {
    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing
    val lay = MaterialTheme.notixLayout

    val isDynamic = accent != null && onAccent != null
    val bg = accent ?: c.surfaceElevated
    val headerFg = onAccent ?: c.contentPrimary
    val weakFg = if (isDynamic) onAccent.copy(alpha = 0.85f) else c.contentSecondary
    val actionFg = if (isDynamic) onAccent else c.primary
    val sepFg = if (isDynamic) onAccent.copy(alpha = 0.2f) else c.outlineVariant
    val tertiaryFg = if (isDynamic) onAccent.copy(alpha = 0.7f) else c.contentTertiary
    val clickableFg = onAccent ?: c.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(NotixCorner.Card)
            .background(bg)
            .then(if (isDynamic) Modifier else Modifier.border(1.dp, c.outlineVariant, NotixCorner.Card))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(lay.cardPadding)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 第一行：规则名称（左） + 开关（右）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ruleName,
                    style = MaterialTheme.typography.titleLarge,
                    color = actionFg,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                NotixSwitch(checked = enabled, onCheckedChange = onToggle)
            }
            // 分割线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(sepFg),
            )

            // 第二行：app图标 + app名称（左） + 重置计数按钮（右）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RealAppIcon(
                    packageName = packageName,
                    appName = appName,
                    size = 24.dp,
                    shape = NotixCorner.Sm,
                )
                Spacer(Modifier.width(sp.sm))
                Text(
                    text = appName,
                    style = t.body,
                    color = headerFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (hitCount > 0) {
                    TextButton(
                        onClick = onResetHitCount,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = clickableFg,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.reset_hit_counters),
                            style = t.label,
                            color = clickableFg,
                        )
                    }
                }
            }

            // 分割线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(sepFg),
            )

            // 第三行：条件区（三行：关键字、状态、时间，不限则不显示）
            if (keywordSummary.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp),
                        tint = actionFg,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "关键字",
                        style = t.caption,
                        color = weakFg,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.widthIn(min = 42.dp),
                    )
                    Text(
                        text = keywordSummary,
                        style = t.caption,
                        color = weakFg,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (phoneStateSummary.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp),
                        tint = weakFg,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "状态",
                        style = t.caption,
                        color = weakFg,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.widthIn(min = 42.dp),
                    )
                    Text(
                        text = phoneStateSummary,
                        style = t.caption,
                        color = weakFg,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (timeSummary.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp),
                        tint = weakFg,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "时间",
                        style = t.caption,
                        color = weakFg,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.widthIn(min = 42.dp),
                    )
                    Text(
                        text = timeSummary,
                        style = t.caption,
                        color = weakFg,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 第四行：动作流（可换行）
            if (actionText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(sepFg),
                )
                Text(
                    text = actionText,
                    style = t.cardTitle.copy(fontWeight = FontWeight.SemiBold),
                    color = actionFg,
                )
            }

            // 第五行：命中次数
            Text(
                text = if (hitCount > 0) {
                    stringResource(R.string.hits_count, hitCount)
                } else {
                    stringResource(R.string.no_hits)
                },
                style = t.caption,
                color = tertiaryFg,
            )
        }
    }
}







