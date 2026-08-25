package com.enlpot.notix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import com.enlpot.notix.ui.theme.*

/**
 * 规则卡片（DESIGN_SYSTEM.md §14）。
 *
 * 视觉层级（本组件核心）：
 * - 匹配条件：bodySecondary / contentSecondary（弱），回答“匹配什么”。
 * - 执行动作：cardTitle(SemiBold) + primary（强），回答“做什么”。
 * - 两者用分隔线拉开，层级明确。
 * 禁用态整体 alpha = 0.5。
 *
 * 纯展示：App 图标走 [RealAppIcon]（与动态色无关）；数据由参数注入。
 */
@Composable
fun RuleCard(
    appName: String,
    conditionText: String,
    actionText: String,
    hitCount: Int,
    modifier: Modifier = Modifier,
    packageName: String? = null,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
) {
    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing
    val lay = MaterialTheme.notixLayout

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(NotixCorner.Card)
            .background(c.surfaceElevated)
            .border(1.dp, c.outlineVariant, NotixCorner.Card)
            .clickable(onClick = onClick)
            .padding(lay.cardPadding)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(sp.sm),
        ) {
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
                    color = c.contentPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = enabled, onCheckedChange = onToggle)
            }

            // 匹配条件（弱）
            Text(
                text = conditionText,
                style = t.bodySecondary,
                color = c.contentSecondary,
            )

            // 分隔线，拉开“匹配”与“动作”的层级
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(c.outlineVariant),
            )

            // 执行动作（强）
            Text(
                text = actionText,
                style = t.cardTitle,
                color = c.primary,
            )
            Text(
                text = "命中 $hitCount 次",
                style = t.caption,
                color = c.contentTertiary,
            )
        }
    }
}
