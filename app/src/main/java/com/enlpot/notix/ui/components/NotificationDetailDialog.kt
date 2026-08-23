package com.enlpot.notix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.enlpot.notix.R
import com.enlpot.notix.SimpleNotification
import com.enlpot.notix.ui.theme.NotixCorner
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

    // v8.6：删除通知前二次确认（与崩溃日志弹窗统一风格）
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        // v8.3：关闭平台默认窄窗口，让弹窗按屏幕宽度 90% 真实生效
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // v8.3：关闭平台默认窗口后，原生 scrim 失效，这里手动补半透明遮罩
                .background(Color.Black.copy(alpha = 0.32f))
                // 点击遮罩（弹窗外部）即关闭
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
        BoxWithConstraints {
            val maxDialogHeight = this.maxHeight * 0.8f
            Surface(
                modifier = Modifier
                    // v8.3：弹窗宽度改为屏幕 90%（关闭平台窄窗口后真实生效）
                    .fillMaxWidth(0.9f)
                    .heightIn(max = maxDialogHeight)
                    // 点弹窗内部不关闭（吞掉点击，避免穿透到外部遮罩）
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = NotixCorner.Dialog,
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
                    // 底部按钮区：上排「删除/打开/还原」三个次级按钮，下排「创建规则」主题色主按钮
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // 上排：删除 / 打开 / 还原（三个等宽次级按钮）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 删除：保留破坏性语义，但用 error 文字色而非实心红块
                            Button(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.weight(1f),
                                shape = NotixCorner.Control,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = errorColor
                                )
                            ) {
                                Text(stringResource(R.string.notification_delete))
                            }
                            // 打开：次级按钮
                            Button(
                                onClick = {
                                    onDismiss()
                                    onOpen()
                                },
                                modifier = Modifier.weight(1f),
                                shape = NotixCorner.Control,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(stringResource(R.string.notification_open))
                            }
                            // 还原：次级按钮
                            Button(
                                onClick = {
                                    onDismiss()
                                    onRestore?.invoke()
                                },
                                modifier = Modifier.weight(1f),
                                shape = NotixCorner.Control,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(stringResource(R.string.notification_restore))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // 下排：创建规则（主题色主按钮，单独一行、整宽）
                        Button(
                            onClick = {
                                onDismiss()
                                onCreateRule()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = NotixCorner.Control,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
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

    if (showDeleteConfirm) {
        NotixConfirmDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDismiss()
                onDelete()
            },
            title = stringResource(R.string.delete_item_title),
            body = stringResource(R.string.delete_item_confirm, displayAppName),
            confirmText = stringResource(R.string.delete),
            danger = true
        )
    }
}
