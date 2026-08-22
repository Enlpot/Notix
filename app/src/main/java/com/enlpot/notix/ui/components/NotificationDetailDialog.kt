package com.enlpot.notix.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.enlpot.notix.NotificationColorEngine
import com.enlpot.notix.R
import com.enlpot.notix.SimpleNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 单条通知详情弹窗（v7.35 抽取为可复用组件，历史列表与聚合窗口共用）。
 *
 * 高度自适应：上限窗口 80%，内容短则压缩到内容高度，超出则 80% 内滚动；
 * 标题/正文可光标选择复制；底部操作按钮（删除 / 打开 / 创建规则 / 还原）固定。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationDetailDialog(
    notification: SimpleNotification,
    blocked: Boolean,
    showRestore: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    onCreateRule: () -> Unit,
    onRestore: (() -> Unit)? = null
) {
    val packageName = notification.packageName
    val displayAppName = notification.appLabel ?: packageName.orEmpty()
    val title = notification.title.orEmpty()
    val text = notification.text.orEmpty()
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val timeStr = sdf.format(Date(notification.timestamp))

    // v7.14：已过滤标签使用 error 实底 + 对比度文字色（与变更计数角标一致）
    val errorColor = MaterialTheme.colorScheme.error
    val errorFg = remember(errorColor) { Color(NotificationColorEngine.chooseTextColor(errorColor.toArgb())) }

    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints {
            val maxDialogHeight = this.maxHeight * 0.8f
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .heightIn(max = maxDialogHeight),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 头部：图标 + 应用名 + 时间（固定）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RealAppIcon(
                            packageName = packageName,
                            appName = displayAppName,
                            size = 36.dp,
                            shape = RoundedCornerShape(10.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = displayAppName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    // 内容区：上限 80% 高度内可滚动（内容短时压缩到内容高度）+ 光标拖动选择复制
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        SelectionContainer {
                            Column {
                                if (title.isNotEmpty()) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                if (text.isNotEmpty()) {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (blocked) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = errorColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.history_blocked_badge),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = errorColor
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    // 底部按钮区：固定四按钮（删除/打开/创建规则/还原），带背景色圆角矩形样式，按钮多时自动换行
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
                    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 删除：error 红底
                        Surface(
                            onClick = {
                                onDismiss()
                                onDelete()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = errorColor
                        ) {
                            Text(
                                text = stringResource(R.string.notification_delete),
                                color = errorFg,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        // 打开：主题色底
                        Surface(
                            onClick = {
                                onDismiss()
                                onOpen()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = primaryColor
                        ) {
                            Text(
                                text = stringResource(R.string.notification_open),
                                color = onPrimaryColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        // 创建规则：主题色边框透明底
                        Surface(
                            onClick = {
                                onDismiss()
                                onCreateRule()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, primaryColor)
                        ) {
                            Text(
                                text = stringResource(R.string.notification_create_rule),
                                color = primaryColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        // 还原：灰底
                        Surface(
                            onClick = {
                                onDismiss()
                                onRestore?.invoke()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = stringResource(R.string.notification_restore),
                                color = onSurfaceVariantColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
