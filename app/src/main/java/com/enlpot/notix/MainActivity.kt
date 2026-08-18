package com.enlpot.notix

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface // Import Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.enlpot.notix.CrashLogManager
import com.enlpot.notix.ui.components.CrashLogDialog
import com.enlpot.notix.ui.components.HistoryNotificationDetailsDialog
import com.enlpot.notix.health.HealthCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.enlpot.notix.setup.SetupState
import com.enlpot.notix.ui.screens.HistoryScreen
import com.enlpot.notix.ui.screens.RulesScreen
import com.enlpot.notix.ui.screens.RuleWizardScreen
import com.enlpot.notix.ui.screens.SettingsScreen
import com.enlpot.notix.ui.screens.SetupWizardScreen
import com.enlpot.notix.ui.theme.NotixTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var ruleStorage: RuleStorage
    private lateinit var notificationHistoryStorage: NotificationHistoryStorage
    private lateinit var blockedNotificationHistoryStorage: BlockedNotificationHistoryStorage
    private lateinit var unmonitoredAppsStorage: UnmonitoredAppsStorage
    private lateinit var appInfoStorage: AppInfoStorage
    private var isServiceEnabled by mutableStateOf(false)
    private var historyEntries by mutableStateOf<List<NotificationHistoryEntry>>(emptyList())
    private var pastNotifications by mutableStateOf<List<SimpleNotification>>(emptyList())
    private var rules by mutableStateOf<List<BlockerRule>>(emptyList())
    private var unmonitoredApps by mutableStateOf<Set<String>>(emptySet())
    private var listenerPaused by mutableStateOf(false)
    private var showRuleWizard by mutableStateOf(false)
    private var editingRule by mutableStateOf<BlockerRule?>(null)
    // v7.39：从成员级提升，随 savedInstanceState 保存恢复（旋转后保留通知预填）
    private var prefillNotification by mutableStateOf<SimpleNotification?>(null)
    private var showSetupWizard by mutableStateOf(false)
    private var wizardShowsWelcome by mutableStateOf(false)

    // v7.24：日志/提示改为应用内展示（Snackbar），不再使用系统 Toast
    private val snackbarHostState = SnackbarHostState()
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private fun showMessage(msg: String) {
        uiScope.launch { snackbarHostState.showSnackbar(msg) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // v7.39：旋转/进程重建后恢复规则向导状态（showRuleWizard/editingRule/prefillNotification），
        // 避免横竖屏切换时创建规则界面被重置退出到主界面
        if (savedInstanceState != null) {
            showRuleWizard = savedInstanceState.getBoolean(STATE_SHOW_RULE_WIZARD, false)
            savedInstanceState.getString(STATE_EDITING_RULE)?.let { json ->
                runCatching { paramsGson.fromJson(json, BlockerRule::class.java) }
                    .getOrNull()?.let { editingRule = it }
            }
            @Suppress("DEPRECATION")
            val restoredNotification: SimpleNotification? = if (Build.VERSION.SDK_INT >= 33) {
                savedInstanceState.getParcelable(STATE_PREFILL_NOTIFICATION, SimpleNotification::class.java)
            } else {
                savedInstanceState.getParcelable(STATE_PREFILL_NOTIFICATION)
            }
            if (restoredNotification != null) prefillNotification = restoredNotification
        }
        ruleStorage = RuleStorage(this)
        notificationHistoryStorage = NotificationHistoryStorage(this)
        blockedNotificationHistoryStorage = BlockedNotificationHistoryStorage(this)
        unmonitoredAppsStorage = UnmonitoredAppsStorage(this)
        appInfoStorage = AppInfoStorage(this)

        // v7.12 数据迁移：老版本分流存储的被过滤历史并入统一历史（blocked 标记）
        val legacyBlocked = blockedNotificationHistoryStorage.getHistory()
        if (legacyBlocked.isNotEmpty()) {
            notificationHistoryStorage.mergeBlockedNotifications(legacyBlocked)
            blockedNotificationHistoryStorage.clearHistory()
        }

        isServiceEnabled = isNotificationServiceEnabled()
        wizardShowsWelcome = !isServiceEnabled && SetupState.lastSeenSetupVersion(this) == 0
        showSetupWizard = SetupState.shouldShowSetupWizard(this) ||
            intent?.getBooleanExtra(HealthCheckWorker.EXTRA_OPEN_WIZARD, false) == true
        setContent {
            NotixTheme {
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = MaterialTheme.colorScheme.background.luminance() > 0.5f
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                }
                MainScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isServiceEnabled = isNotificationServiceEnabled()
        if (!isServiceEnabled) {
            showSetupWizard = true
        }
        historyEntries = notificationHistoryStorage.getEntries()
        pastNotifications = historyEntries.flatMap { it.changes }
        // v7.13：加载后同样过滤，防御旧数据残留
        rules = ruleStorage.getRules().filter { it.isValid }
        unmonitoredApps = unmonitoredAppsStorage.getUnmonitoredApps().toSet()
        listenerPaused = NotificationBlockerService.isListenerPaused(this)
    }

    @Composable
    private fun MainScreen() {
        val context = LocalContext.current
        var notificationToShowHistoryDetailsDialog by remember { mutableStateOf<NotificationHistoryEntry?>(null) }
        val pagerState = rememberPagerState(pageCount = { 3 })
        val coroutineScope = rememberCoroutineScope()
        var backToCurrentWeekTrigger by remember { mutableIntStateOf(0) }
        var scrollToTopTrigger by remember { mutableIntStateOf(0) }
        // v7.13：上次崩溃弹窗状态（检测到日志非空时下次启动提示）
        var showCrashReportDialog by remember { mutableStateOf(CrashLogManager.hasCrashes(context)) }
        var showCrashLogDialog by remember { mutableStateOf(false) }
        // v7.29：启动崩溃弹窗「清空日志」二次确认
        var showCrashClearConfirm by remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            val historyUpdateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == NotificationBlockerService.ACTION_HISTORY_UPDATED) {
                        historyEntries = notificationHistoryStorage.getEntries()
                        pastNotifications = historyEntries.flatMap { it.changes }
                        listenerPaused = NotificationBlockerService.isListenerPaused(context!!)
                        // v7.24：规则命中计数变更后同步刷新 rules 列表，UI 立即显示最新命中数
                        rules = ruleStorage.getRules()
                    }
                }
            }
            ContextCompat.registerReceiver(
                context,
                historyUpdateReceiver,
                IntentFilter(NotificationBlockerService.ACTION_HISTORY_UPDATED),
                ContextCompat.RECEIVER_EXPORTED
            )
            onDispose {
                context.unregisterReceiver(historyUpdateReceiver)
            }
        }

        // v7.24：应用内提示宿主——Snackbar 覆盖所有子页面（向导/主界面）
        Box(modifier = Modifier.fillMaxSize()) {
        if (showSetupWizard) {
            SetupWizardScreen(
                showWelcome = wizardShowsWelcome,
                onFinish = {
                    showSetupWizard = false
                    isServiceEnabled = isNotificationServiceEnabled()
                },
            )
        } else if (showRuleWizard) {
            // No BackHandler here: the wizard owns back (keyboard first, then
            // step-back, then close).
            RuleWizardScreen(
                existingRules = rules,
                pastNotifications = pastNotifications,
                onClose = { showRuleWizard = false; editingRule = null; prefillNotification = null },
                onCreateRule = { rule ->
                    rules = ruleStorage.addRules(listOf(rule))
                    NotificationBlockerService.requestApplyRule(context, rule)
                    showRuleWizard = false; prefillNotification = null
                    showMessage(context.getString(R.string.toast_rule_added))
                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                },
                editingRule = editingRule,
                onUpdateRule = editingRule?.let { old ->
                    { oldRule, newRule ->
                        ruleStorage.updateRuleById(oldRule.id, newRule)?.let { rules = it }
                        NotificationBlockerService.requestApplyRule(context, newRule)
                        showRuleWizard = false; editingRule = null
                        showMessage(context.getString(R.string.toast_rule_updated))
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    }
                },
                onDeleteRule = editingRule?.let { _ ->
                    { deleted ->
                        rules = ruleStorage.deleteRuleById(deleted.id)
                        // v7.26：删除规则后全量重扫，让剩余启用规则重新作用于现有通知
                        NotificationBlockerService.requestRescanAll(context)
                        showRuleWizard = false; editingRule = null
                        showMessage(context.getString(R.string.toast_rule_deleted))
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    }
                },
                prefillNotification = prefillNotification
            )
        } else {
            TabbedScreen(
                pagerState = pagerState,
                historyEntries = historyEntries,
                pastNotifications = pastNotifications,
                rules = rules,
                unmonitoredApps = unmonitoredApps,
                listenerPaused = listenerPaused,
                backToCurrentWeekTrigger = backToCurrentWeekTrigger,
                onBackToCurrentWeek = { backToCurrentWeekTrigger++ },
                scrollToTopTrigger = scrollToTopTrigger,
                onHistoryTabClick = { scrollToTopTrigger++ },
                onRefreshHistory = {
                    historyEntries = notificationHistoryStorage.getEntries()
                    pastNotifications = historyEntries.flatMap { it.changes }
                    listenerPaused = NotificationBlockerService.isListenerPaused(context)
                },
                onEntryHistoryClick = { entry -> notificationToShowHistoryDetailsDialog = entry },
                onOpenNotification = { notification -> triggerNotificationAction(context, notification) },
                onRestoreNotification = { notification -> restoreNotificationToShade(context, notification) },
                onCreateRuleFromNotification = { notification ->
                    prefillNotification = notification
                    showRuleWizard = true
                },
                onClearHistory = {
                    notificationHistoryStorage.clearHistory()
                    appInfoStorage.clearAllAppInfo()
                    historyEntries = emptyList()
                    pastNotifications = emptyList()
                    showMessage(context.getString(R.string.toast_history_cleared))
                },
                onClearBlockedHistory = {
                    notificationHistoryStorage.clearBlockedHistory()
                    historyEntries = notificationHistoryStorage.getEntries()
                    pastNotifications = historyEntries.flatMap { it.changes }
                    showMessage(context.getString(R.string.toast_blocked_history_cleared))
                },
                onRuleClick = { rule -> editingRule = rule; showRuleWizard = true },
                onCreateRuleClick = { showRuleWizard = true },
                onDeleteHistoryNotificationClick = { notification ->
                    notificationHistoryStorage.deleteNotification(notification)
                    historyEntries = notificationHistoryStorage.getEntries()
                    pastNotifications = historyEntries.flatMap { it.changes }
                    showMessage(context.getString(R.string.toast_notification_deleted))
                },
                onToggleListenerPaused = { paused ->
                    if (paused) {
                        NotificationBlockerService.pauseListening(context)
                    } else {
                        NotificationBlockerService.resumeListening(context)
                    }
                    listenerPaused = paused
                },
                isServiceEnabled = isServiceEnabled, // Pass isServiceEnabled
                onClearHistoryByDate = { startMs, endMs ->
                    notificationHistoryStorage.clearHistoryBetween(startMs, endMs)
                    historyEntries = notificationHistoryStorage.getEntries()
                    pastNotifications = historyEntries.flatMap { it.changes }
                },
                onClearHistoryByPackages = { packages ->
                    notificationHistoryStorage.clearHistoryByPackages(packages.toSet())
                    historyEntries = notificationHistoryStorage.getEntries()
                    pastNotifications = historyEntries.flatMap { it.changes }
                },
                onToggleAllRules = { enabled ->
                    rules = ruleStorage.setAllEnabled(enabled)
                },
                onStopMonitoring = { packageName, appName ->
                    unmonitoredAppsStorage.addApp(packageName)
                    unmonitoredApps = unmonitoredAppsStorage.getUnmonitoredApps().toSet()
                    showMessage(context.getString(R.string.toast_stopped_monitoring, appName))
                },
                onResumeMonitoring = { packageName ->
                    unmonitoredAppsStorage.removeApp(packageName)
                    unmonitoredApps = unmonitoredAppsStorage.getUnmonitoredApps().toSet()
                    showMessage(context.getString(R.string.toast_resumed_monitoring, packageName))
                },
                onDeleteRule = { rule ->
                    rules = ruleStorage.deleteRuleById(rule.id)
                    // v7.26：删除规则后全量重扫，让剩余启用规则重新作用于现有通知
                    NotificationBlockerService.requestRescanAll(context)
                    showMessage(context.getString(R.string.toast_rule_deleted))
                },
                onToggleRule = { rule, enabled ->
                    ruleStorage.updateRuleById(rule.id, rule.copy(isEnabled = enabled))?.let { rules = it }
                    // v7.26：规则开关变更后全量重扫（禁用即不再处理现有通知，启用则立即生效）
                    NotificationBlockerService.requestRescanAll(context)
                },
                onResetHitCount = { rule ->
                    ruleStorage.resetHitCounts(listOf(rule.id))
                    rules = ruleStorage.getRules()
                },
                onRescanRule = { NotificationBlockerService.requestRescanAll(context) }
            )
        }

        notificationToShowHistoryDetailsDialog?.let { entry ->
            HistoryNotificationDetailsDialog(
                entry = entry,
                onDismiss = { notificationToShowHistoryDetailsDialog = null },
                onOpenNotification = { notification -> triggerNotificationAction(context, notification) },
                onCreateRule = { notification ->
                    notificationToShowHistoryDetailsDialog = null
                    prefillNotification = notification
                    showRuleWizard = true
                },
                onDeleteNotification = { notification ->
                    notificationHistoryStorage.deleteNotification(notification)
                    historyEntries = notificationHistoryStorage.getEntries()
                    pastNotifications = historyEntries.flatMap { it.changes }
                    notificationToShowHistoryDetailsDialog = null
                    showMessage(context.getString(R.string.toast_notification_deleted))
                }
            )
        }

        // v7.13：上次崩溃提示弹窗（查看日志 / 忽略 / 清空日志）
        if (showCrashReportDialog) {
            AlertDialog(
                onDismissRequest = { showCrashReportDialog = false },
                title = { Text(stringResource(R.string.crash_detected_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.crash_detected_message))
                        Spacer(Modifier.height(8.dp))
                        // v7.29：清空日志按钮（二次确认）
                        TextButton(onClick = { showCrashClearConfirm = true }) {
                            Text(
                                stringResource(R.string.crash_log_clear),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showCrashReportDialog = false
                        showCrashLogDialog = true
                    }) {
                        Text(stringResource(R.string.crash_view_log))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCrashReportDialog = false }) {
                        Text(stringResource(R.string.crash_ignore))
                    }
                }
            )
        }
        // v7.29：启动崩溃弹窗「清空日志」二次确认
        if (showCrashClearConfirm) {
            AlertDialog(
                onDismissRequest = { showCrashClearConfirm = false },
                title = { Text(stringResource(R.string.crash_log_clear_title)) },
                text = { Text(stringResource(R.string.crash_log_clear_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        CrashLogManager.clearLogs(context)
                        showCrashClearConfirm = false
                        showCrashReportDialog = false
                    }) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCrashClearConfirm = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        if (showCrashLogDialog) {
            CrashLogDialog(onDismiss = { showCrashLogDialog = false })
        }

        // v7.24：应用内 Snackbar 提示（替代系统 Toast），上移避开底部导航栏
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
        }
    }

    @Composable
    private fun TabbedScreen(
        pagerState: PagerState,
        historyEntries: List<NotificationHistoryEntry>,
        pastNotifications: List<SimpleNotification>,
        rules: List<BlockerRule>,
        unmonitoredApps: Set<String>,
        listenerPaused: Boolean,
        backToCurrentWeekTrigger: Int,
        onBackToCurrentWeek: () -> Unit,
        scrollToTopTrigger: Int,
        onRefreshHistory: () -> Unit,
        onEntryHistoryClick: (NotificationHistoryEntry) -> Unit,
        onOpenNotification: (SimpleNotification) -> Unit,
        onRestoreNotification: (SimpleNotification) -> Unit,
        onCreateRuleFromNotification: (SimpleNotification) -> Unit,
        onClearHistory: () -> Unit,
        onClearBlockedHistory: () -> Unit,
        onRuleClick: (BlockerRule) -> Unit,
        onCreateRuleClick: () -> Unit,
        onDeleteHistoryNotificationClick: (SimpleNotification) -> Unit,
        onToggleListenerPaused: (Boolean) -> Unit,
        isServiceEnabled: Boolean, // Pass isServiceEnabled
        onClearHistoryByDate: (Long, Long) -> Unit,
        onClearHistoryByPackages: (Set<String>) -> Unit,
        onToggleAllRules: (Boolean) -> Unit,
        onStopMonitoring: (String, String) -> Unit,
        onResumeMonitoring: (String) -> Unit,
        onDeleteRule: (BlockerRule) -> Unit,
        onToggleRule: (BlockerRule, Boolean) -> Unit,
        onResetHitCount: (BlockerRule) -> Unit,
        onRescanRule: () -> Unit,
        onHistoryTabClick: () -> Unit
    ) {
        val context = LocalContext.current // Get context inside Composable
        val coroutineScope = rememberCoroutineScope()
        // v7.37：底部"历史"tab 双击检测（300ms 内二次点击返回本周）
        var lastHistoryTabClickTime by remember { mutableLongStateOf(0L) }
        val tabTitles = listOf(
            stringResource(R.string.tab_history),
            stringResource(R.string.tab_rules),
            stringResource(R.string.settings)
        )
        val tabIcons = listOf(
            Icons.Default.History,
            Icons.Default.Rule,
            Icons.Default.Settings
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar {
                    tabTitles.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                // v7.5：已在历史页时再次点击底部"历史"tab 则回到顶部
                                if (index == 0 && pagerState.currentPage == 0) {
                                    val now = SystemClock.uptimeMillis()
                                    // v7.37：300ms 内快速双击返回本周（与点击"通知历史"一致）
                                    if (now - lastHistoryTabClickTime <= 300L) {
                                        lastHistoryTabClickTime = 0L
                                        onBackToCurrentWeek()
                                    } else {
                                        lastHistoryTabClickTime = now
                                        onHistoryTabClick()
                                    }
                                } else {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                }
                            },
                            icon = { Icon(tabIcons[index], contentDescription = title) },
                            label = { Text(title) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.padding(innerPadding)) {
                    HorizontalPager(state = pagerState) {
                        PagerScreenContent(
                            page = it,
                            historyEntries = historyEntries,
                            pastNotifications = pastNotifications,
                            rules = rules,
                            unmonitoredApps = unmonitoredApps,
                            listenerPaused = listenerPaused,
                            backToCurrentWeekTrigger = backToCurrentWeekTrigger,
                            scrollToTopTrigger = scrollToTopTrigger,
                            onRefreshHistory = onRefreshHistory,
                            onEntryHistoryClick = onEntryHistoryClick,
                            onOpenNotification = onOpenNotification,
                            onRestoreNotification = onRestoreNotification,
                            onCreateRuleFromNotification = onCreateRuleFromNotification,
                            onClearHistory = onClearHistory,
                            onClearBlockedHistory = onClearBlockedHistory,
                            onRuleClick = onRuleClick,
                            onCreateRuleClick = onCreateRuleClick,
                            onDeleteHistoryNotificationClick = onDeleteHistoryNotificationClick,
                            onToggleListenerPaused = onToggleListenerPaused,
                            onToggleAllRules = onToggleAllRules,
                            onStopMonitoring = onStopMonitoring,
                            onResumeMonitoring = onResumeMonitoring,
                            onDeleteRule = onDeleteRule,
                            onToggleRule = onToggleRule,
                            onResetHitCount = onResetHitCount,
                            onRescanRule = onRescanRule,
                            onClearHistoryByDate = onClearHistoryByDate,
                            onClearHistoryByPackages = onClearHistoryByPackages,
                            onSettingsClose = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                            onBackToCurrentWeek = onBackToCurrentWeek
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PagerScreenContent(
        page: Int,
        historyEntries: List<NotificationHistoryEntry>,
        pastNotifications: List<SimpleNotification>,
        rules: List<BlockerRule>,
        unmonitoredApps: Set<String>,
        listenerPaused: Boolean,
        backToCurrentWeekTrigger: Int,
        scrollToTopTrigger: Int,
        onRefreshHistory: () -> Unit,
        onEntryHistoryClick: (NotificationHistoryEntry) -> Unit,
        onOpenNotification: (SimpleNotification) -> Unit,
        onRestoreNotification: (SimpleNotification) -> Unit,
        onCreateRuleFromNotification: (SimpleNotification) -> Unit,
        onClearHistory: () -> Unit,
        onClearBlockedHistory: () -> Unit,
        onRuleClick: (BlockerRule) -> Unit,
        onCreateRuleClick: () -> Unit,
        onDeleteHistoryNotificationClick: (SimpleNotification) -> Unit,
        onToggleListenerPaused: (Boolean) -> Unit,
        onToggleAllRules: (Boolean) -> Unit,
        onStopMonitoring: (String, String) -> Unit,
        onResumeMonitoring: (String) -> Unit,
        onDeleteRule: (BlockerRule) -> Unit,
        onToggleRule: (BlockerRule, Boolean) -> Unit,
        onResetHitCount: (BlockerRule) -> Unit,
        onRescanRule: () -> Unit,
        onClearHistoryByDate: (Long, Long) -> Unit,
        onClearHistoryByPackages: (Set<String>) -> Unit,
        onSettingsClose: () -> Unit,
        onBackToCurrentWeek: () -> Unit
    ) {
        when (page) {
            0 -> HistoryScreen(
                entries = historyEntries,
                unmonitoredApps = unmonitoredApps,
                listenerPaused = listenerPaused,
                backToCurrentWeekTrigger = backToCurrentWeekTrigger,
                onBackToCurrentWeek = onBackToCurrentWeek,
                scrollToTopTrigger = scrollToTopTrigger,
                onRefresh = onRefreshHistory,
                onEntryHistoryClick = onEntryHistoryClick,
                onOpenNotification = onOpenNotification,
                onRestoreNotification = onRestoreNotification,
                onCreateRuleFromNotification = onCreateRuleFromNotification,
                onClearHistory = onClearHistory,
                onDeleteNotification = onDeleteHistoryNotificationClick,
                onToggleListenerPaused = onToggleListenerPaused,
                onStopMonitoring = onStopMonitoring,
                onResumeMonitoring = onResumeMonitoring,
                onClearBlockedHistory = onClearBlockedHistory,
                rules = rules
            )

            1 -> RulesScreen(rules, onRuleClick, onCreateRuleClick, onToggleAllRules,
                onDeleteRule = onDeleteRule,
                onToggleRule = onToggleRule,
                onResetHitCount = onResetHitCount,
                onRescanRule = onRescanRule
            )
            2 -> SettingsScreen(
                onClose = onSettingsClose,
                onClearHistory = onClearHistory,
                onClearHistoryByDate = onClearHistoryByDate,
                onClearHistoryByPackages = onClearHistoryByPackages,
                pastNotifications = pastNotifications,
            )
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(this)
        return enabledListeners.contains(packageName)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_SHOW_RULE_WIZARD, showRuleWizard)
        editingRule?.let { outState.putString(STATE_EDITING_RULE, paramsGson.toJson(it)) }
        prefillNotification?.let { outState.putParcelable(STATE_PREFILL_NOTIFICATION, it) }
        super.onSaveInstanceState(outState)
    }

    private fun triggerNotificationAction(context: Context, notification: SimpleNotification) {
        val intent = if (notification.id != null) NotificationActionRepository.getAction(notification.id) else null
        if (intent != null) {
            try {
                val options = android.app.ActivityOptions.makeBasic()
                if (Build.VERSION.SDK_INT >= 34) {
                    options.setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS)
                }
                val actionIntent = Intent()
                actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.send(context, 0, actionIntent, null, null, null, options.toBundle())
            } catch (e: Exception) {
                showMessage(context.getString(R.string.toast_failed_to_trigger))
            }
        } else {
            showMessage(context.getString(R.string.toast_action_unavailable))
        }
    }

    /** 还原通知：把历史中的已消除通知重新发到通知栏，点击可跳转。 */
    private fun restoreNotificationToShade(context: Context, notification: SimpleNotification) {
        val id = notification.id ?: return
        val action = if (notification.id != null) NotificationActionRepository.getAction(notification.id) else null
        if (action == null) {
            showMessage(context.getString(R.string.toast_action_unavailable))
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            showMessage(context.getString(R.string.toast_action_unavailable))
            return
        }
        try {
            val channelId = "restored_notifications"
            val nm = NotificationManagerCompat.from(context)
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    context.getString(R.string.channel_restored),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
            val icon = notification.packageName?.let { pkg -> appInfoStorage.getAppIcon(pkg) }?.let { Icon.createWithBitmap(it) }
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_stack)
                .setContentTitle(notification.title ?: "")
                .setContentText(notification.text ?: "")
                .setWhen(notification.timestamp)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(action)
            if (icon != null) builder.setLargeIcon(icon)
            val notifyId = notification.id?.hashCode() ?: notification.timestamp.toInt()
            nm.notify(notifyId, builder.build())
            showMessage(context.getString(R.string.toast_notification_restored))
        } catch (e: Exception) {
            Log.w("MainActivity", "restore notification failed", e)
            showMessage(context.getString(R.string.toast_failed_to_restore))
        }
    }

    companion object {
        private const val STATE_SHOW_RULE_WIZARD = "state_show_rule_wizard"
        private const val STATE_EDITING_RULE = "state_editing_rule"
        private const val STATE_PREFILL_NOTIFICATION = "state_prefill_notification"
    }
}

fun Color.luminance(): Float {
    return (this.red * 0.2126f + this.green * 0.7152f + this.blue * 0.0722f)
}