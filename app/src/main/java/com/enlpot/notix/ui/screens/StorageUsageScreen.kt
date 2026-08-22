package com.enlpot.notix.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.enlpot.notix.CrashLogManager
import com.enlpot.notix.R
import java.io.File
import java.util.Locale

// v7.50：存储占用计算与格式化（供设置主页 + 二级界面共用）

/** 通知历史相关文件（与 NotificationHistoryStorage / BlockedNotificationHistoryStorage 保持一致） */
private val HISTORY_FILE_NAMES = setOf(
    "notification_history.json",
    "notification_history.json.tmp",
    "blocked_notification_history.json",
)

/** 规则相关文件（rules.json 及其备份/临时文件） */
private fun isRuleFile(name: String): Boolean = name.startsWith("rules")

/** 临时/备份文件后缀（其他分类：.bak/.corrupt/.tmp） */
private fun isTempBackupFile(name: String): Boolean =
    name.endsWith(".bak") || name.endsWith(".corrupt") || name.endsWith(".tmp")

/** filesDir 下全部文件大小之和（总占用） */
internal fun computeStorageUsageBytes(context: Context): Long {
    val dir = context.filesDir
    if (!dir.exists() || !dir.isDirectory) return 0L
    return dir.listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L
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

/** 通知历史分类占用 */
private fun historyBytes(context: Context): Long =
    filesDirFiles(context).filter { it.name in HISTORY_FILE_NAMES }.sumOf { it.length() }

/** 规则分类占用 */
private fun rulesBytes(context: Context): Long =
    filesDirFiles(context).filter { isRuleFile(it.name) }.sumOf { it.length() }

/** 其他：崩溃日志（外部目录）+ 临时/备份文件 */
private fun otherBytes(context: Context): Long {
    val tempBytes = filesDirFiles(context)
        .filter { !HISTORY_FILE_NAMES.contains(it.name) && !isRuleFile(it.name) && isTempBackupFile(it.name) }
        .sumOf { it.length() }
    val crashFile = CrashLogManager.logFile(context)
    val crashBytes = if (crashFile.exists() && crashFile.isFile) crashFile.length() else 0L
    return tempBytes + crashBytes
}

/** 清除「其他」：崩溃日志 + 临时/备份文件（不动 SharedPreferences 与统计） */
private fun clearOtherFiles(context: Context) {
    CrashLogManager.clearLogs(context)
    filesDirFiles(context)
        .filter { !HISTORY_FILE_NAMES.contains(it.name) && !isRuleFile(it.name) && isTempBackupFile(it.name) }
        .forEach { runCatching { it.delete() } }
}

@Composable
fun StorageUsageScreen(
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    onClearRules: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }

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

    Column(modifier = modifier) {
        // 顶部返回栏「< 存储占用」
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Text(
                text = stringResource(R.string.storage_usage_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
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
            Spacer(Modifier.height(24.dp))
            // v7.50：底部全宽红色「清除全部」（二次确认，依次清空三项）
            // v8.2：与列表内「清除」一致，改为红字 TextButton
            TextButton(
                onClick = { confirmClearAll = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.storage_clear_all), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // 二次确认弹窗
    if (confirmClearHistory) {
        ConfirmClearDialog(
            title = stringResource(R.string.storage_clear_history_confirm_title),
            body = stringResource(R.string.storage_clear_history_confirm_body),
            onConfirm = {
                confirmClearHistory = false
                runClear { onClearHistory() }
            },
            onDismiss = { confirmClearHistory = false }
        )
    }
    if (confirmClearRules) {
        ConfirmClearDialog(
            title = stringResource(R.string.storage_clear_rules_confirm_title),
            body = stringResource(R.string.storage_clear_rules_confirm_body),
            onConfirm = {
                confirmClearRules = false
                runClear { onClearRules() }
            },
            onDismiss = { confirmClearRules = false }
        )
    }
    if (confirmClearOther) {
        ConfirmClearDialog(
            title = stringResource(R.string.storage_clear_other_confirm_title),
            body = stringResource(R.string.storage_clear_other_confirm_body),
            onConfirm = {
                confirmClearOther = false
                runClear { clearOtherFiles(context) }
            },
            onDismiss = { confirmClearOther = false }
        )
    }
    if (confirmClearAll) {
        ConfirmClearDialog(
            title = stringResource(R.string.storage_clear_all_confirm_title),
            body = stringResource(R.string.storage_clear_all_confirm_body),
            onConfirm = {
                confirmClearAll = false
                runClear {
                    onClearHistory()
                    onClearRules()
                    clearOtherFiles(context)
                }
            },
            onDismiss = { confirmClearAll = false }
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

@Composable
private fun ConfirmClearDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
