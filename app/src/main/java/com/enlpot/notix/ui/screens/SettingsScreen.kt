package com.enlpot.notix.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.enlpot.notix.CrashLogManager
import com.enlpot.notix.ImportError
import com.enlpot.notix.ImportResult
import com.enlpot.notix.NotificationBlockerService
import com.enlpot.notix.R
import com.enlpot.notix.RuleExport
import com.enlpot.notix.RuleExportSerializer
import com.enlpot.notix.RuleImport
import com.enlpot.notix.RuleStorage
import com.enlpot.notix.RuleWizardSupport
import com.enlpot.notix.setup.SetupState
import com.enlpot.notix.SimpleNotification
import com.enlpot.notix.ui.components.CrashLogDialog
import com.enlpot.notix.ui.components.NotixConfirmDialog
import com.enlpot.notix.ui.components.NotixDangerButton
import com.enlpot.notix.ui.components.NotixDialog
import com.enlpot.notix.ui.components.NotixDialogButton
import com.enlpot.notix.ui.components.RealAppIcon
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    onClearHistory: () -> Unit = {},
    onClearHistoryByDate: (Long, Long) -> Unit = { _, _ -> },
    onClearHistoryByPackages: (Set<String>) -> Unit = {},
    onClearRules: () -> Unit = {},
    pastNotifications: List<SimpleNotification> = emptyList(),
) {
    val context = LocalContext.current
    val ruleStorage = remember { RuleStorage(context) }

    // v7.24：日志/提示改为应用内展示（Snackbar），不再使用系统 Toast
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun showMessage(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    var showExportImportDialog by remember { mutableStateOf(false) }
    var exportImportMessage by remember { mutableStateOf<String?>(null) }

    // v7.13：崩溃日志入口
    var showCrashLogDialog by remember { mutableStateOf(false) }
    var crashLogEnabled by remember { mutableStateOf(CrashLogManager.isEnabled(context)) }

    // v7.45：无文本通知文字提取开关（默认关）
    var extractRemoteViewsEnabled by remember {
        mutableStateOf(NotificationBlockerService.isRemoteViewsTextExtractionEnabled(context))
    }

    // Clear history — two-phase: pick mode → detail dialog
    var showClearModeDialog by remember { mutableStateOf(false) }
    var showClearByDateDialog by remember { mutableStateOf(false) }
    var showClearByAppDialog by remember { mutableStateOf(false) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    // v8.14：恢复常驻通知——确认弹窗状态
    var showRestoreSnoozedDialog by remember { mutableStateOf(false) }

    // v7.18：关于分组展开折叠状态
    var aboutFeaturesExpanded by remember { mutableStateOf(false) }
    var aboutPrivacyExpanded by remember { mutableStateOf(false) }

    // v8.6：权限管理二级界面路由 + 主界面聚合状态（进入前台/返回时刷新）
    var showPermissionScreen by remember { mutableStateOf(false) }
    var permissionRefreshTick by remember { mutableStateOf(0) }

    val permListenerGranted = remember(permissionRefreshTick) { SetupState.isNotificationListenerEnabled(context) }
    val permPostNotifGranted = remember(permissionRefreshTick) { SetupState.isPostNotificationsGranted(context) }
    val permBatteryGranted = remember(permissionRefreshTick) { SetupState.isIgnoringBatteryOptimizations(context) }
    val permForegroundGranted = remember(permissionRefreshTick) { isKeepaliveServiceRunning(context) }
    val permFailedCount = listOf(
        permListenerGranted, permPostNotifGranted, permBatteryGranted, permForegroundGranted
    ).count { !it }

    // 进入前台重新检查权限状态
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRefreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Date range input state
    val cal = remember { Calendar.getInstance() }
    var startDateMillis by remember { mutableStateOf(cal.timeInMillis) }
    var endDateMillis by remember { mutableStateOf(cal.timeInMillis) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf<String?>(null) }

    // App selection state
    val uniqueApps = remember(pastNotifications) {
        pastNotifications
            .mapNotNull { n -> n.packageName?.let { pkg -> pkg to (n.appLabel ?: pkg) } }
            .distinct()
            .sortedBy { it.second.lowercase() }
    }
    var selectedPackages by remember { mutableStateOf(setOf<String>()) }
    var appSearchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(uniqueApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) uniqueApps
        else uniqueApps.filter { (pkg, label) ->
            pkg.contains(appSearchQuery, ignoreCase = true) ||
                    label.contains(appSearchQuery, ignoreCase = true)
        }
    }

    // v7.50：存储占用——二级界面路由与总占用（刷新时重算）
    var showStorageUsageScreen by remember { mutableStateOf(false) }
    var storageRefreshTick by remember { mutableStateOf(0) }
    val storageTotalBytes = remember(context, storageRefreshTick) {
        computeStorageUsageBytes(context)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                val localeTag = context.resources.configuration.locales[0].toLanguageTag()
                val export = RuleExport(locale = localeTag, rules = ruleStorage.getRules())
                val json = RuleExportSerializer.toJson(export)
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                exportImportMessage = context.getString(R.string.rules_exported_successfully)
            } catch (e: Exception) {
                exportImportMessage = context.getString(R.string.failed_to_export_rules, e.message ?: "")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val json = readCappedText(context, it, MAX_IMPORT_BYTES)
                if (json == null) {
                    exportImportMessage = context.getString(R.string.rules_file_too_large)
                    return@let
                }
                when (val result = RuleImport.parse(json)) {
                    is ImportResult.Error -> {
                        exportImportMessage = when (result.reason) {
                            ImportError.TooLarge -> context.getString(R.string.rules_file_too_large)
                            ImportError.Malformed -> context.getString(R.string.invalid_rules_file)
                            ImportError.SchemaMismatch -> context.getString(R.string.invalid_rules_file_schema)
                            ImportError.Empty -> context.getString(R.string.import_no_rules)
                        }
                    }
                    is ImportResult.Success -> {
                        val currentRules = ruleStorage.getRules()
                        // Dedup by rule *signature* (not identity): imported ids are always freshly
                        // minted, so they can never match an existing rule.
                        val newRules = result.rules.filter { imported ->
                            !RuleWizardSupport.isDuplicate(
                                currentRules,
                                imported.sourcePackages.map { it.packageName },
                                imported.condition,
                                imported.actions,
                            )
                        }
                        if (newRules.isNotEmpty()) {
                            ruleStorage.addRules(newRules)
                        }
                        exportImportMessage = when {
                            // v8.0：导入文件有规则但全部与现有重复 → 明确提示"无新规则"，避免误导为"导入 0 条"
                            newRules.isEmpty() && result.rules.isNotEmpty() ->
                                context.getString(R.string.import_no_new_rules)
                            result.droppedCount > 0 ->
                                context.getString(
                                    R.string.imported_rules_some_skipped,
                                    newRules.size,
                                    result.droppedCount
                                )
                            else ->
                                context.getString(R.string.successfully_imported_rules, newRules.size)
                        }
                    }
                }
            } catch (e: Exception) {
                exportImportMessage = context.getString(R.string.failed_to_import_rules, e.message ?: "")
            }
        }
    }

    if (showExportImportDialog) {
        NotixDialog(
            onDismiss = { showExportImportDialog = false },
            title = stringResource(R.string.export_import_rules),
            content = {
                Text(
                    text = stringResource(R.string.choose_action),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
            },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    NotixDialogButton(
                        onClick = {
                            showExportImportDialog = false
                            exportLauncher.launch("Notix_rules.json")
                        },
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.export)
                    )
                    Spacer(Modifier.width(8.dp))
                    NotixDialogButton(
                        onClick = {
                            showExportImportDialog = false
                            importLauncher.launch(arrayOf("application/json"))
                        },
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.import_rules)
                    )
                }
            }
        )
    }

    if (exportImportMessage != null) {
        NotixDialog(
            onDismiss = { exportImportMessage = null },
            title = stringResource(R.string.status),
            content = {
                Text(
                    text = exportImportMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
            },
            buttons = {
                NotixDialogButton(
                    onClick = { exportImportMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.ok)
                )
            }
        )
    }

    // Clear history — mode picker
    if (showClearModeDialog) {
        NotixDialog(
            onDismiss = { showClearModeDialog = false },
            title = stringResource(R.string.clear_history),
            content = {
                Text(
                    text = stringResource(R.string.clear_history_mode_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                NotixDialogButton(
                    onClick = {
                        showClearModeDialog = false
                        showClearAllConfirmDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.clear_all_history),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                NotixDialogButton(
                    onClick = {
                        showClearModeDialog = false
                        showClearByDateDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.clear_by_date_range)
                )
                Spacer(Modifier.height(8.dp))
                NotixDialogButton(
                    onClick = {
                        showClearModeDialog = false
                        selectedPackages = emptySet()
                        showClearByAppDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.clear_by_app)
                )
                Spacer(Modifier.height(8.dp))
            },
            buttons = {
                NotixDialogButton(
                    onClick = { showClearModeDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.cancel),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        )
    }

    // Clear history — clear all (two-step confirmation)
    if (showClearAllConfirmDialog) {
        NotixConfirmDialog(
            onDismiss = { showClearAllConfirmDialog = false },
            onConfirm = {
                showClearAllConfirmDialog = false
                onClearHistory()
            },
            title = stringResource(R.string.clear_all_history_confirm_title),
            body = stringResource(R.string.clear_all_history_confirm_body),
            confirmText = stringResource(R.string.clear_all_history)
        )
    }

    // Clear history — by date range
    if (showClearByDateDialog) {
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
        var showClearByDateConfirmDialog by remember { mutableStateOf(false) }
        NotixDialog(
            onDismiss = { showClearByDateDialog = false; dateError = null },
            title = stringResource(R.string.clear_by_date_range_title),
            content = {
                Text(
                    text = stringResource(R.string.clear_by_date_range_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                dateError?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(16.dp))
                DateField(
                    label = stringResource(R.string.start_date),
                    value = dateFormat.format(Date(startDateMillis)),
                    onClick = { showStartDatePicker = true }
                )
                Spacer(Modifier.height(8.dp))
                DateField(
                    label = stringResource(R.string.end_date),
                    value = dateFormat.format(Date(endDateMillis)),
                    onClick = { showEndDatePicker = true }
                )
                Spacer(Modifier.height(16.dp))
                // v7.50：快捷选项（7天/30天/90天/全部）——仅更新日期范围，不直接清除
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotixDialogButton(
                        onClick = {
                            cal.timeInMillis = System.currentTimeMillis()
                            endDateMillis = cal.timeInMillis
                            cal.add(Calendar.DAY_OF_MONTH, -6)
                            startDateMillis = cal.timeInMillis
                            dateError = null
                        },
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.clear_date_7d)
                    )
                    NotixDialogButton(
                        onClick = {
                            cal.timeInMillis = System.currentTimeMillis()
                            endDateMillis = cal.timeInMillis
                            cal.add(Calendar.DAY_OF_MONTH, -29)
                            startDateMillis = cal.timeInMillis
                            dateError = null
                        },
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.clear_date_30d)
                    )
                    NotixDialogButton(
                        onClick = {
                            cal.timeInMillis = System.currentTimeMillis()
                            endDateMillis = cal.timeInMillis
                            cal.add(Calendar.DAY_OF_MONTH, -89)
                            startDateMillis = cal.timeInMillis
                            dateError = null
                        },
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.clear_date_90d)
                    )
                    NotixDialogButton(
                        onClick = {
                            startDateMillis = 0L
                            endDateMillis = System.currentTimeMillis()
                            dateError = null
                        },
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.clear_date_all)
                    )
                }
                Spacer(Modifier.height(16.dp))
            },
            buttons = {
                NotixDangerButton(
                    onClick = { showClearByDateConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.clear_this_range_history)
                )
                Spacer(Modifier.height(8.dp))
                NotixDialogButton(
                    onClick = { showClearByDateDialog = false; dateError = null },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.cancel),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        )
        if (showClearByDateConfirmDialog) {
            NotixConfirmDialog(
                onDismiss = { showClearByDateConfirmDialog = false },
                onConfirm = {
                    val startMs = startDateMillis
                    cal.timeInMillis = endDateMillis
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    cal.add(Calendar.MILLISECOND, -1)
                    val endMs = cal.timeInMillis
                    if (endMs < startMs) {
                        dateError = context.getString(R.string.invalid_date_range)
                        showClearByDateConfirmDialog = false
                        return@NotixConfirmDialog
                    }
                    onClearHistoryByDate(startMs, endMs)
                    showClearByDateDialog = false
                    dateError = null
                    showClearByDateConfirmDialog = false
                    showMessage(context.getString(R.string.toast_history_cleared))
                },
                title = stringResource(R.string.clear_by_date_range_confirm_title),
                body = stringResource(R.string.clear_by_date_range_confirm_body),
                confirmText = stringResource(R.string.clear_this_range_history)
            )
        }
    }

    // DatePicker dialogs
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDateMillis = it }
                    showStartDatePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { endDateMillis = it }
                    showEndDatePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Clear history — by app
    if (showClearByAppDialog) {
        var showClearByAppConfirmDialog by remember { mutableStateOf(false) }
        NotixDialog(
            onDismiss = { showClearByAppDialog = false },
            title = stringResource(R.string.clear_by_app_title),
            content = {
                Text(
                    text = stringResource(R.string.clear_by_app_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                if (uniqueApps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_apps_in_history),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = appSearchQuery,
                            onValueChange = { appSearchQuery = it },
                            label = { Text(stringResource(R.string.search_apps)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        // v7.50：全选（切换式）——作用于当前 filteredApps
                        NotixDialogButton(
                            onClick = {
                                val filteredPkgs = filteredApps.map { it.first }.toSet()
                                selectedPackages = if (filteredPkgs.all { it in selectedPackages }) {
                                    selectedPackages - filteredPkgs
                                } else {
                                    selectedPackages + filteredPkgs
                                }
                            },
                            text = stringResource(R.string.select_all),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (filteredApps.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_results_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(filteredApps) { (pkg, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedPackages = if (pkg in selectedPackages) selectedPackages - pkg
                                            else selectedPackages + pkg
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RealAppIcon(
                                        packageName = pkg,
                                        appName = label,
                                        size = 32.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Checkbox(
                                        checked = pkg in selectedPackages,
                                        onCheckedChange = { checked ->
                                            selectedPackages = if (checked) selectedPackages + pkg
                                            else selectedPackages - pkg
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            },
            buttons = {
                NotixDangerButton(
                    onClick = { showClearByAppConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.clear_selected_apps_history)
                )
                Spacer(Modifier.height(8.dp))
                NotixDialogButton(
                    onClick = { showClearByAppDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.cancel),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        )
        if (showClearByAppConfirmDialog) {
            NotixConfirmDialog(
                onDismiss = { showClearByAppConfirmDialog = false },
                onConfirm = {
                    onClearHistoryByPackages(selectedPackages)
                    showClearByAppDialog = false
                    showClearByAppConfirmDialog = false
                    showMessage(context.getString(R.string.toast_history_cleared))
                },
                title = stringResource(R.string.clear_by_app_confirm_title),
                body = stringResource(R.string.clear_by_app_confirm_body),
                confirmText = stringResource(R.string.clear_selected_apps_history)
            )
        }
    }

    // v7.14：修复崩溃日志入口点击无反应——showCrashLogDialog 已声明但从未挂载 CrashLogDialog 渲染
    if (showCrashLogDialog) {
        CrashLogDialog(
            onDismiss = { showCrashLogDialog = false },
            onEnabledChanged = { crashLogEnabled = it }
        )
    }

    // v8.14：恢复常驻通知——确认弹窗（确认后恢复所有被规则冻结的常驻通知）
    if (showRestoreSnoozedDialog) {
        NotixConfirmDialog(
            onDismiss = { showRestoreSnoozedDialog = false },
            onConfirm = {
                val n = NotificationBlockerService.instance?.restoreAllSnoozedNotifications() ?: 0
                showRestoreSnoozedDialog = false
                showMessage(
                    if (n > 0) context.getString(R.string.settings_restore_snoozed_done, n)
                    else context.getString(R.string.settings_restore_snoozed_none)
                )
            },
            title = stringResource(R.string.settings_restore_snoozed_confirm_title),
            body = stringResource(R.string.settings_restore_snoozed_confirm_body),
            confirmText = stringResource(R.string.confirm),
            danger = false,
        )
    }

    // v7.25：移除顶部 TopAppBar（底部 tab 已有"设置"入口），内容从状态栏下直接开始
    Scaffold(
        // v7.24：应用内 Snackbar 提示（替代系统 Toast）
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // v8.2：设置页主标题（与历史/规则页 headlineMedium+Bold 统一）
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            SettingsSection(title = stringResource(R.string.settings_section_general)) {
                // v7.45：无文本通知文字提取开关（默认关）
                SettingsRow(
                    icon = Icons.Filled.TextFields,
                    title = stringResource(R.string.settings_extract_remoteviews_title),
                    subtitle = stringResource(R.string.settings_extract_remoteviews_desc),
                    onClick = null,
                    trailing = {
                        Switch(
                            checked = extractRemoteViewsEnabled,
                            onCheckedChange = { enabled ->
                                extractRemoteViewsEnabled = enabled
                                NotificationBlockerService.setRemoteViewsTextExtractionEnabled(context, enabled)
                            }
                        )
                    }
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Filled.History,
                    title = stringResource(R.string.clear_history),
                    subtitle = stringResource(R.string.clear_history_desc),
                    onClick = { showClearModeDialog = true },
                    trailing = { NavChevron() }
                )
                // v7.50：存储占用入口（常规分区）
                RowDivider()
                SettingsRow(
                    icon = Icons.Filled.Storage,
                    title = stringResource(R.string.settings_storage_usage),
                    subtitle = formatStorageBytes(storageTotalBytes),
                    onClick = { showStorageUsageScreen = true },
                    trailing = { NavChevron() }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_rules)) {
                SettingsRow(
                    icon = Icons.Filled.ImportExport,
                    title = stringResource(R.string.export_import_rules),
                    subtitle = stringResource(R.string.export_import_rules_desc),
                    onClick = { showExportImportDialog = true },
                    trailing = { NavChevron() }
                )
                // v8.14：恢复常驻通知——恢复所有被规则冻结（snooze）的常驻通知
                RowDivider()
                SettingsRow(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.settings_restore_snoozed),
                    subtitle = stringResource(R.string.settings_restore_snoozed_desc),
                    onClick = { showRestoreSnoozedDialog = true },
                    trailing = { NavChevron() }
                )
            }

            // v7.13：崩溃日志入口
            SettingsSection(title = stringResource(R.string.settings_crash_log)) {
                SettingsRow(
                    icon = Icons.Filled.BugReport,
                    title = stringResource(R.string.settings_crash_log),
                    subtitle = stringResource(
                        if (crashLogEnabled) R.string.settings_crash_log_summary_on
                        else R.string.settings_crash_log_summary_off
                    ),
                    onClick = { showCrashLogDialog = true },
                    trailing = { NavChevron() }
                )
            }

            // v8.6：权限管理——入口（仅展示实时状态，点击进入二级界面）
            SettingsSection(title = stringResource(R.string.settings_permission_section_title)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPermissionScreen = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_permission_section_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.settings_permission_monitoring_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    if (permFailedCount == 0) {
                        Text(
                            text = stringResource(R.string.settings_permission_all_normal),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.settings_permission_n_abnormal, permFailedCount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    NavChevron()
                }
            }

            // v7.18：关于分组（功能介绍 / 隐私与安全，页面内展开折叠，默认收起）
            SettingsSection(title = stringResource(R.string.about)) {
                ExpandableSection(
                    title = stringResource(R.string.about_features_title),
                    expanded = aboutFeaturesExpanded,
                    onToggle = { aboutFeaturesExpanded = !aboutFeaturesExpanded },
                ) {
                    Text(
                        text = stringResource(R.string.about_features_body),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ExpandableSection(
                    title = stringResource(R.string.settings_privacy_security_title),
                    expanded = aboutPrivacyExpanded,
                    onToggle = { aboutPrivacyExpanded = !aboutPrivacyExpanded },
                ) {
                    val privacyItems = listOf(
                        R.string.settings_privacy_item_1,
                        R.string.settings_privacy_item_2,
                        R.string.settings_privacy_item_3,
                        R.string.settings_privacy_item_4,
                        R.string.settings_privacy_item_5
                    )
                    privacyItems.forEach { resId ->
                        Text(
                            text = "\u2022 ${stringResource(resId)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            val packageInfo = try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: Exception) {
                null
            }
            val versionName = packageInfo?.versionName ?: stringResource(R.string.unknown)

            Text(
                text = stringResource(R.string.app_version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    // v8.6→重构：权限管理改为弹窗（不再全屏路由）
    if (showPermissionScreen) {
        PermissionScreen(
            onBack = {
                showPermissionScreen = false
                permissionRefreshTick++
            }
        )
    }

    // v7.50→重构：存储占用改为弹窗
    if (showStorageUsageScreen) {
        StorageUsageScreen(
            onBack = {
                showStorageUsageScreen = false
                storageRefreshTick++
            },
            onClearHistory = onClearHistory,
            onClearRules = onClearRules
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 28.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun NavChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** 关于分组内的可展开/折叠子项，默认由调用方控制展开状态 */
@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) 180f else 0f)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 14.dp
                )
            ) {
                content()
            }
        }
        RowDivider()
    }
}

/** Cap on the size of an imported rules document (defends against OOM from a corrupt file). */
private const val MAX_IMPORT_BYTES = 5 * 1024 * 1024

/**
 * Reads the document as UTF-8 text, but never buffers more than [maxBytes]. Returns null if the
 * document exceeds the cap (so the caller can report "file too large" instead of risking OOM).
 */
private fun readCappedText(context: Context, uri: Uri, maxBytes: Int): String? {
    context.contentResolver.openInputStream(uri)?.use { input ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) return null
            out.write(buffer, 0, read)
        }
        return out.toString(Charsets.UTF_8.name())
    }
    return null
}

/** v7.50：清除历史按时间段弹窗中的日期选择项（点击弹出 DatePicker） */
@Composable
private fun DateField(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// v8.6：权限管理二级界面——实时监听 4 项系统权限（已改为弹窗）
@Composable
fun PermissionScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }

    // 实时状态：每次 refreshTick 变化时重算（进入前台/从系统设置返回时刷新）
    val listenerGranted = remember(refreshTick) { SetupState.isNotificationListenerEnabled(context) }
    val postNotifGranted = remember(refreshTick) { SetupState.isPostNotificationsGranted(context) }
    val batteryGranted = remember(refreshTick) { SetupState.isIgnoringBatteryOptimizations(context) }
    val foregroundGranted = remember(refreshTick) { isKeepaliveServiceRunning(context) }
    val failedCount = listOf(listenerGranted, postNotifGranted, batteryGranted, foregroundGranted)
        .count { !it }

    // 从系统设置返回 / 进入前台时重新检查
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NotixDialog(
        onDismiss = onBack,
        title = stringResource(R.string.settings_permission_section_title),
        content = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    // 实时监控中 徽标
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_permission_monitoring_pill),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_permission_monitor_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // 聚合告警横幅
                if (failedCount > 0) {
                    item {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.settings_permission_abnormal_banner, failedCount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                item {
                    PermissionCard(
                        icon = Icons.Filled.Notifications,
                        title = stringResource(R.string.settings_permission_notification_title),
                        desc = stringResource(R.string.settings_permission_notification_desc),
                        granted = listenerGranted,
                        permName = "BIND_NOTIFICATION_LISTENER_SERVICE",
                        usedBy = stringResource(R.string.settings_permission_notification_usedby),
                        fixLabel = stringResource(R.string.settings_permission_go_to_settings),
                        onFix = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                item {
                    PermissionCard(
                        icon = Icons.AutoMirrored.Filled.Send,
                        title = stringResource(R.string.settings_permission_postnotif_title),
                        desc = stringResource(R.string.settings_permission_postnotif_desc),
                        granted = postNotifGranted,
                        permName = "POST_NOTIFICATIONS",
                        usedBy = stringResource(R.string.settings_permission_postnotif_usedby),
                        fixLabel = stringResource(R.string.settings_permission_go_to_settings),
                        onFix = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                item {
                    PermissionCard(
                        icon = Icons.Filled.BatteryAlert,
                        title = stringResource(R.string.settings_permission_battery_title),
                        desc = stringResource(R.string.settings_permission_battery_desc),
                        granted = batteryGranted,
                        permName = "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
                        usedBy = stringResource(R.string.settings_permission_battery_usedby),
                        fixLabel = stringResource(R.string.settings_permission_battery_one_click),
                        onFix = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                item {
                    PermissionCard(
                        icon = Icons.Filled.Security,
                        title = stringResource(R.string.settings_permission_foreground_title),
                        desc = stringResource(R.string.settings_permission_foreground_desc),
                        granted = foregroundGranted,
                        permName = "FOREGROUND_SERVICE + FOREGROUND_SERVICE_SPECIAL_USE",
                        usedBy = stringResource(R.string.settings_permission_foreground_usedby),
                        fixLabel = stringResource(R.string.settings_permission_go_to_settings),
                        onFix = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    desc: String,
    granted: Boolean,
    permName: String,
    usedBy: String,
    fixLabel: String,
    onFix: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onFix),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (granted) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (granted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (granted) stringResource(R.string.settings_permission_status_normal)
                                else stringResource(R.string.settings_permission_status_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (granted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(8.dp))
                // 整卡可点击跳转对应系统设置，用箭头提示可交互
                NavChevron()
            }
            Spacer(Modifier.height(8.dp))
            // 权限标识（Android 常量，跨语言一致）
            Text(
                text = permName,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            // 使用组件：标注该权限被 App 中哪个组件用到
            Text(
                text = stringResource(R.string.settings_permission_usedby_label) + "：" + usedBy,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!granted) {
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(
                        onClick = onFix,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(fixLabel, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** 前台服务（通知守护）是否正在以 fg 身份运行——用于「前台服务保活」实时监控 */
private fun isKeepaliveServiceRunning(context: Context): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return am.getRunningServices(Int.MAX_VALUE).any {
        it.service.className == NotificationBlockerService::class.java.name && it.foreground
    }
}
