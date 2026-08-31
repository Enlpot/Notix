package com.enlpot.notix.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import com.enlpot.notix.ui.components.NotixConfirmDialog
import com.enlpot.notix.ui.components.NotixDangerButton
import com.enlpot.notix.ui.components.NotixDialog
import com.enlpot.notix.ui.components.NotixDialogButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.enlpot.notix.CrashLogManager
import com.enlpot.notix.DebugLogManager
import com.enlpot.notix.R
import java.io.File
import java.util.Locale

// v7.50：存储占用计算与格式化（供设置主页 + 二级界面共用）

/** 通知历史相关文件（与 NotificationHistoryStorage / BlockedNotificationHistoryStorage 保持一致） */
/** 通知历史相关文件（Room 数据库 + 应用信息库 + 旧版 JSON 兼容） */
private val HISTORY_FILE_NAMES = setOf(
    // Room 通知历史数据库（v8.23+ 主存储）
    "notix.db",
    "notix.db-wal",
    "notix.db-shm",
    // 应用信息库（图标、名称缓存）
    "app_info.db",
    "app_info.db-journal",
    // 旧版 JSON 文件（迁移后可能残留）
    "notification_history.json",
    "notification_history.json.tmp",
    "blocked_notification_history.json",
)

/** 规则相关文件（rules.json 及其备份/临时文件） */
private fun isRuleFile(name: String): Boolean = name.startsWith("rules")

/** 临时/备份文件后缀（其他分类：.bak/.corrupt/.tmp） */
private fun isTempBackupFile(name: String): Boolean =
    name.endsWith(".bak") || name.endsWith(".corrupt") || name.endsWith(".tmp")

/** filesDir + databases 目录下全部文件大小之和（总占用，v8.23 包含 Room 数据库） */
internal fun computeStorageUsageBytes(context: Context): Long {
    var total = 0L
    // filesDir 下的文件
    val dir = context.filesDir
    if (dir.exists() && dir.isDirectory) {
        total += dir.listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L
    }
    // databases 目录下的 Room 数据库文件
    val dbDir = context.getDatabasePath("notix.db").parentFile
    if (dbDir != null && dbDir.exists() && dbDir.isDirectory) {
        total += dbDir.listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L
    }
    return total
}

/** 格式化为 B/KB/MB（一位小数） */
internal fun formatStorageBytes(bytes: Long): String = when {
    bytes < 0L -> "0 B"
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
}

private fun filesDirFiles(context: Context): List<File> =
    context.filesDir.listFiles()?.filter { it.isFile } ?: emptyList()

/** 通知历史分类占用（Room 数据库 + 旧版 JSON 兼容） */
private fun historyBytes(context: Context): Long {
    // filesDir 下的旧版 JSON 文件
    val filesDirSize = filesDirFiles(context)
        .filter { it.name in HISTORY_FILE_NAMES }
        .sumOf { it.length() }
    // databases 目录下的 Room 数据库文件
    val dbDir = context.getDatabasePath("notix.db").parentFile
    val dbSize = if (dbDir != null && dbDir.exists() && dbDir.isDirectory) {
        dbDir.listFiles()?.filter { it.name in HISTORY_FILE_NAMES }?.sumOf { it.length() } ?: 0L
    } else 0L
    return filesDirSize + dbSize
}

/** 规则分类占用 */
private fun rulesBytes(context: Context): Long =
    filesDirFiles(context).filter { isRuleFile(it.name) }.sumOf { it.length() }

/** 其他：崩溃日志 + 调试日志（外部目录）+ 临时/备份文件 */
private fun otherBytes(context: Context): Long {
    val tempBytes = filesDirFiles(context)
        .filter { !HISTORY_FILE_NAMES.contains(it.name) && !isRuleFile(it.name) && isTempBackupFile(it.name) }
        .sumOf { it.length() }
    val crashFile = CrashLogManager.logFile(context)
    val crashBytes = if (crashFile.exists() && crashFile.isFile) crashFile.length() else 0L
    // v8.47.0：调试日志计入「其他」占用
    val debugFile = DebugLogManager.logFile(context)
    val debugBytes = if (debugFile.exists() && debugFile.isFile) debugFile.length() else 0L
    return tempBytes + crashBytes + debugBytes
}

