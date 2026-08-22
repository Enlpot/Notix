package com.enlpot.notix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.enlpot.notix.NotificationColorEngine
import com.enlpot.notix.NotificationHistoryEntry
import com.enlpot.notix.R
import com.enlpot.notix.SimpleNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 聚合变更历史窗口（v7.8 可交互通知卡片列表）。
 *
 * 从中心弹出，高度自适应（v7.34）：上限窗口 80%，内容短则压缩到内容高度，超出则 80% 内滚动。
 * 顶部为聚合信息（app、标题、变更次数），中部为变更通知卡片列表，每条卡片以通知卡片形式展示，
 * 点击变更卡片弹单条详情弹窗（操作入口在详情窗口内），底部固定关闭按钮。
 */
@Composable
fun HistoryNotificationDetailsDialog(
    entry: NotificationHistoryEntry,
    onDismiss: () -> Unit,
    onOpenNotification: (SimpleNotification) -> Unit = {},
    onCreateRule: (SimpleNotification) -> Unit = {},
    onDeleteNotification: (SimpleNotification) -> Unit = {},
    onRestoreNotification: (SimpleNotification) -> Unit = {}
) {
    val timeFormat = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }
    val changes = entry.changes

    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints {
            val maxDialogHeight = this.maxHeight * 0.8f
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogHeight),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // 顶部：聚合信息
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.appLabel ?: entry.packageName.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = entry.title ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.change_count, entry.displayCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 中部：变更通知卡片列表（占剩余空间，可滚动；内容短时压缩到内容高度；每条可点击弹详情弹窗）
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(changes) { index, change ->
                        ChangeNotificationCard(
                            change = change,
                            isLatest = index == 0,
                            blocked = entry.blocked,
                            timeFormat = timeFormat,
                            onOpen = { onOpenNotification(change) },
                            onCreateRule = { onCreateRule(change) },
                            onDelete = { onDeleteNotification(change) },
                            onRestore = { onRestoreNotification(change) }
                        )
                    }
                }

                // 底部：关闭按钮（固定不滚动）
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.close), fontSize = 16.sp)
                }
            }
            }
        }
    }
}

/** 单条变更通知卡片：app 图标 + 名称 + 标题 + 内容 + 时间；点击弹出详情弹窗（v7.35 方案A）。v7.14 支持右下角「已过滤」标签。 */
@Composable
private fun ChangeNotificationCard(
    change: SimpleNotification,
    isLatest: Boolean,
    blocked: Boolean,
    timeFormat: SimpleDateFormat,
    onOpen: () -> Unit,
    onCreateRule: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit
) {
    var detailExpanded by remember { mutableStateOf(false) }
    // v7.14：已过滤标签 error 实底 + 对比度文字色（与通知卡片一致）
    val errorColor = MaterialTheme.colorScheme.error
    val errorFg = remember(errorColor) { Color(NotificationColorEngine.chooseTextColor(errorColor.toArgb())) }
    Box {
        Card(
            onClick = { detailExpanded = true },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isLatest) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                RealAppIcon(
                    packageName = change.packageName,
                    appName = change.appLabel,
                    size = 34.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = change.appLabel ?: change.packageName.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isLatest) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = stringResource(R.string.latest),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    if (!change.title.isNullOrBlank()) {
                        Text(
                            text = change.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isLatest) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (!change.text.isNullOrBlank()) {
                        Text(
                            text = change.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Text(
                        text = timeFormat.format(Date(change.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // v7.15：已过滤标签——展开变更卡片右下角固定（与通知卡片样式/位置一致）
        if (blocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(errorColor)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = errorFg,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = stringResource(R.string.history_blocked_badge),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = errorFg
                    )
                }
            }
        }

        // v7.35：方案A——点击变更卡片弹详情弹窗（复用 NotificationDetailDialog），操作入口进详情窗口内
        // 四按钮固定显示（含还原）：v7.49 起 showRestore 恒为 true，聚合窗口也提供还原入口
        if (detailExpanded) {
            NotificationDetailDialog(
                notification = change,
                blocked = blocked,
                showRestore = true,
                onDismiss = { detailExpanded = false },
                onDelete = onDelete,
                onOpen = onOpen,
                onCreateRule = onCreateRule,
                onRestore = onRestore
            )
        }
    }
}
