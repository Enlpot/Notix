package com.enlpot.notix.ui.components

import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.enlpot.notix.CrashLogManager
import com.enlpot.notix.R
import com.enlpot.notix.ui.theme.NotixCorner
import com.enlpot.notix.ui.theme.notix
import com.enlpot.notix.ui.theme.notixSpacing
import com.enlpot.notix.ui.theme.notixType

/**
 * v7.13 崩溃日志弹窗（设置页 / 长按历史搜索按钮双入口共用）。
 * 包含：日志抓取开关、查看日志内容（只读可滚动）、打开日志存放位置。
 *
 * @param onEnabledChanged 开关状态变化回调（设置页用于刷新入口卡片状态）
 */
@Composable
fun CrashLogDialog(
    onDismiss: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val c = MaterialTheme.notix
    val sp = MaterialTheme.notixSpacing
    var showContent by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(CrashLogManager.isEnabled(context)) }
    // v7.24：打开日志位置失败时在弹窗内展示路径（不再使用系统 Toast）
    var openError by remember { mutableStateOf<String?>(null) }
    // v7.29：清空日志二次确认
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        NotixConfirmDialog(
            onDismiss = { showClearConfirm = false },
            onConfirm = {
                CrashLogManager.clearLogs(context)
                showClearConfirm = false
                openError = context.getString(R.string.crash_log_cleared)
            },
            title = stringResource(R.string.crash_log_clear_title),
            body = stringResource(R.string.crash_log_clear_confirm)
        )
    }

    if (showContent) {
        CrashLogContentDialog(onBack = { showContent = false })
    } else {
        NotixDialog(
            onDismiss = onDismiss,
            title = stringResource(R.string.crash_log_title),
            content = {
                // 日志抓取：圆角列表项（标题/副标题分行 + 开关居右），Control 圆角
                Surface(
                    color = c.surfaceVariant,
                    shape = NotixCorner.Control
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(sp.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.crash_log_capture_state),
                                style = MaterialTheme.notixType.button
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(
                                    if (enabled) R.string.crash_log_capture_enabled
                                    else R.string.crash_log_capture_disabled
                                ),
                                style = MaterialTheme.notixType.caption,
                                color = if (enabled) c.primary
                                else c.contentSecondary
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                CrashLogManager.setEnabled(context, it)
                                onEnabledChanged(it)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(sp.lg))
                // 查看日志 / 打开日志位置：两个等宽次级按钮（主题色内容 + 灰色容器）
                Row(modifier = Modifier.fillMaxWidth()) {
                    NotixDialogButton(
                        onClick = { showContent = true },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Visibility,
                        text = stringResource(R.string.crash_log_view),
                        containerColor = c.surfaceVariant,
                        contentColor = c.primary
                    )
                    Spacer(Modifier.width(sp.sm))
                    NotixDialogButton(
                        onClick = { openError = openLogLocation(context) },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FolderOpen,
                        text = stringResource(R.string.crash_log_open_location),
                        containerColor = c.surfaceVariant,
                        contentColor = c.primary
                    )
                }
                Spacer(Modifier.height(sp.md))
                // v7.29：清空日志（二次确认后删除全部日志）—— 红色填充危险按钮
                NotixDangerButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Delete,
                    text = stringResource(R.string.crash_log_clear)
                )
                // v7.24：打开失败时在弹窗内展示日志路径（不再使用系统 Toast）
                openError?.let { err ->
                    Spacer(Modifier.height(sp.md))
                    Text(
                        text = err,
                        style = MaterialTheme.notixType.caption,
                        color = c.error
                    )
                }
            }
        )
    }
}

/** 只读日志内容对话框（可滚动）。 */
@Composable
private fun CrashLogContentDialog(onBack: () -> Unit) {
    val context = LocalContext.current
    val sp = MaterialTheme.notixSpacing
    val logs = remember { CrashLogManager.readLogs(context) }
    NotixDialog(
        onDismiss = onBack,
        title = stringResource(R.string.crash_log_view),
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = logs.ifEmpty { stringResource(R.string.crash_log_empty) },
                    style = MaterialTheme.notixType.caption,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(sp.lg))
        },
        buttons = {
            NotixDialogButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.close)
            )
        }
    )
}

/**
 * 打开日志所在目录；失败时尝试直接打开日志文件；再次失败时返回可在弹窗内展示的路径提示。
 * v7.24 起不再弹系统 Toast，错误以弹窗内文本呈现。
 */
private fun openLogLocation(context: Context): String? {
    val file = CrashLogManager.logFile(context)
    val parent = file.parentFile
        ?: return context.getString(R.string.crash_log_path_toast, file.absolutePath)

    // 1. 尝试用系统文件管理器打开日志目录
    try {
        val relativePath = parent.absolutePath
            .removePrefix("/storage/emulated/0/")
            .removePrefix("/sdcard/")
        val documentId = "primary:$relativePath"
        val dirUri = DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            documentId
        )
        val dirIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(dirUri, DocumentsContract.Document.MIME_TYPE_DIR)
        }
        context.startActivity(dirIntent)
        return null
    } catch (ignored: Exception) {
        // 系统文件管理器无法打开该目录，继续回退
    }

    // 2. 回退：尝试直接打开日志文件
    return try {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/plain")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        null
    } catch (e: Exception) {
        context.getString(R.string.crash_log_path_toast, file.absolutePath)
    }
}
