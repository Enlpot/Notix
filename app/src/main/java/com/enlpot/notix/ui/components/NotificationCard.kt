package com.enlpot.notix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.enlpot.notix.R
import com.enlpot.notix.ui.theme.*

/**
 * 通知卡片（DESIGN_SYSTEM.md §13）。
 *
 * 纯展示组件：动态背景色由页面层经 [com.enlpot.notix.NotificationColorEngine] 取色后通过
 * [accent] / [onAccent] 注入，组件内部不调用引擎、不判断 Light/Dark。
 *
 * Variant：
 * - [Normal]：单条折叠态（图标 + App 名 + 标题 + 一行摘要 + 时间）。
 * - [Multiple]：同 App 多条聚合（右上计数徽标 + "其余 N 条"）。
 * Expanded（展开完整正文 + 操作区）留待正式页面迁移时补充（见 STAGE3_PROGRESS）。
 *
 * v5（Stage 5）新增能力（不接入页面，仅组件补全）：
 * - [blocked]：右下角 error 底「已过滤」徽标（与现有 History 行为一致）。
 * - [compact] + [indent]：折叠展开态缩宽显示（缩进 + 紧凑布局）。
 * - [onHistoryClick]：计数徽标独立点击（与卡片 [onClick] 分离）。
 *
 * v6（Stage 6）补全：
 * - 计数徽标对齐现有 History 行为：「数字 + 下拉三角」（`Icons.Default.ArrowDropDown`）。
 * - 「其余 N 条」改用 [R.string.notification_more_count]。
 * - 清理冗余 `maxLines = if (compact) 1 else 1` → `maxLines = 1`。
 */
enum class NotificationCardVariant { Normal, Multiple }

data class NotificationCardData(
    val appName: String,
    val title: String,
    val summary: String,
    val timestamp: String,
    val count: Int = 1,
)

@Composable
fun NotificationCard(
    data: NotificationCardData,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
    packageName: String? = null,
    variant: NotificationCardVariant =
        if (data.count > 1) NotificationCardVariant.Multiple else NotificationCardVariant.Normal,
    onClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    blocked: Boolean = false,
    compact: Boolean = false,
    indent: Dp = 0.dp,
) {
    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing
    val lay = MaterialTheme.notixLayout

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = indent)
            .clip(NotixCorner.Card)
            .background(accent)
            .clickable(onClick = onClick)
            .padding(lay.cardPadding)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            RealAppIcon(
                packageName = packageName,
                appName = data.appName,
                size = 28.dp,
                shape = NotixCorner.Sm,
            )
            Spacer(Modifier.width(sp.sm))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = data.appName,
                        style = t.cardTitle,
                        color = onAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (variant == NotificationCardVariant.Multiple) {
                        Spacer(Modifier.width(sp.sm))
                        CountBadge(
                            count = data.count,
                            onAccent = onAccent,
                            onClick = onHistoryClick,
                        )
                    }
                }
                if (data.title.isNotEmpty()) {
                    Spacer(Modifier.height(sp.xs))
                    Text(
                        text = data.title,
                        style = t.body,
                        color = onAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (data.summary.isNotEmpty()) {
                    Spacer(Modifier.height(sp.xs))
                    Text(
                        text = data.summary,
                        style = t.bodySecondary,
                        color = onAccent,
                        maxLines = if (compact) 1 else if (variant == NotificationCardVariant.Multiple) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(sp.xs))
                Text(
                    text = data.timestamp,
                    style = t.caption,
                    color = onAccent.copy(alpha = 0.8f),
                )
            }
        }
        if (blocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = sp.md, bottom = sp.md)
                    .clip(NotixCorner.Sm)
                    .background(c.error)
                    .padding(horizontal = sp.sm, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.history_blocked_badge),
                    style = t.label,
                    color = c.onError,
                )
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int, onAccent: Color, onClick: () -> Unit) {
    val sp = MaterialTheme.notixSpacing
    Box(
        modifier = Modifier
            .clip(NotixCorner.Sm)
            .background(onAccent.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = sp.sm, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = count.toString(),
                style = MaterialTheme.notixType.numeric,
                color = onAccent,
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.open_history),
                tint = onAccent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
