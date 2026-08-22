package com.enlpot.notix.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
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
 * 标题/正文可光标选择复制；底部操作按钮（删除 / 打开 / 还原 / 创建规则）一排并排显示。
 */
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
                    // v7.51：弹窗宽度改为屏幕 90%
                    .fillMaxWidth(0.9f)
                    .heightIn(max = maxDialogHeight),
                shape = RoundedCornerShape(16.dp),
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
                            shape = RoundedCornerShape(8.dp),
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
                    // 底部按钮区：四按钮一排并排显示（v8.2：统一 Material3 Button 体系，圆角 8dp、labelLarge 字级）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 删除：error 红底（与其他危险操作同形态）
                        Button(
                            onClick = {
                                onDismiss()
                                onDelete()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = errorColor,
                                contentColor = errorFg
                            )
                        ) {
                            Text(stringResource(R.string.notification_delete))
                        }
                        // 打开：主题色底
                        Button(
                            onClick = {
                                onDismiss()
                                onOpen()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(stringResource(R.string.notification_open))
                        }
                        // 还原：灰底
                        Button(
                            onClick = {
                                onDismiss()
                                onRestore?.invoke()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(stringResource(R.string.notification_restore))
                        }
                        // 创建规则：主题色边框透明底
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onCreateRule()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(stringResource(R.string.notification_create_rule))
                        }
                    }
                }
            }
        }
    }
}
