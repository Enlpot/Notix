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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Palette
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
import com.enlpot.notix.ui.components.NotixSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.enlpot.notix.AppInfoStorage
import com.enlpot.notix.CrashLogManager
import com.enlpot.notix.ImportError
import com.enlpot.notix.ImportResult
import com.enlpot.notix.NotificationBlockerService
import com.enlpot.notix.NotificationColorEngine
import com.enlpot.notix.R
import com.enlpot.notix.RuleExport
import com.enlpot.notix.RuleExportSerializer
import com.enlpot.notix.RuleImport
import com.enlpot.notix.RuleStorage
import com.enlpot.notix.UnmonitoredAppsStorage
import com.enlpot.notix.RuleWizardSupport
import com.enlpot.notix.setup.SetupState
import com.enlpot.notix.SimpleNotification
import com.enlpot.notix.ui.components.CrashLogDialog
import com.enlpot.notix.ui.components.NotixConfirmDialog
import com.enlpot.notix.ui.components.NotixDangerButton
import com.enlpot.notix.ui.components.NotixDialog
import com.enlpot.notix.ui.components.NotixDialogButton
import com.enlpot.notix.ui.components.RealAppIcon
import com.enlpot.notix.ui.components.SectionHeader
import com.enlpot.notix.ui.theme.*
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
    // v8.31：未监控应用状态由 MainActivity 统一管理，确保历史页和设置页同步刷新
    unmonitoredApps: Set<String> = emptySet(),
    onUnmonitoredAppsChanged: (Set<String>) -> Unit = {},
    scrollToTopTrigger: Int = 0
) {
    val context = LocalContext.current
    val ruleStorage = remember { RuleStorage(context) }

    // v8.46.1：改用系统 Toast——Snackbar 是页面内组件，会被弹窗（Dialog）阴影遮挡；
    // 系统 Toast 是系统级窗口，始终显示在所有 app 窗口（含弹窗）之上，不会被遮挡
    fun showMessage(msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
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

    // v8.20：动态取色开关（默认开）
    var dynamicColorEnabled by remember {
        mutableStateOf(NotificationColorEngine.isDynamicColorEnabled(context))
    }

    // v8.43.0：词云相关设置
    var dailyRebuildEnabled by remember {
        mutableStateOf(com.enlpot.notix.data.repository.WordFrequencyRepository.isDailyRebuildEnabled(context))
    }
    var pluginInstalled by remember {
        mutableStateOf(com.enlpot.notix.plugin.WordTokenizerManager.isPluginLoaded())
    }
    var pluginInstalling by remember { mutableStateOf(false) }
    var pluginInstallProgress by remember { mutableStateOf(0) }
    var pluginStage by remember { mutableStateOf<com.enlpot.notix.plugin.WordTokenizerManager.InstallStage?>(null) }
    val pluginScope = rememberCoroutineScope()
    // v8.45.1：插件市场弹窗状态
    var showPluginMarketDialog by remember { mutableStateOf(false) }
    var pluginMarketQuery by remember { mutableStateOf("") }
    var pluginToUninstall by remember { mutableStateOf<PluginInfo?>(null) }
    // v8.46.0：镜像源管理
    var showMirrorDialog by remember { mutableStateOf(false) }
    var mirrorLatencies by remember { mutableStateOf<Map<String, Long?>>(emptyMap()) }
    var mirrorTesting by remember { mutableStateOf(false) }
    var mirrorAddMode by remember { mutableStateOf(false) }
    var mirrorInput by remember { mutableStateOf("") }
    var pluginInstallError by remember { mutableStateOf<String?>(null) }

    // v8.21：未监控应用管理（v8.31：状态由 MainActivity 统一管理，确保历史页和设置页同步刷新）
    val unmonitoredStorage = remember { UnmonitoredAppsStorage(context) }
    var showUnmonitoredAppsDialog by remember { mutableStateOf(false) }
    var unmonitoredSearchQuery by remember { mutableStateOf("") }
    var selectedUnmonitoredPackages by remember { mutableStateOf(setOf<String>()) }
    val unmonitoredAppList = remember(unmonitoredApps) {
        val pm = context.packageManager
        unmonitoredApps.map { pkg ->
            val label = try {
                val flags = android.content.pm.PackageManager.GET_UNINSTALLED_PACKAGES or android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, flags)).toString()
            } catch (_: Exception) { pkg }
            pkg to label
        }.sortedBy { it.second.lowercase() }
    }
    val filteredUnmonitoredApps = remember(unmonitoredAppList, unmonitoredSearchQuery) {
        if (unmonitoredSearchQuery.isBlank()) unmonitoredAppList
        else unmonitoredAppList.filter { (pkg, label) ->
            pkg.contains(unmonitoredSearchQuery, ignoreCase = true) || label.contains(unmonitoredSearchQuery, ignoreCase = true)
        }
    }

    // Clear history — two-phase: pick mode → detail dialog
    var showClearModeDialog by remember { mutableStateOf(false) }
    var showClearByDateDialog by remember { mutableStateOf(false) }
    var showClearByAppDialog by remember { mutableStateOf(false) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    // v8.14：恢复常驻通知——确认弹窗状态
    var showRestoreSnoozedDialog by remember { mutableStateOf(false) }

    // v8.31：刷新应用信息——清除缓存的应用名称和图标，下次通知到达时重新获取
    var showRefreshAppInfoDialog by remember { mutableStateOf(false) }

    // v8.17：关于分组改为弹窗形式（功能介绍 / 隐私与安全）
    var showAboutFeaturesDialog by remember { mutableStateOf(false) }
    var showAboutPrivacyDialog by remember { mutableStateOf(false) }

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
            contentScrollable = false,
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

    // v8.21：未监控应用管理弹窗（多选+搜索，恢复监控）
    if (showUnmonitoredAppsDialog) {
        NotixDialog(
            onDismiss = {
                showUnmonitoredAppsDialog = false
                unmonitoredSearchQuery = ""
                selectedUnmonitoredPackages = setOf()
            },
            title = stringResource(R.string.settings_unmonitored_apps_title),
            contentScrollable = false,
            content = {
                Text(
                    text = stringResource(R.string.unmonitored_apps_dialog_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                if (unmonitoredAppList.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_unmonitored_apps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = unmonitoredSearchQuery,
                            onValueChange = { unmonitoredSearchQuery = it },
                            label = { Text(stringResource(R.string.search_apps)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        NotixDialogButton(
                            onClick = {
                                val filteredPkgs = filteredUnmonitoredApps.map { it.first }.toSet()
                                selectedUnmonitoredPackages = if (filteredPkgs.all { it in selectedUnmonitoredPackages }) {
                                    selectedUnmonitoredPackages - filteredPkgs
                                } else {
                                    selectedUnmonitoredPackages + filteredPkgs
                                }
                            },
                            text = stringResource(R.string.select_all),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (filteredUnmonitoredApps.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_results_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(filteredUnmonitoredApps) { (pkg, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedUnmonitoredPackages = if (pkg in selectedUnmonitoredPackages) selectedUnmonitoredPackages - pkg
                                            else selectedUnmonitoredPackages + pkg
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
                                        checked = pkg in selectedUnmonitoredPackages,
                                        onCheckedChange = { checked ->
                                            selectedUnmonitoredPackages = if (checked) selectedUnmonitoredPackages + pkg
                                            else selectedUnmonitoredPackages - pkg
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
                NotixDialogButton(
                    onClick = {
                        selectedUnmonitoredPackages.forEach { pkg ->
                            unmonitoredStorage.removeApp(pkg)
                        }
                        // v8.31：通知 MainActivity 更新未监控应用状态，确保历史页同步刷新
                        onUnmonitoredAppsChanged(unmonitoredStorage.getUnmonitoredApps().toSet())
                        showUnmonitoredAppsDialog = false
                        unmonitoredSearchQuery = ""
                        selectedUnmonitoredPackages = setOf()
                        showMessage(context.getString(R.string.toast_monitoring_resumed))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.resume_selected_apps)
                )
                Spacer(Modifier.height(8.dp))
                NotixDialogButton(
                    onClick = {
                        showUnmonitoredAppsDialog = false
                        unmonitoredSearchQuery = ""
                        selectedUnmonitoredPackages = setOf()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.cancel),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        )
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

    // v8.45.1：添加插件弹窗（搜索 + 插件清单，已安装变灰）
    if (showPluginMarketDialog) {
        NotixDialog(
            onDismiss = {
                showPluginMarketDialog = false
                pluginMarketQuery = ""
                pluginInstallError = null
            },
            title = "添加插件",
            titleTrailing = {
                // v8.46.0：镜像源管理入口（标题行右侧）
                Text(
                    text = "镜像源",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.notix.primary,
                    modifier = Modifier
                        .clip(NotixCorner.Control)
                        .clickable {
                            pluginInstallError = null
                            showMirrorDialog = true
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            },
            contentScrollable = false,
            content = {
                OutlinedTextField(
                    value = pluginMarketQuery,
                    onValueChange = { pluginMarketQuery = it },
                    label = { Text("搜索插件") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                val filteredPlugins = pluginCatalog.filter {
                    pluginMarketQuery.isBlank() ||
                        it.title.contains(pluginMarketQuery.trim(), ignoreCase = true) ||
                        it.description.contains(pluginMarketQuery.trim(), ignoreCase = true)
                }
                if (filteredPlugins.isEmpty()) {
                    Text(
                        text = "未找到相关插件",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(filteredPlugins) { plugin ->
                            val installed = pluginInstalled && plugin.id == "hanlp"
                            PluginMarketRow(
                                plugin = plugin,
                                installed = installed,
                                installing = pluginInstalling,
                                installProgress = pluginInstallProgress,
                                installStage = pluginStage,
                                onInstall = {
                                    if (!installed && !pluginInstalling) {
                                        pluginInstalling = true
                                        pluginInstallProgress = 0
                                        pluginStage = com.enlpot.notix.plugin.WordTokenizerManager.InstallStage.DOWNLOADING
                                        pluginInstallError = null
                                        pluginScope.launch {
                                            val result = com.enlpot.notix.plugin.WordTokenizerManager.downloadAndInstallPlugin(
                                                context,
                                                { stage, progress ->
                                                    pluginStage = stage
                                                    if (stage == com.enlpot.notix.plugin.WordTokenizerManager.InstallStage.DOWNLOADING) {
                                                        pluginInstallProgress = progress
                                                    }
                                                },
                                                { _ -> }
                                            )
                                            pluginInstalling = false
                                            pluginStage = null
                                            when (result) {
                                                is com.enlpot.notix.plugin.WordTokenizerManager.PluginInstallResult.Success -> {
                                                    pluginInstalled = true
                                                    pluginInstallError = null
                                                }
                                                is com.enlpot.notix.plugin.WordTokenizerManager.PluginInstallResult.Failure -> {
                                                    pluginInstalled = false
                                                    pluginInstallError = result.reason
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                // v8.46.0：安装失败错误提示 + 切换镜像源入口
                pluginInstallError?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "安装失败：$err",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        NotixDialogButton(
                            onClick = { showMirrorDialog = true },
                            text = "切换镜像源",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        )
    }

    // v8.45.1：卸载插件二次确认
    pluginToUninstall?.let { plugin ->
        NotixConfirmDialog(
            onDismiss = { pluginToUninstall = null },
            onConfirm = {
                com.enlpot.notix.plugin.WordTokenizerManager.unloadPlugin(context)
                pluginInstalled = false
                pluginToUninstall = null
                showMessage("已卸载「${plugin.title}」")
            },
            title = "卸载插件",
            body = "确定卸载「${plugin.title}」吗？卸载后将恢复内置简单分词。",
            confirmText = "卸载",
        )
    }

    // v8.46.0：镜像源管理弹窗（打开自动测连通；添加/删除；官方固定）
    if (showMirrorDialog) {
        LaunchedEffect(showMirrorDialog) {
            if (showMirrorDialog) {
                mirrorTesting = true
                val sources = listOf(com.enlpot.notix.plugin.WordTokenizerManager.getOfficialPrefix()) +
                    com.enlpot.notix.plugin.WordTokenizerManager.getMirrorPrefixes(context)
                val result = mutableMapOf<String, Long?>()
                sources.forEach { p ->
                    result[p] = com.enlpot.notix.plugin.WordTokenizerManager.testLatency(p)
                }
                mirrorLatencies = result
                mirrorTesting = false
            }
        }
        NotixDialog(
            onDismiss = {
                showMirrorDialog = false
                mirrorAddMode = false
                mirrorInput = ""
            },
            title = "镜像源管理",
            titleTrailing = {
                Text(
                    text = "添加（${com.enlpot.notix.plugin.WordTokenizerManager.getMirrorPrefixes(context).size}/${com.enlpot.notix.plugin.WordTokenizerManager.MAX_MIRROR_COUNT}）",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.notix.primary,
                    modifier = Modifier
                        .clip(NotixCorner.Control)
                        .clickable {
                            if (com.enlpot.notix.plugin.WordTokenizerManager.getMirrorPrefixes(context).size >= com.enlpot.notix.plugin.WordTokenizerManager.MAX_MIRROR_COUNT) {
                                showMessage("最多可添加 ${com.enlpot.notix.plugin.WordTokenizerManager.MAX_MIRROR_COUNT} 个镜像源")
                            } else {
                                mirrorAddMode = !mirrorAddMode
                                mirrorInput = ""
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            },
            contentScrollable = false,
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 官方源（固定不可删）
                    MirrorSourceRow(
                        label = "GitHub 官方",
                        latency = mirrorLatencies[""],
                        fixed = true,
                        onRemove = null
                    )
                    // 用户镜像源
                    com.enlpot.notix.plugin.WordTokenizerManager.getMirrorPrefixes(context).forEach { prefix ->
                        MirrorSourceRow(
                            label = prefix,
                            latency = mirrorLatencies[prefix],
                            fixed = false,
                            onRemove = {
                                com.enlpot.notix.plugin.WordTokenizerManager.removeMirror(context, prefix)
                                mirrorLatencies = mirrorLatencies - prefix
                            }
                        )
                    }
                    if (mirrorTesting) {
                        Text(
                            text = "正在测试连通性…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.notix.contentSecondary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
                // 添加镜像源模式
                if (mirrorAddMode) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = mirrorInput,
                        onValueChange = {
                            // v8.46.0：输入法可能把英文标点转成全角，统一转半角再保存
                            mirrorInput = toHalfWidth(it)
                        },
                        label = { Text("镜像源前缀（如 https://gh-proxy.com）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    // v8.46.1：添加源无需先测连通性——输入后直接添加，添加完自动测延迟
                    NotixDialogButton(
                        onClick = {
                            val p = mirrorInput.trim().trimEnd('/')
                            if (p.isNotBlank()) {
                                val err = com.enlpot.notix.plugin.WordTokenizerManager.addMirror(context, p)
                                if (err == null) {
                                    mirrorLatencies = mirrorLatencies + (p to null)  // 先标记测试中
                                    mirrorInput = ""
                                    mirrorAddMode = false
                                    showMessage("已添加镜像源")
                                    // 自动测连通性并更新列表
                                    pluginScope.launch {
                                        val lat = com.enlpot.notix.plugin.WordTokenizerManager.testLatency(p)
                                        mirrorLatencies = mirrorLatencies + (p to lat)
                                    }
                                } else {
                                    showMessage(err)
                                }
                            } else {
                                showMessage("请输入镜像源前缀")
                            }
                        },
                        text = "添加",
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.notix.primary,
                        contentColor = MaterialTheme.notix.onPrimary
                    )
                }
            }
        )
    }

    // v8.31：刷新应用信息——确认弹窗（清除缓存的应用名称和图标）
    if (showRefreshAppInfoDialog) {
        NotixConfirmDialog(
            onDismiss = { showRefreshAppInfoDialog = false },
            onConfirm = {
                val storage = AppInfoStorage(context)
                val pm = context.packageManager
                val flags = android.content.pm.PackageManager.GET_UNINSTALLED_PACKAGES or
                        android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
                val allApps = storage.getAllApps()
                var refreshedCount = 0
                allApps.forEach { (packageName, _) ->
                    try {
                        val appInfo = pm.getApplicationInfo(packageName, flags)
                        val appName = pm.getApplicationLabel(appInfo).toString()
                        val icon = appInfo.loadIcon(pm)
                        storage.saveAppInfo(packageName, appName, icon)
                        refreshedCount++
                    } catch (_: Exception) {
                        // 获取失败的 app 保留原有缓存
                    }
                }
                showRefreshAppInfoDialog = false
                showMessage("已重新获取 $refreshedCount 个应用的名称和图标")
            },
            title = "刷新应用信息",
            body = "将立即根据包名从系统重新获取所有已缓存应用的名称和图标，覆盖现有缓存。此操作不会删除通知历史。",
            confirmText = stringResource(R.string.confirm),
            danger = false,
        )
    }

    // v8.17：关于——功能介绍弹窗
    if (showAboutFeaturesDialog) {
        NotixDialog(
            onDismiss = { showAboutFeaturesDialog = false },
            title = stringResource(R.string.about_features_title),
            content = {
                Text(
                    text = stringResource(R.string.about_features_body),
                    style = MaterialTheme.notixType.bodySecondary,
                    color = MaterialTheme.notix.contentSecondary
                )
            },
            buttons = {
                NotixDialogButton(
                    onClick = { showAboutFeaturesDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.close),
                    containerColor = MaterialTheme.notix.primary,
                    contentColor = MaterialTheme.notix.onPrimary
                )
            }
        )
    }

    // v8.17：关于——隐私与安全弹窗
    if (showAboutPrivacyDialog) {
        NotixDialog(
            onDismiss = { showAboutPrivacyDialog = false },
            title = stringResource(R.string.settings_privacy_security_title),
            content = {
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
                        style = MaterialTheme.notixType.bodySecondary,
                        color = MaterialTheme.notix.contentSecondary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            },
            buttons = {
                NotixDialogButton(
                    onClick = { showAboutPrivacyDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.close),
                    containerColor = MaterialTheme.notix.primary,
                    contentColor = MaterialTheme.notix.onPrimary
                )
            }
        )
    }

    // v8.16：吸顶标题——左上角"设置"随内容自然上滑，到达状态栏下沿后吸顶固定
    val scrollState = rememberScrollState()

    // v8.33：底部设置tab单击回到顶部
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            scrollState.animateScrollTo(0)
        }
    }
    val density = LocalDensity.current

    Scaffold(
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 注意：不应用 innerPadding——父级 MainActivity Scaffold 已处理状态栏/导航栏 inset，
                // 再次应用会导致双重顶部 padding，标题与状态栏间隙过大
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(scrollState)
            ) {
                // 标题区域占位（原标题：8dp + display 34sp + 8dp ≈ 50dp）
                Spacer(modifier = Modifier.height(50.dp))
            // ===== 常规：不属于功能分区的全局/系统设置 =====
            SettingsSection(title = stringResource(R.string.settings_section_general)) {
                SettingsRow(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.settings_dynamic_color_title),
                    subtitle = stringResource(R.string.settings_dynamic_color_desc),
                    onClick = null,
                    trailing = {
                        NotixSwitch(
                            checked = dynamicColorEnabled,
                            onCheckedChange = { enabled ->
                                dynamicColorEnabled = enabled
                                NotificationColorEngine.setDynamicColorEnabled(context, enabled)
                            }
                        )
                    }
                )

                SettingsRow(
                    icon = Icons.Filled.Refresh,
                    title = "刷新应用信息",
                    subtitle = "立即根据包名重新获取所有应用的名称和图标",
                    onClick = { showRefreshAppInfoDialog = true },
                )

                // Stage 8：Token 化颜色 / 字体 / 间距；保留 Notix 图标圆圈视觉
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = NotixCorner.Card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.notix.surfaceVariant)
                ) {
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
                            .background(MaterialTheme.notix.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.notix.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_permission_section_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.notix.contentPrimary
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.settings_permission_monitoring_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 18.sp,
                            color = MaterialTheme.notix.contentSecondary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    if (permFailedCount == 0) {
                        Text(
                            text = stringResource(R.string.settings_permission_all_normal),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.notix.primary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.settings_permission_n_abnormal, permFailedCount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.notix.error
                        )
                    }
                }
                }
            }

            // ===== 插件：可选功能插件 =====
            SettingsSection(title = stringResource(R.string.settings_section_plugins)) {
                // v8.45.1：添加插件入口（置顶）
                SettingsRow(
                    icon = Icons.Filled.Extension,
                    title = "添加插件",
                    subtitle = "浏览并安装更多插件",
                    onClick = { showPluginMarketDialog = true },
                    trailing = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.notix.contentTertiary)
                    }
                )

                // v8.45.1：已安装插件卡片（右侧卸载，二次确认）
                if (pluginInstalled) {
                    SettingsRow(
                        icon = Icons.Filled.TextFields,
                        title = "高级分词插件",
                        subtitle = "HanLP 高级分词，分词更精准",
                        onClick = null,
                        trailing = {
                            Text(
                                text = "卸载",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clip(NotixCorner.Card)
                                    .clickable { pluginToUninstall = pluginCatalog.first() }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    )
                }

            }

            // ===== 通知：通知处理与通知历史数据管理 =====
            SettingsSection(title = stringResource(R.string.settings_section_notification)) {
                SettingsRow(
                    icon = Icons.Filled.TextFields,
                    title = stringResource(R.string.settings_extract_remoteviews_title),
                    subtitle = stringResource(R.string.settings_extract_remoteviews_desc),
                    onClick = null,
                    trailing = {
                        NotixSwitch(
                            checked = extractRemoteViewsEnabled,
                            onCheckedChange = { enabled ->
                                extractRemoteViewsEnabled = enabled
                                NotificationBlockerService.setRemoteViewsTextExtractionEnabled(context, enabled)
                            }
                        )
                    }
                )

                SettingsRow(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.settings_restore_snoozed),
                    subtitle = stringResource(R.string.settings_restore_snoozed_desc),
                    onClick = { showRestoreSnoozedDialog = true },
                )

                SettingsRow(
                    icon = Icons.Filled.NotificationsOff,
                    title = stringResource(R.string.settings_unmonitored_apps_title),
                    subtitle = if (unmonitoredApps.isNotEmpty()) stringResource(R.string.unmonitored_apps, unmonitoredApps.size) else stringResource(R.string.settings_unmonitored_apps_desc),
                    onClick = { showUnmonitoredAppsDialog = true },
                )

                SettingsRow(
                    icon = Icons.Filled.History,
                    title = stringResource(R.string.clear_history),
                    subtitle = stringResource(R.string.clear_history_desc),
                    onClick = { showClearModeDialog = true },
                )

                SettingsRow(
                    icon = Icons.Filled.Storage,
                    title = stringResource(R.string.settings_storage_usage),
                    subtitle = formatStorageBytes(storageTotalBytes),
                    onClick = { showStorageUsageScreen = true },
                )
            }

            // ===== 规则 =====
            SettingsSection(title = stringResource(R.string.settings_section_rules)) {
                SettingsRow(
                    icon = Icons.Filled.ImportExport,
                    title = stringResource(R.string.export_import_rules),
                    subtitle = stringResource(R.string.export_import_rules_desc),
                    onClick = { showExportImportDialog = true },
                )
            }

            // ===== 统计 =====
            SettingsSection(title = stringResource(R.string.settings_section_statistics)) {
                SettingsRow(
                    icon = Icons.Filled.Refresh,
                    title = "每日重建词频",
                    subtitle = "每天凌晨3点全量重建词频，防止增量误差（默认关闭）",
                    onClick = null,
                    trailing = {
                        NotixSwitch(
                            checked = dailyRebuildEnabled,
                            onCheckedChange = { enabled ->
                                dailyRebuildEnabled = enabled
                                com.enlpot.notix.data.repository.WordFrequencyRepository.setDailyRebuildEnabled(context, enabled)
                            }
                        )
                    }
                )
            }

            // ===== 关于 =====
            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                SettingsRow(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.about_features_title),
                    onClick = { showAboutFeaturesDialog = true }
                )

                SettingsRow(
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.settings_privacy_security_title),
                    onClick = { showAboutPrivacyDialog = true }
                )

                SettingsRow(
                    icon = Icons.Filled.BugReport,
                    title = stringResource(R.string.settings_crash_log),
                    subtitle = stringResource(
                        if (crashLogEnabled) R.string.settings_crash_log_summary_on
                        else R.string.settings_crash_log_summary_off
                    ),
                    onClick = { showCrashLogDialog = true },
                )
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
                color = MaterialTheme.notix.contentSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // v8.16：吸顶时顶部背景条——标题到达顶部后渐显，盖住滚动内容避免重叠
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .graphicsLayer {
                    val threshold = with(density) { 4.dp.toPx() }
                    alpha = (scrollState.value.toFloat() / threshold).coerceIn(0f, 1f)
                }
                .background(MaterialTheme.colorScheme.background)
        )

        // v8.16：吸顶标题——左上角随内容自然上滑，到达状态栏下沿后吸顶固定，下滑还原
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.notixType.display,
            color = MaterialTheme.notix.contentPrimary,
            modifier = Modifier.graphicsLayer {
                val startX = with(density) { 16.dp.toPx() }
                val startY = with(density) { 4.dp.toPx() }
                translationX = startX
                // 自然上滑：Y = 原位置 - 滚动量，到顶后（Y>=0）吸顶
                translationY = maxOf(startY - scrollState.value.toFloat(), 0f)
            }
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

/** v8.46.0：全角字符转半角（输入法可能把英文标点自动转成全角，如 ：→: 。→. ） */
private fun toHalfWidth(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
        val c = ch.code
        sb.append(
            when (c) {
                0x3002 -> '.' // 。（CJK 句号，不在 FF01..FF5E 范围）
                0x3001 -> ',' // 、
                0x3000 -> ' ' // 全角空格
                in 0xFF01..0xFF5E -> (c - 0xFEE0).toChar() // 全角 ASCII 转半角
                else -> ch
            }
        )
    }
    return sb.toString()
}

@Composable
private fun MirrorSourceRow(
    label: String,
    latency: Long?,
    fixed: Boolean,
    onRemove: (() -> Unit)?
) {
    val c = MaterialTheme.notix
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 连通性指示灯：绿=正常(<300ms) 黄=较慢(<1000ms) 红=超时 灰=测试中
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(
                    when {
                        latency == null -> c.contentTertiary
                        latency < 0 -> MaterialTheme.colorScheme.error
                        latency < 300 -> Color(0xFF4CAF50)
                        latency < 1000 -> Color(0xFFFFC107)
                        else -> MaterialTheme.colorScheme.error
                    }
                )
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = c.contentPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        // v8.46.0：延迟列固定宽度右对齐（ms 右端对齐，数字往左增长）
        Box(modifier = Modifier.width(76.dp)) {
            Text(
                text = when {
                    latency == null -> "测试中"
                    latency < 0 -> "超时"
                    else -> "${latency}ms"
                },
                style = MaterialTheme.typography.labelSmall,
                color = c.contentSecondary,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.width(10.dp))
        // v8.46.0：操作列固定宽度右对齐（官方源空占位），保证延迟列右端一致、ms 上下对齐
        Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.CenterEnd) {
            if (!fixed && onRemove != null) {
                Text(
                    text = "删除",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clip(NotixCorner.Control)
                        .clickable { onRemove() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// v8.45.1：插件市场——插件清单数据（可扩展）
private data class PluginInfo(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
)

private val pluginCatalog = listOf(
    PluginInfo(
        id = "hanlp",
        title = "高级分词插件",
        description = "HanLP 高级分词，分词更精准",
        icon = Icons.Filled.TextFields
    )
)

@Composable
private fun PluginMarketRow(
    plugin: PluginInfo,
    installed: Boolean,
    installing: Boolean,
    installProgress: Int,
    installStage: com.enlpot.notix.plugin.WordTokenizerManager.InstallStage?,
    onInstall: () -> Unit
) {
    val c = MaterialTheme.notix
    val sp = MaterialTheme.notixSpacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(c.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = plugin.icon,
                contentDescription = null,
                tint = c.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(sp.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plugin.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (installed) c.contentTertiary else c.contentPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = plugin.description,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 18.sp,
                color = if (installed) c.contentTertiary else c.contentSecondary
            )
        }
        Spacer(modifier = Modifier.width(sp.md))
        when {
            installed -> Text(
                text = "已安装",
                style = MaterialTheme.typography.labelMedium,
                color = c.contentTertiary
            )
            installing -> Text(
                text = when (installStage) {
                    com.enlpot.notix.plugin.WordTokenizerManager.InstallStage.DOWNLOADING -> "安装中 $installProgress%"
                    com.enlpot.notix.plugin.WordTokenizerManager.InstallStage.EXTRACTING -> "解压中"
                    com.enlpot.notix.plugin.WordTokenizerManager.InstallStage.LOADING -> "加载中"
                    else -> "安装中"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            else -> NotixDialogButton(
                onClick = onInstall,
                text = "安装",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    // v8.17：每行独立 Card
    val sp = MaterialTheme.notixSpacing
    Column(modifier = Modifier.fillMaxWidth()) {
        // v8.16：分栏标题加水平边距，与下方卡片左边缘对齐
        SectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = sp.lg)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sp.lg),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
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
    // Stage 8：保留 Notix「图标圆形圈」视觉元素（OD-8.1，详见 STAGE8_PROGRESS.md），
    // 仅 Token 化颜色 / 间距 / 字体（无等价令牌的保留原 typography）；
    // 触控目标 ≥44dp 由垂直 14.dp + 40.dp 圆形 + 文字高度共同保证。
    val c = MaterialTheme.notix
    val sp = MaterialTheme.notixSpacing
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = NotixCorner.Card,
        colors = CardDefaults.cardColors(containerColor = c.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = sp.lg, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(c.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(sp.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = c.contentPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 18.sp,
                    color = c.contentSecondary
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(sp.md))
            trailing()
        }
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.notix.outlineVariant
    )
}

@Composable
private fun NavChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.notix.contentSecondary
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
    val c = MaterialTheme.notix
    val sp = MaterialTheme.notixSpacing
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = NotixCorner.Card,
        colors = CardDefaults.cardColors(containerColor = c.surfaceVariant)
    ) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = sp.lg, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = c.contentPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = c.contentSecondary,
                modifier = Modifier.rotate(if (expanded) 180f else 0f)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(
                    start = sp.lg,
                    end = sp.lg,
                    bottom = 14.dp
                )
            ) {
                content()
            }
        }
    }
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
    val c = MaterialTheme.notix
    val sp = MaterialTheme.notixSpacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NotixCorner.ListItem)
            .background(c.surface.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = sp.lg, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = c.contentSecondary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = c.contentPrimary
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
        contentScrollable = false,
        titleTrailing = {
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
        },
        content = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
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
    // Stage 8：Token 化颜色 / 字体 / 间距 / 圆角，保留 Notix「图标圆圈」+ 权限标识等宽字体视觉
    val c = MaterialTheme.notix
    val sp = MaterialTheme.notixSpacing
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onFix),
        shape = NotixCorner.Card
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (granted) c.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (granted) c.primary
                                else c.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = c.contentPrimary
                    )
                    Spacer(Modifier.height(sp.xs))
                    Text(
                        text = if (granted) stringResource(R.string.settings_permission_status_normal)
                                else stringResource(R.string.settings_permission_status_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (granted) c.primary
                                else c.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(sp.sm))
                // 整卡可点击跳转对应系统设置，用箭头提示可交互
                NavChevron()
            }
            Spacer(Modifier.height(sp.sm))
            // 权限标识（Android 常量，跨语言一致）
            Text(
                text = permName,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = c.contentSecondary.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                color = c.contentSecondary
            )
            Spacer(Modifier.height(6.dp))
            // 使用组件：标注该权限被 App 中哪个组件用到
            Text(
                text = stringResource(R.string.settings_permission_usedby_label) + "：" + usedBy,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                color = c.contentSecondary
            )
            if (!granted) {
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(
                        onClick = onFix,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = c.error
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













