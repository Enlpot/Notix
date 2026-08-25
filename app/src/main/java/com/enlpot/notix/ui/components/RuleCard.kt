package com.enlpot.notix.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.enlpot.notix.R
import com.enlpot.notix.ui.theme.*

/**
 * 规则卡片（DESIGN_SYSTEM.md §14）。
 *
 * 视觉层级（本组件核心）：
 * - 匹配条件：bodySecondary / 弱色（动态底上 onAccent·0.85），回答“匹配什么”。
 * - 执行动作：cardTitle(SemiBold) + 强色（动态底上 onAccent），回答“做什么”。
 * - 两者用分隔线拉开，层级明确。
 * 禁用态整体 alpha = 0.5。
 *
 * 动态底色（产品特色，v7.12 起）：`accent` / `onAccent` 非 null 时用 App 图标动态色；
 * 页面经 [com.enlpot.notix.NotificationColorEngine] 取色后注入，组件内不调引擎、不判 Light/Dark。
 * `accent` / `onAccent` 为 null 时回退中性 surfaceElevated（供 Preview 展示两种形态）。
 *
 * 纯展示：App 图标走 [RealAppIcon]（与动态色无关）；数据由参数注入。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RuleCard(
    appName: String,
    conditionText: String,
    actionText: String,
    hitCount: Int,
    modifier: Modifier = Modifier,
    packageName: String? = null,
    enabled: Boolean = true,
    extraConditionText: String = "",
    accent: Color? = null,
    onAccent: Color? = null,
    onLongClick: () -> Unit = {},
    onToggle: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
    onRescan: () -> Unit = {},
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
            verticalArrangement = Arrangement.spacedBy(sp.sm),
        ) {
            // Top row: app icon + name + rescan + toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RealAppIcon(
                    packageName = packageName,
                    appName = appName,
                    size = 28.dp,
                    shape = NotixCorner.Sm,
                )
                Spacer(Modifier.width(sp.sm))
                Text(
                    text = appName,
                    style = t.cardTitle,
                    color = headerFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onRescan,
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = clickableFg),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.notification_rescan),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }

            // 匹配条件（弱）
            if (conditionText.isNotBlank()) {
                Text(
                    text = conditionText,
                    style = t.bodySecondary,
                    color = weakFg,
                )
            }

            // 附加条件（弱）
            if (extraConditionText.isNotBlank()) {
                Text(
                    text = extraConditionText,
                    style = t.label,
                    color = weakFg,
                )
            }

            // 分隔线 + 执行动作（强）；仅当存在动作时展示，避免空布局
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

            // 命中次数（三级）
            Text(
                text = if (hitCount > 0) {
                    stringResource(R.string.hits_count, hitCount)
                } else {
                    stringResource(R.string.no_hits)
                },
                style = t.caption,
                color = tertiaryFg,
            )

            // 重置命中按钮（命中 > 0 时显示，点击区 ≥44dp）
            if (hitCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
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
                            text = stringResource(R.string.reset_hit_counters),
                            style = t.label,
                            color = clickableFg,
                        )
                    }
                }
            }
        }
    }
}