/** 清除「其他」：崩溃日志 + 调试日志 + 临时/备份文件（不动 SharedPreferences 与统计） */
private fun clearOtherFiles(context: Context) {
    CrashLogManager.clearLogs(context)
    DebugLogManager.clearLogs(context)
    filesDirFiles(context)
        .filter { !HISTORY_FILE_NAMES.contains(it.name) && !isRuleFile(it.name) && isTempBackupFile(it.name) }
        .forEach { runCatching { it.delete() } }
}

@Composable
fun StorageUsageScreen(
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    onClearRules: () -> Unit,
) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }

    val totalBytes = remember(refreshTick) { computeStorageUsageBytes(context) }
    val historySize = remember(refreshTick) { historyBytes(context) }
    val rulesSize = remember(refreshTick) { rulesBytes(context) }
    val otherSize = remember(refreshTick) { otherBytes(context) }

    // 二次确认弹窗状态
    var confirmClearHistory by remember { mutableStateOf(false) }
    var confirmClearRules by remember { mutableStateOf(false) }
    var confirmClearOther by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }

    fun runClear(action: () -> Unit) {
        action()
        refreshTick++
    }

    NotixDialog(
        onDismiss = onBack,
        title = stringResource(R.string.storage_usage_title),
        content = {
            Text(
                text = stringResource(R.string.storage_usage_total, formatStorageBytes(totalBytes)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            StorageItemCard(
                title = stringResource(R.string.storage_usage_history),
                sizeText = formatStorageBytes(historySize),
                desc = stringResource(R.string.storage_usage_history_desc),
                onClear = { confirmClearHistory = true }
            )
            Spacer(Modifier.height(12.dp))
            StorageItemCard(
                title = stringResource(R.string.storage_usage_rules),
                sizeText = formatStorageBytes(rulesSize),
                desc = stringResource(R.string.storage_usage_rules_desc),
                onClear = { confirmClearRules = true }
            )
            Spacer(Modifier.height(12.dp))
            StorageItemCard(
                title = stringResource(R.string.storage_usage_other),
                sizeText = formatStorageBytes(otherSize),
                desc = stringResource(R.string.storage_usage_other_desc),
                onClear = { confirmClearOther = true }
            )
            Spacer(Modifier.height(8.dp))
        },
        buttons = {
            NotixDangerButton(
                onClick = { confirmClearAll = true },
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.storage_clear_all)
            )
        }
    )

    // 二次确认弹窗（覆盖在存储占用弹窗之上）
    if (confirmClearHistory) {
        NotixConfirmDialog(
            onDismiss = { confirmClearHistory = false },
            onConfirm = {
                confirmClearHistory = false
                runClear { onClearHistory() }
            },
            title = stringResource(R.string.storage_clear_history_confirm_title),
            body = stringResource(R.string.storage_clear_history_confirm_body),
            confirmText = stringResource(R.string.storage_clear)
        )
    }
    if (confirmClearRules) {
        NotixConfirmDialog(
            onDismiss = { confirmClearRules = false },
            onConfirm = {
                confirmClearRules = false
                runClear { onClearRules() }
            },
            title = stringResource(R.string.storage_clear_rules_confirm_title),
            body = stringResource(R.string.storage_clear_rules_confirm_body),
            confirmText = stringResource(R.string.storage_clear)
        )
    }
    if (confirmClearOther) {
        NotixConfirmDialog(
            onDismiss = { confirmClearOther = false },
            onConfirm = {
                confirmClearOther = false
                runClear { clearOtherFiles(context) }
            },
            title = stringResource(R.string.storage_clear_other_confirm_title),
            body = stringResource(R.string.storage_clear_other_confirm_body),
            confirmText = stringResource(R.string.storage_clear)
        )
    }
    if (confirmClearAll) {
        NotixConfirmDialog(
            onDismiss = { confirmClearAll = false },
            onConfirm = {
                confirmClearAll = false
                runClear {
                    onClearHistory()
                    onClearRules()
                    clearOtherFiles(context)
                }
            },
            title = stringResource(R.string.storage_clear_all_confirm_title),
            body = stringResource(R.string.storage_clear_all_confirm_body),
            confirmText = stringResource(R.string.storage_clear_all)
        )
    }
}

@Composable
private fun StorageItemCard(
    title: String,
    sizeText: String,
    desc: String,
    onClear: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                TextButton(
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.storage_clear), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
