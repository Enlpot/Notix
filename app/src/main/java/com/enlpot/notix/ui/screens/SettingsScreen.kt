package com.enlpot.notix.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.res.stringResource
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
import com.enlpot.notix.SimpleNotification
import com.enlpot.notix.ui.components.CrashLogDialog
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

    // v7.18：关于分组展开折叠状态
    var aboutFeaturesExpanded by remember { mutableStateOf(false) }
    var aboutPrivacyExpanded by remember { mutableStateOf(false) }

    // Permission status
    val notificationAccessGranted = remember {
        val listeners = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        )
        listeners?.contains(context.packageName) == true
    }
    val batteryOptimizationGranted = remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName)
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
                        exportImportMessage = if (result.droppedCount > 0) {
                            context.getString(
                                R.string.imported_rules_some_skipped,
                                newRules.size,
                                result.droppedCount
                            )
                        } else {
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
        AlertDialog(
            onDismissRequest = { showExportImportDialog = false },
            title = { Text(stringResource(R.string.export_import_rules)) },
            text = { Text(stringResource(R.string.choose_action)) },
            confirmButton = {
                TextButton(onClick = {
                    showExportImportDialog = false
                    exportLauncher.launch("Notix_rules.json")
                }) {
                    Text(stringResource(R.string.export))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportImportDialog = false
                    importLauncher.launch(arrayOf("application/json"))
                }) {
                    Text(stringResource(R.string.import_rules))
                }
            }
        )
    }

    if (exportImportMessage != null) {
        AlertDialog(
            onDismissRequest = { exportImportMessage = null },
            title = { Text(stringResource(R.string.status)) },
            text = { Text(exportImportMessage!!) },
            confirmButton = {
                TextButton(onClick = { exportImportMessage = null }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // Clear history — mode picker
    if (showClearModeDialog) {
        // v7.24：清除历史弹窗——深色 Material 卡片风格（替代默认 AlertDialog）
        Dialog(
            onDismissRequest = { showClearModeDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.clear_history),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.clear_history_mode_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            showClearModeDialog = false
                            showClearAllConfirmDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.clear_all_history),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    TextButton(
                        onClick = {
                            showClearModeDialog = false
                            showClearByDateDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.clear_by_date_range)) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    TextButton(
                        onClick = {
                            showClearModeDialog = false
                            selectedPackages = emptySet()
                            showClearByAppDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.clear_by_app)) }
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { showClearModeDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }

    // Clear history — clear all (two-step confirmation)
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = { Text(stringResource(R.string.clear_all_history_confirm_title)) },
            text = { Text(stringResource(R.string.clear_all_history_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearAllConfirmDialog = false
                    onClearHistory()
                }) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Clear history — by date range
    if (showClearByDateDialog) {
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
        Dialog(
            onDismissRequest = { showClearByDateDialog = false; dateError = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.clear_by_date_range_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showClearByDateDialog = false; dateError = null }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.clear_by_date_range_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    dateError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
                        TextButton(
                            onClick = {
                                cal.timeInMillis = System.currentTimeMillis()
                                endDateMillis = cal.timeInMillis
                                cal.add(Calendar.DAY_OF_MONTH, -6)
                                startDateMillis = cal.timeInMillis
                                dateError = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.clear_date_7d), maxLines = 1)
                        }
                        TextButton(
                            onClick = {
                                cal.timeInMillis = System.currentTimeMillis()
                                endDateMillis = cal.timeInMillis
                                cal.add(Calendar.DAY_OF_MONTH, -29)
                                startDateMillis = cal.timeInMillis
                                dateError = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.clear_date_30d), maxLines = 1)
                        }
                        TextButton(
                            onClick = {
                                cal.timeInMillis = System.currentTimeMillis()
                                endDateMillis = cal.timeInMillis
                                cal.add(Calendar.DAY_OF_MONTH, -89)
                                startDateMillis = cal.timeInMillis
                                dateError = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.clear_date_90d), maxLines = 1)
                        }
                        TextButton(
                            onClick = {
                                startDateMillis = 0L
                                endDateMillis = System.currentTimeMillis()
                                dateError = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.clear_date_all), maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val startMs = startDateMillis
                            cal.timeInMillis = endDateMillis
                            cal.add(Calendar.DAY_OF_MONTH, 1)
                            cal.add(Calendar.MILLISECOND, -1)
                            val endMs = cal.timeInMillis
                            if (endMs < startMs) {
                                dateError = context.getString(R.string.invalid_date_range)
                                return@Button
                            }
                            onClearHistoryByDate(startMs, endMs)
                            showClearByDateDialog = false
                            dateError = null
                            showMessage(context.getString(R.string.toast_history_cleared))
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            stringResource(R.string.clear_this_range_history),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showClearByDateDialog = false; dateError = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.cancel)) }
                }
            }
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
        Dialog(
            onDismissRequest = { showClearByAppDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.clear_by_app_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showClearByAppDialog = false }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
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
                            TextButton(onClick = {
                                val filteredPkgs = filteredApps.map { it.first }.toSet()
                                selectedPackages = if (filteredPkgs.all { it in selectedPackages }) {
                                    selectedPackages - filteredPkgs
                                } else {
                                    selectedPackages + filteredPkgs
                                }
                            }) {
                                Text(stringResource(R.string.select_all))
                            }
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
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (selectedPackages.isNotEmpty()) {
                                onClearHistoryByPackages(selectedPackages)
                                showClearByAppDialog = false
                                showMessage(context.getString(R.string.toast_history_cleared))
                            }
                        },
                        enabled = selectedPackages.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            stringResource(R.string.clear_selected_apps_history),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // v7.14：修复崩溃日志入口点击无反应——showCrashLogDialog 已声明但从未挂载 CrashLogDialog 渲染
    if (showCrashLogDialog) {
        CrashLogDialog(
            onDismiss = { showCrashLogDialog = false },
            onEnabledChanged = { crashLogEnabled = it }
        )
    }

    // v7.25：移除顶部 TopAppBar（底部 tab 已有"设置"入口），内容从状态栏下直接开始
    Scaffold(
        // v7.24：应用内 Snackbar 提示（替代系统 Toast）
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (showStorageUsageScreen) {
            // v7.50：存储占用二级界面
            StorageUsageScreen(
                onBack = {
                    showStorageUsageScreen = false
                    storageRefreshTick++
                },
                onClearHistory = onClearHistory,
                onClearRules = onClearRules,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .navigationBarsPadding()
            )
        } else {
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

            // Permission guidance cards
            SettingsSection(title = stringResource(R.string.settings_permission_notification_title)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (notificationAccessGranted)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (notificationAccessGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (notificationAccessGranted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_permission_notification_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (notificationAccessGranted)
                                    stringResource(R.string.settings_permission_notification_granted)
                                else
                                    stringResource(R.string.settings_permission_notification_not_granted),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (notificationAccessGranted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_permission_notification_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!notificationAccessGranted) {
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        }) {
                            Text(stringResource(R.string.settings_permission_go_to_settings))
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.settings_permission_battery_title)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (batteryOptimizationGranted)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (batteryOptimizationGranted) Icons.Filled.CheckCircle else Icons.Filled.BatteryAlert,
                                contentDescription = null,
                                tint = if (batteryOptimizationGranted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_permission_battery_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (batteryOptimizationGranted)
                                    stringResource(R.string.settings_permission_battery_granted)
                                else
                                    stringResource(R.string.settings_permission_battery_not_granted),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (batteryOptimizationGranted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_permission_battery_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!batteryOptimizationGranted) {
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }) {
                            Text(stringResource(R.string.settings_permission_battery_one_click))
                        }
                    }
                }
            }

            // v7.23：后台保活引导分组
            SettingsSection(title = stringResource(R.string.settings_keepalive_section_title)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (batteryOptimizationGranted)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (batteryOptimizationGranted) Icons.Filled.CheckCircle else Icons.Filled.BatteryAlert,
                                contentDescription = null,
                                tint = if (batteryOptimizationGranted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_permission_battery_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (batteryOptimizationGranted)
                                    stringResource(R.string.settings_keepalive_battery_granted)
                                else
                                    stringResource(R.string.settings_permission_battery_not_granted),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (batteryOptimizationGranted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_keepalive_battery_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!batteryOptimizationGranted) {
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {
                            // 优先电池优化白名单，action 不可用时回退应用详情页
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                    )
                                }
                            }
                        }) {
                            Text(stringResource(R.string.settings_keepalive_battery_button))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.settings_keepalive_autostart_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_keepalive_autostart_desc),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
