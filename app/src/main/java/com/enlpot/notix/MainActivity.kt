package com.enlpot.notix

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.enlpot.notix.CrashLogManager
import com.enlpot.notix.ui.components.CrashLogDialog
import com.enlpot.notix.ui.components.HistoryNotificationDetailsDialog
import com.enlpot.notix.ui.components.NotixConfirmDialog
import com.enlpot.notix.ui.components.NotixDangerButton
import com.enlpot.notix.ui.components.NotixDialog
import com.enlpot.notix.ui.components.NotixDialogButton
import com.enlpot.notix.ui.theme.notix
import com.enlpot.notix.ui.theme.notixSpacing
import com.enlpot.notix.ui.theme.notixType
import com.enlpot.notix.health.HealthCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.enlpot.notix.setup.SetupState
import java.time.LocalDate
import com.enlpot.notix.ui.screens.HistoryScreen
import com.enlpot.notix.ui.screens.ChartPanel
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

    /**
     * v8.0：历史/规则刷新节流——服务每条通知都发 ACTION_HISTORY_UPDATED，密集推送时若每次都
     * 主线程全量读取+重组会卡顿。这里做 400ms 去抖：高频广播只合并为一次刷新，且读盘在 IO 线程
     * （配合 NotificationHistoryStorage 的内存缓存，历史文件再大也不阻塞主线程/不掉帧）。
     */
    private var historyRefreshScheduled = false
    private fun scheduleHistoryRefresh() {
        if (historyRefreshScheduled) return
        historyRefreshScheduled = true
        uiScope.launch {
            delay(400)
            historyRefreshScheduled = false
            val entries = withContext(Dispatchers.IO) { notificationHistoryStorage.getEntries() }
            historyEntries = entries
            pastNotifications = entries.flatMap { it.changes }
            rules = ruleStorage.getRules().filter { it.isValid }
            unmonitoredApps = unmonitoredAppsStorage.getUnmonitoredApps().toSet()
            listenerPaused = NotificationBlockerService.isListenerPaused(this@MainActivity)
        }
    }

    // v7.50：存储占用——清空全部规则并刷新状态
    private fun clearAllRulesForStorage() {
        ruleStorage.saveRules(emptyList())
        rules = emptyList()
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
        // v8.0：重建时若 savedInstanceState 显式保存过 showSetupWizard，优先尊重之（避免向导进行中旋转被强制退出）
        showSetupWizard = if (savedInstanceState != null) {
            savedInstanceState.getBoolean(STATE_SHOW_SETUP_WIZARD, false)
        } else {
            SetupState.shouldShowSetupWizard(this) ||
                intent?.getBooleanExtra(HealthCheckWorker.EXTRA_OPEN_WIZARD, false) == true
        }
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
        // v8.0：历史/规则读取移到 IO 线程，避免大历史文件在主线程 Gson 解析导致进入页面卡顿
        uiScope.launch {
            val entries = withContext(Dispatchers.IO) { notificationHistoryStorage.getEntries() }
            historyEntries = entries
            pastNotifications = entries.flatMap { it.changes }
            rules = ruleStorage.getRules().filter { it.isValid }
            unmonitoredApps = unmonitoredAppsStorage.getUnmonitoredApps().toSet()
            listenerPaused = NotificationBlockerService.isListenerPaused(this@MainActivity)
        }
    }

    @Composable
    private fun MainScreen() {
        val context = LocalContext.current
        val c = MaterialTheme.notix
        val sp = MaterialTheme.notixSpacing
        // v7.40：旋转恢复——通知详情弹窗对象（Gson 序列化）
        val historyEntrySaver = remember {
            Saver<NotificationHistoryEntry?, String>(
                save = { it?.let(paramsGson::toJson) },
                restore = { json ->
                    json?.let {
                        runCatching { paramsGson.fromJson(it, NotificationHistoryEntry::class.java) }.getOrNull()
                    }
                }
            )
        }
        var notificationToShowHistoryDetailsDialog by rememberSaveable(stateSaver = historyEntrySaver) { mutableStateOf<NotificationHistoryEntry?>(null) }
        // v7.40：旋转恢复——底部当前 tab（阶段3 起为纯点击切换，无滑动）
        var currentTab by rememberSaveable { mutableIntStateOf(0) }
        val coroutineScope = rememberCoroutineScope()
        var backToCurrentWeekTrigger by rememberSaveable { mutableIntStateOf(0) }
        var scrollToTopTrigger by rememberSaveable { mutableIntStateOf(0) }
        // v7.13：上次崩溃弹窗状态（检测到日志非空时下次启动提示）
        var showCrashReportDialog by rememberSaveable { mutableStateOf(CrashLogManager.hasCrashes(context)) }
        var showCrashLogDialog by rememberSaveable { mutableStateOf(false) }
        // v7.29：启动崩溃弹窗「清空日志」二次确认
        var showCrashClearConfirm by rememberSaveable { mutableStateOf(false) }

        DisposableEffect(Unit) {
            val historyUpdateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == NotificationBlockerService.ACTION_HISTORY_UPDATED) {
                        // v8.0：交由节流刷新（IO 线程读盘 + 400ms 去抖），避免高频通知下主线程卡顿
                        scheduleHistoryRefresh()
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
                    currentTab = 1
                },
                editingRule = editingRule,
                onUpdateRule = editingRule?.let { old ->
                    { oldRule, newRule ->
                        ruleStorage.updateRuleById(oldRule.id, newRule)?.let { rules = it }
                        NotificationBlockerService.requestApplyRule(context, newRule)
                        showRuleWizard = false; editingRule = null
                        showMessage(context.getString(R.string.toast_rule_updated))
                        currentTab = 1
                    }
                },
                onDeleteRule = editingRule?.let { _ ->
                    { deleted ->
                        rules = ruleStorage.deleteRuleById(deleted.id)
                        // v7.26：删除规则后全量重扫，让剩余启用规则重新作用于现有通知
                        NotificationBlockerService.requestRescanAll(context)
                        showRuleWizard = false; editingRule = null
                        showMessage(context.getString(R.string.toast_rule_deleted))
                        currentTab = 1
                    }
                },
                prefillNotification = prefillNotification
            )
        } else {
            TabbedScreen(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
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
                                onResumeMonitoring = { packageName, appName ->
                    unmonitoredAppsStorage.removeApp(packageName)
                    unmonitoredApps = unmonitoredAppsStorage.getUnmonitoredApps().toSet()
                    showMessage(context.getString(R.string.toast_resumed_monitoring, appName))
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
                },
                onRestoreNotification = { notification ->
                    restoreNotificationToShade(context, notification)
                }
            )
        }

        // v7.13：上次崩溃提示弹窗（查看日志 / 忽略 / 清空日志）
        if (showCrashReportDialog) {
            NotixDialog(
                onDismiss = { showCrashReportDialog = false },
                title = stringResource(R.string.crash_detected_title),
                content = {
                    Text(
                        text = stringResource(R.string.crash_detected_message),
                        style = MaterialTheme.notixType.bodySecondary,
                        color = c.contentSecondary
                    )
                    Spacer(Modifier.height(sp.md))
                    // v7.29：清空日志按钮（二次确认）
                    NotixDangerButton(
                        onClick = { showCrashClearConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.crash_log_clear)
                    )
                    Spacer(Modifier.height(sp.lg))
                },
                buttons = {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // 忽略：次要、半宽
                        NotixDialogButton(
                            onClick = { showCrashReportDialog = false },
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.crash_ignore),
                            containerColor = c.surfaceVariant,
                            contentColor = c.contentPrimary
                        )
                        Spacer(Modifier.width(sp.sm))
                        // 查看日志：主操作、半宽
                        NotixDialogButton(
                            onClick = {
                                showCrashReportDialog = false
                                showCrashLogDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.crash_view_log),
                            containerColor = c.primary,
                            contentColor = c.onPrimary
                        )
                    }
                }
            )
        }
        // v7.29：启动崩溃弹窗「清空日志」二次确认
        if (showCrashClearConfirm) {
            NotixConfirmDialog(
                onDismiss = { showCrashClearConfirm = false },
                onConfirm = {
                    CrashLogManager.clearLogs(context)
                    showCrashClearConfirm = false
                    showCrashReportDialog = false
                },
                title = stringResource(R.string.crash_log_clear_title),
                body = stringResource(R.string.crash_log_clear_confirm)
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
        // v7.40：底部三 tab 取消滑动，仅点击切换（currentTab 由 rememberSaveable 持久化）
        currentTab: Int,
        onTabSelected: (Int) -> Unit,
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
        onResumeMonitoring: (String, String) -> Unit,
        onDeleteRule: (BlockerRule) -> Unit,
        onToggleRule: (BlockerRule, Boolean) -> Unit,
        onResetHitCount: (BlockerRule) -> Unit,
        onRescanRule: () -> Unit,
        onHistoryTabClick: () -> Unit
    ) {
        val context = LocalContext.current // Get context inside Composable
        val c = MaterialTheme.notix
        val sp = MaterialTheme.notixSpacing
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

        // v7.41：横屏通用布局——selectedDay 状态提升至此（ChartPanel 与 HistoryScreen 共用，旋转保持）
        var selectedDay by rememberSaveable { mutableStateOf<LocalDate?>(null) }
        // v7.41：横屏左栏图表铃铛的暂停/恢复二次确认（竖屏仍由 HistoryScreen 内部负责）
        var showListenerPauseConfirm by rememberSaveable { mutableStateOf(false) }
        // v7.41：返回本周触发时全局清除选中日（左栏 ChartPanel 同步生效）
        LaunchedEffect(backToCurrentWeekTrigger) {
            if (backToCurrentWeekTrigger > 0) {
                selectedDay = null
            }
        }
        // v7.42：底部三 tab 导航（竖屏 bottomBar / 横屏右半复用）
        // 高度降至 56dp、仅图标，长按显示文字气泡（横竖屏一致）
        val bottomNav: @Composable () -> Unit = {
            val density = LocalDensity.current
            var longPressedTab by remember { mutableStateOf<Int?>(null) }
            // 长按气泡 1.5s 后自动消失
            LaunchedEffect(longPressedTab) {
                if (longPressedTab != null) {
                    delay(1500)
                    longPressedTab = null
                }
            }
            NavigationBar(
                modifier = Modifier.height(56.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .combinedClickable(
                                onClick = {
                                    // v7.5：已在历史页时再次点击底部"历史"tab 则回到顶部
                                    if (index == 0 && currentTab == 0) {
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
                                        onTabSelected(index)
                                    }
                                },
                                onLongClick = { longPressedTab = index },
                                onClickLabel = title
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // v7.43：选中态主题色圆角胶囊背景（替代顶部指示条，参考图样式）
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (currentTab == index) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    }
                                )
                        )
                        Icon(
                            imageVector = tabIcons[index],
                            contentDescription = title,
                            tint = if (currentTab == index) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        // 长按显示文字气泡（图标上方）
                        if (longPressedTab == index) {
                            Popup(
                                alignment = Alignment.TopCenter,
                                offset = IntOffset(0, with(density) { (-46).dp.roundToPx() })
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.inverseSurface,
                                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // v7.41：当前 tab 页面内容（横屏右半 / 竖屏内容区共用）
        val screenContent: @Composable () -> Unit = {
            // v8.0：tab 切换过渡动画（淡入淡出 + 轻微水平位移，约 250ms）
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    (fadeIn(tween(250)) + slideInHorizontally { it / 8 }) togetherWith
                        (fadeOut(tween(250)) + slideOutHorizontally { -it / 8 })
                },
                label = "tabContent",
            ) { tab ->
                when (tab) {
                0 -> PagerScreenContent(
                    page = 0,
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
                    onSettingsClose = { onTabSelected(0) },
                    onBackToCurrentWeek = onBackToCurrentWeek,
                    selectedDay = selectedDay,
                    onSelectedDayChange = { selectedDay = it }
                )
                1 -> PagerScreenContent(
                    page = 1,
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
                    onSettingsClose = { onTabSelected(0) },
                    onBackToCurrentWeek = onBackToCurrentWeek,
                    selectedDay = selectedDay,
                    onSelectedDayChange = { selectedDay = it }
                )
                else -> PagerScreenContent(
                    page = 2,
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
                    onSettingsClose = { onTabSelected(0) },
                    onBackToCurrentWeek = onBackToCurrentWeek,
                    selectedDay = selectedDay,
                    onSelectedDayChange = { selectedDay = it }
                )
                }
            }
        }
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            // v7.41：横屏——左半图表(50%) + 竖线 + 右半内容(50%)，底部三 tab 仅占右半（叠加）
            // v7.42：显式使用主题背景，避免露出 values-night windowBackground 旧深灰
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // 左半：通用图表面板（三 tab 共用同一图表，延伸至屏幕底）
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ChartPanel(
                            entries = historyEntries,
                            selectedDay = selectedDay,
                            onDayClick = { day ->
                                selectedDay = if (selectedDay == day) null else day
                            },
                            onClearDay = { selectedDay = null },
                            listenerPaused = listenerPaused,
                            onToggleListenerPaused = { showListenerPauseConfirm = true },
                            onBackToCurrentWeek = { onBackToCurrentWeek() },
                            backToCurrentWeekTrigger = backToCurrentWeekTrigger
                        )
                    }
                    // 中间竖线分割
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    // 右半：当前 tab 页面内容（底部预留 NavigationBar 高度，避免被遮挡）
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(bottom = 56.dp)
                    ) {
                        screenContent()
                    }
                }
                // 底部三 tab：横屏仅占右半（叠加在右半底部，左半图表延伸到底）
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxWidth(0.5f)
                ) {
                    bottomNav()
                }
            }
            // v7.41：横屏左栏图表铃铛的暂停/恢复二次确认
            if (showListenerPauseConfirm) {
                NotixConfirmDialog(
                    onDismiss = { showListenerPauseConfirm = false },
                    onConfirm = {
                        showListenerPauseConfirm = false
                        onToggleListenerPaused(!listenerPaused)
                    },
                    title = stringResource(if (listenerPaused) R.string.listener_resume_confirm_title else R.string.listener_pause_confirm_title),
                    body = stringResource(if (listenerPaused) R.string.listener_resume_confirm_message else R.string.listener_pause_confirm_message),
                    confirmText = stringResource(if (listenerPaused) R.string.listener_resume else R.string.listener_pause),
                    danger = false
                )
            }
        } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { bottomNav() }
        ) { innerPadding ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.padding(innerPadding)) {
                    // v7.40：底部三 tab 取消滑动，改为按 currentTab 直接渲染当前页
                    // v8.0：tab 切换过渡动画（淡入淡出 + 轻微水平位移，约 250ms）
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            (fadeIn(tween(250)) + slideInHorizontally { it / 8 }) togetherWith
                                (fadeOut(tween(250)) + slideOutHorizontally { -it / 8 })
                        },
                        label = "tabContent",
                    ) { tab ->
                        when (tab) {
                        0 -> PagerScreenContent(
                            page = 0,
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
                            onSettingsClose = { onTabSelected(0) },
                            onBackToCurrentWeek = onBackToCurrentWeek,
                            selectedDay = selectedDay,
                            onSelectedDayChange = { selectedDay = it }
                        )
                        1 -> PagerScreenContent(
                            page = 1,
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
                            onSettingsClose = { onTabSelected(0) },
                            onBackToCurrentWeek = onBackToCurrentWeek,
                            selectedDay = selectedDay,
                            onSelectedDayChange = { selectedDay = it }
                        )
                        else -> PagerScreenContent(
                            page = 2,
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
                            onSettingsClose = { onTabSelected(0) },
                            onBackToCurrentWeek = onBackToCurrentWeek,
                            selectedDay = selectedDay,
                            onSelectedDayChange = { selectedDay = it }
                        )
                        }
                    }
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
        onResumeMonitoring: (String, String) -> Unit,
        onDeleteRule: (BlockerRule) -> Unit,
        onToggleRule: (BlockerRule, Boolean) -> Unit,
        onResetHitCount: (BlockerRule) -> Unit,
        onRescanRule: () -> Unit,
        onClearHistoryByDate: (Long, Long) -> Unit,
        onClearHistoryByPackages: (Set<String>) -> Unit,
        onSettingsClose: () -> Unit,
        onBackToCurrentWeek: () -> Unit,
        // v7.41：横屏通用图表面板——选中日状态透传 HistoryScreen
        selectedDay: LocalDate? = null,
        onSelectedDayChange: (LocalDate?) -> Unit = {}
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
                rules = rules,
                selectedDay = selectedDay,
                onSelectedDayChange = onSelectedDayChange
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
                // v7.50：存储占用——清空全部规则
                onClearRules = { clearAllRulesForStorage() },
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
        outState.putBoolean(STATE_SHOW_SETUP_WIZARD, showSetupWizard)
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
        private const val STATE_SHOW_SETUP_WIZARD = "state_show_setup_wizard"
        private const val STATE_EDITING_RULE = "state_editing_rule"
        private const val STATE_PREFILL_NOTIFICATION = "state_prefill_notification"
    }
}

fun Color.luminance(): Float {
    return (this.red * 0.2126f + this.green * 0.7152f + this.blue * 0.0722f)
}