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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.enlpot.notix.DebugLogManager
import com.enlpot.notix.ui.theme.NotixCorner
import com.enlpot.notix.ui.theme.notix
import com.enlpot.notix.ui.theme.notixSpacing
import com.enlpot.notix.ui.theme.notixType

/**
 * v8.47.0 调试日志弹窗（设置页入口）。
 * 记录关键业务路径详细日志（插件/通知/规则/动作/启动），便于远程诊断。
 * 包含：抓取开关、查看日志、打开存放位置、分享日志文件、清空（二次确认）。
 */
@Composable
fun DebugLogDialog(
    onDismiss: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val c = MaterialTheme.notix
    val sp = MaterialTheme.notixSpacing
    var showContent by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(DebugLogManager.isEnabled(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        NotixConfirmDialog(
            onDismiss = { showClearConfirm = false },
            onConfirm = {
                DebugLogManager.clearLogs(context)
                showClearConfirm = false
                message = "调试日志已清空"
            },
            title = "清空调试日志",
            body = "确定清空调试日志吗？清空后不可恢复。"
        )
    }

    if (showContent) {
        val logs = DebugLogManager.readLogs(context)
        NotixDialog(
            onDismiss = { showContent = false },
            title = "查看调试日志",
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = logs.ifEmpty { "暂无日志" },
                        style = MaterialTheme.notixType.caption,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(sp.lg))
            },
            buttons = {
                NotixDialogButton(
                    onClick = { showContent = false },
                    modifier = Modifier.fillMaxWidth(),
                    text = "返回"
                )
            }
        )
    } else {
        NotixDialog(
            onDismiss = onDismiss,
            title = "调试日志",
            content = {
                Text(
                    text = "记录插件下载/加载、通知处理、规则匹配、动作执行等关键路径的详细日志，用于问题诊断。",
                    style = MaterialTheme.notixType.caption,
                    color = c.contentSecondary
                )
                Spacer(Modifier.height(sp.lg))
                // 抓取开关
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
                                text = "日志抓取",
                                style = MaterialTheme.notixType.button
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (enabled) "已开启（崩溃后会自动开启）" else "已关闭（崩溃后下次启动自动开启）",
                                style = MaterialTheme.notixType.caption,
                                color = if (enabled) c.primary else c.contentSecondary
                            )
                        }
                        NotixSwitch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                DebugLogManager.setEnabled(context, it)
                                onEnabledChanged(it)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(sp.lg))
                // 查看 / 打开位置 / 分享：三个等宽按钮
                Row(modifier = Modifier.fillMaxWidth()) {
                    NotixDialogButton(
                        onClick = { showContent = true },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Visibility,
                        text = "查看",
                        containerColor = c.surfaceVariant,
                        contentColor = c.primary
                    )
                    Spacer(Modifier.width(sp.sm))
                    NotixDialogButton(
                        onClick = { message = openLogLocation(context) },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FolderOpen,
                        text = "位置",
                        containerColor = c.surfaceVariant,
                        contentColor = c.primary
                    )
                    Spacer(Modifier.width(sp.sm))
                    NotixDialogButton(
                        onClick = { message = shareLog(context) },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Share,
                        text = "分享",
                        containerColor = c.surfaceVariant,
                        contentColor = c.primary
                    )
                }
                Spacer(Modifier.height(sp.md))
                // 日志文件路径（常显，便于用户直接告知）
                Text(
                    text = "路径：${DebugLogManager.logFile(context).absolutePath}",
                    style = MaterialTheme.notixType.caption,
                    color = c.contentSecondary
                )
                Spacer(Modifier.height(sp.md))
                // 清空
                NotixDangerButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Delete,
                    text = "清空调试日志"
                )
                message?.let { err ->
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

/** 打开日志所在目录；失败时返回可在弹窗内展示的路径提示。 */
private fun openLogLocation(context: Context): String? {
    val file = DebugLogManager.logFile(context)
    val parent = file.parentFile
        ?: return "日志路径：${file.absolutePath}"
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
    }
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
        "日志路径：${file.absolutePath}"
    }
}

/** 分享日志文件（系统分享面板）。失败时返回提示。 */
private fun shareLog(context: Context): String? {
    val file = DebugLogManager.logFile(context)
    if (!file.exists() || file.length() == 0L) return "暂无日志可分享"
    return try {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Notix 调试日志")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享调试日志"))
        null
    } catch (e: Exception) {
        "分享失败：${e.message}"
    }
}
