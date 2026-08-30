package com.enlpot.notix.ui.screens

import com.enlpot.notix.FoldStateStorage
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.IdentityHashMap
import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enlpot.notix.BlockerRule
import com.enlpot.notix.NotificationBlockerService
import com.enlpot.notix.NotificationColorEngine
import com.enlpot.notix.NotificationColors
import com.enlpot.notix.NotificationHistoryEntry
import com.enlpot.notix.R
import com.enlpot.notix.SimpleNotification
import com.enlpot.notix.StatsStorage
import com.enlpot.notix.ui.components.NotixConfirmDialog
import com.enlpot.notix.ui.components.CrashLogDialog
import com.enlpot.notix.ui.components.NotificationCard
import com.enlpot.notix.ui.components.NotificationCardData
import com.enlpot.notix.ui.components.NotificationCardVariant
import com.enlpot.notix.ui.components.NotificationDetailDialog
import com.enlpot.notix.ui.components.EmptyState
import com.enlpot.notix.ui.components.RealAppIcon
import com.enlpot.notix.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class HistoryTab {
    BY_TIME, BY_APP, FILTERED
}

private enum class SearchHeaderMode { NORMAL, SEARCH_EXPANDED }

@Composable
fun HistoryScreen(
    entries: List<NotificationHistoryEntry>,
    unmonitoredApps: Set<String>,
    backToCurrentWeekTrigger: Int,
    onBackToCurrentWeek: () -> Unit = {},
    scrollToTopTrigger: Int = 0,
    onRefresh: () -> Unit = {},
    onEntryHistoryClick: (NotificationHistoryEntry) -> Unit,
    onOpenNotification: (SimpleNotification) -> Unit,
    onRestoreNotification: (SimpleNotification) -> Unit,
    onCreateRuleFromNotification: (SimpleNotification) -> Unit,
    onDeleteNotification: (SimpleNotification) -> Unit,
    onClearHistory: () -> Unit,
    onStopMonitoring: (String, String) -> Unit,
    onResumeMonitoring: (String, String) -> Unit,
    onToggleListenerPaused: (Boolean) -> Unit,
    listenerPaused: Boolean,
    onClearBlockedHistory: () -> Unit = {},
    // v7.36：规则列表（Filtered tab 按规则分组依据；规则被删除后条目归「未知规则」组）
    rules: List<BlockerRule> = emptyList(),
    // v7.41：横屏通用图表面板——selectedDay 状态提升至 TabbedScreen 层，竖屏/横屏共用
    selectedDay: LocalDate? = null,
    onSelectedDayChange: (LocalDate?) -> Unit = {},
    // v8.22：全量搜索回调——接入 Repository 层搜索，覆盖所有历史数据
    onSearch: (suspend (String) -> List<SimpleNotification>)? = null,
    // v8.22：分页加载
    onLoadMore: () -> Unit = {},
    hasMore: Boolean = false,
    loadingMore: Boolean = false
) {
    NotificationColorEngine.isDarkTheme = isSystemInDarkTheme()
    // v7.40：旋转恢复——三 tab 及弹窗/搜索/展开等 UI 状态
    var selectedTab by rememberSaveable { mutableStateOf(HistoryTab.BY_TIME) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showStopMonitoringDialog by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    // v7.8：通知监听铃铛二次确认对话框
    var showListenerPauseConfirm by rememberSaveable { mutableStateOf(false) }
    // v7.13：长按搜索按钮打开崩溃日志弹窗
    var showCrashLogDialog by rememberSaveable { mutableStateOf(false) }
    // v7.24：权限掉线弹窗中"打开系统设置"失败的应用内提示（不再使用系统 Toast）
    var openSettingsFailed by rememberSaveable { mutableStateOf(false) }
    val expandedApps = rememberSaveable { mutableStateOf(setOf<String>()) }
    // v7.36：Filtered tab 按规则分组的展开状态（默认收起，与按应用一致）
    val expandedRuleIds = rememberSaveable { mutableStateOf(setOf<String>()) }
    // v7.51：通知折叠——折叠段的展开状态（默认收起，按"pkg_firstId"段级唯一标识保存，旋转不丢失）。
    // 此前按 packageName 保存会导致同包名多个折叠段被一个开关联动展开，现改为段级隔离。
    val expandedFoldPackages = rememberSaveable { mutableStateOf(setOf<String>()) }
    // v8.29：折叠展开状态持久化——app 重启后恢复用户之前展开的段，上限 20 个
    val foldContext = LocalContext.current
    val foldStateStorage = remember { FoldStateStorage(foldContext) }
    LaunchedEffect(Unit) {
        val saved = foldStateStorage.getExpandedKeys()
        if (saved.isNotEmpty()) {
            expandedFoldPackages.value = saved.toSet()
        }
    }

    // --- v7.5：列表滚动状态 / 吸顶搜索区 / 下拉刷新 / 回顶 / 权限掉线提示 ---
    // v7.37：三 tab 页各自独立滚动状态（滑动切换后保留各自位置）
    // v7.40：旋转恢复——每 tab 滚动位置由 LazyListState.Saver 持久化
    val tabListStates = List(HistoryTab.entries.size) {
        rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    }
    // v7.37：三 tab 滑动切换 Pager（页序与 HistoryTab 枚举一致）
    val tabPagerState = rememberPagerState(initialPage = selectedTab.ordinal, pageCount = { HistoryTab.entries.size })
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showPermissionLostDialog by rememberSaveable { mutableStateOf(false) }

    // v7.49：搜索框展开后自动弹出键盘；关闭时隐藏键盘
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            // AnimatedContent 过渡（300ms）完成后请求焦点，避免焦点被过渡动画抢占
            delay(320)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    // 单击底部"历史"tab 回顶（作用于当前显示的 tab 页）
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            tabListStates[tabPagerState.settledPage].animateScrollToItem(0)
        }
    }

    val context = LocalContext.current

    // v7.5：进入历史页检测一次通知监听权限是否掉线
    LaunchedEffect(Unit) {
        if (!isNotificationListenerEnabled(context)) {
            showPermissionLostDialog = true
        }
    }

    // --- 柱状图状态：以周为单位分页（前26周 + 本周 + 后26周），一屏一周 ---
    // v7.41：图表数据计算与 selectedDay 已上移 ChartPanel（通用图表面板），此处仅保留页码常量
    val weeksBefore = 26
    val weeksAfter = 26
    val totalPages = weeksBefore + 1 + weeksAfter
    val currentWeekPage = weeksBefore
    // v7.37：三 tab 页各自持有图表 PagerState（一个 PagerState 不能同时绑定多个 pager）
    val chartPagerStates = rememberChartPagerStates(HistoryTab.entries.size, currentWeekPage) { totalPages }
    val coroutineScope = rememberCoroutineScope()

    // 点击顶部"通知历史"返回本周（作用于当前显示的 tab 页）
    LaunchedEffect(backToCurrentWeekTrigger) {
        if (backToCurrentWeekTrigger > 0) {
            onSelectedDayChange(null)
            coroutineScope.launch { chartPagerStates[tabPagerState.settledPage].animateScrollToPage(currentWeekPage) }
        }
    }

    // v7.37：滑动切页后同步 selectedTab（tab 选择器高亮跟随）
    LaunchedEffect(tabPagerState.settledPage) {
        selectedTab = HistoryTab.entries[tabPagerState.settledPage]
    }

    // --- Stop monitoring dialog ---
    showStopMonitoringDialog?.let { (packageName, appName) ->
        NotixConfirmDialog(
            onDismiss = { showStopMonitoringDialog = null },
            onConfirm = {
                onStopMonitoring(packageName, appName)
                showStopMonitoringDialog = null
            },
            title = stringResource(R.string.stop_monitoring_title),
            body = stringResource(R.string.stop_monitoring_confirm, appName),
            confirmText = stringResource(R.string.stop),
            danger = true
        )
    }

    // --- v7.8：通知监听暂停/恢复二次确认 ---
    if (showListenerPauseConfirm) {
        val pauseTitle = stringResource(
            if (listenerPaused) R.string.listener_resume_confirm_title
            else R.string.listener_pause_confirm_title
        )
        val pauseMessage = stringResource(
            if (listenerPaused) R.string.listener_resume_confirm_message
            else R.string.listener_pause_confirm_message
        )
        val pauseConfirm = stringResource(
            if (listenerPaused) R.string.listener_resume else R.string.listener_pause
        )
        NotixConfirmDialog(
            onDismiss = { showListenerPauseConfirm = false },
            onConfirm = {
                showListenerPauseConfirm = false
                onToggleListenerPaused(!listenerPaused)
            },
            title = pauseTitle,
            body = pauseMessage,
            confirmText = pauseConfirm,
            danger = true
        )
    }

    // --- v7.5：通知监听权限掉线引导重新授权 ---
    if (showPermissionLostDialog) {
        val permissionBody = if (openSettingsFailed) {
            stringResource(R.string.listener_permission_lost_message) + "\n\n" + stringResource(R.string.open_link_failed)
        } else {
            stringResource(R.string.listener_permission_lost_message)
        }
        NotixConfirmDialog(
            onDismiss = { showPermissionLostDialog = false },
            onConfirm = {
                openSettingsFailed = false
                try {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    showPermissionLostDialog = false
                } catch (_: Exception) {
                    openSettingsFailed = true
                }
            },
            title = stringResource(R.string.listener_permission_lost_title),
            body = permissionBody,
            confirmText = stringResource(R.string.grant_now),
            danger = false
        )
    }

    // --- v7.13：长按搜索按钮打开崩溃日志弹窗（查看/开关/打开位置） ---
    if (showCrashLogDialog) {
        CrashLogDialog(onDismiss = { showCrashLogDialog = false })
    }

    // v7.47：三 tab 共用同一套数据缓存——删除 activeEntries（原依赖 selectedTab，切 tab 会导致 remember 缓存 key 失效、全量重算卡顿约 1s）
    // 日期过滤统一基于 entries；FILTERED 的 blocked 过滤在渲染分组时由 filteredBlocked 派生，不再影响本缓存
    // v7.15：日期详情按组内任一 change 时间戳归属日过滤（避免跨天聚合组导致某日详情"未找到结果"）
    // v8.22：全量搜索结果状态——接入 Repository 层搜索，覆盖所有历史数据
    val searchResults by produceState(initialValue = emptyList<NotificationHistoryEntry>(), searchQuery, onSearch) {
        if (onSearch != null && searchQuery.isNotBlank()) {
            try {
                val results = onSearch(searchQuery)
                value = results.map { notification ->
                    NotificationHistoryEntry(
                        id = notification.id ?: java.util.UUID.randomUUID().toString(),
                        packageName = notification.packageName,
                        appLabel = notification.appLabel,
                        title = notification.title,
                        count = 1,
                        firstTimestamp = notification.timestamp,
                        lastTimestamp = notification.timestamp,
                        blocked = false,
                        changes = listOf(notification)
                    )
                }
            } catch (e: Exception) {
                value = emptyList()
            }
        } else {
            value = emptyList()
        }
    }

    val dayFilteredEntries = remember(entries, selectedDay) {
        val day = selectedDay
        if (day == null) entries
        else entries.filter { entry -> entry.changes.any { isSameDay(it.timestamp, day) } }
    }

    val filteredEntries = remember(dayFilteredEntries, searchQuery) {
        if (searchQuery.isBlank()) dayFilteredEntries
        else {
            val query = searchQuery.lowercase()
            dayFilteredEntries.filter { entry ->
                val n = entry.latest
                val app = (n?.appLabel ?: n?.packageName.orEmpty()).lowercase()
                val t = n?.title.orEmpty().lowercase()
                val tx = n?.text.orEmpty().lowercase()
                app.contains(query) || t.contains(query) || tx.contains(query)
            }
        }
    }

    // v7.12：被过滤列表从统一历史按 blocked 标记派生（原 blockedNotifications 已并入统一历史）
    val filteredBlocked = remember(entries, selectedDay, searchQuery) {
        val day = selectedDay
        var list = entries.filter { it.blocked }
        list = if (day == null) list
        else list.filter { entry -> entry.changes.any { isSameDay(it.timestamp, day) } }
        if (searchQuery.isBlank()) list
        else {
            val query = searchQuery.lowercase()
            list.filter { entry ->
                val n = entry.latest
                val app = (n?.appLabel ?: n?.packageName.orEmpty()).lowercase()
                val t = n?.title.orEmpty().lowercase()
                val tx = n?.text.orEmpty().lowercase()
                app.contains(query) || t.contains(query) || tx.contains(query)
            }
        }
    }

    // v7.44：分组/排序结果缓存——滑动切 tab 时不再每次重组全量重算（卡顿修复）
    val appGrouped = remember(filteredEntries) {
        filteredEntries.groupBy { it.appLabel ?: it.packageName.orEmpty() }
            .entries.sortedByDescending { (_, list) -> list.maxOf { it.lastTimestamp } }
    }
    val ruleById = remember(rules) { rules.associateBy { it.id } }
    val ruleGrouped = remember(filteredBlocked, rules) {
        val grouped = filteredBlocked.groupBy { entry ->
            entry.latest?.matchedRuleIds?.firstOrNull()
                ?: entry.changes.firstOrNull()?.matchedRuleIds?.firstOrNull()
        }
        grouped.entries.sortedWith(
            compareByDescending<Map.Entry<String?, List<NotificationHistoryEntry>>> { (_, list) -> list.maxOf { it.lastTimestamp } }
                .thenByDescending { it.key != null }
        )
    }

    // v7.45：通知折叠分段缓存——渲染前按时间倒序后按 packageName 连续分段（避免每次重组重算）
    val timeFoldSegments = remember(filteredEntries) {
        buildFoldSegments(filteredEntries.sortedByDescending { it.lastTimestamp })
    }
    // v7.47：BY_APP 组内折叠按发送时间判定——组内相邻两条须在全局时间线（filteredEntries 倒序）上位置连续
    // （中间无任何其他 app 条目）才保持同段，否则断开，避免全同 pkg 被并成一大段
    val appFoldSegments = remember(appGrouped) {
        val globalOrder = filteredEntries.sortedByDescending { it.lastTimestamp }
        appGrouped.map { (_, list) -> buildAppFoldSegments(list, globalOrder) }
    }
    val ruleFoldSegments = remember(ruleGrouped) {
        ruleGrouped.map { (_, list) -> buildFoldSegments(list.sortedByDescending { it.lastTimestamp }) }
    }
    // v7.51：折叠段开关——入参为段级唯一标识"${pkg}_${firstId}"，实现各折叠段独立展开/收起
    val toggleFold = { foldKey: String ->
        val isExpanded = expandedFoldPackages.value.contains(foldKey)
        expandedFoldPackages.value = if (isExpanded) {
            expandedFoldPackages.value - foldKey
        } else {
            expandedFoldPackages.value + foldKey
        }
        // v8.29：同步持久化展开状态
        foldStateStorage.toggle(foldKey)
        Unit
    }
    // v8.29：删除通知时迁移折叠展开状态——若被删通知是某展开段第一条，将展开状态迁移到新第一条
    val handleDeleteNotification: (SimpleNotification) -> Unit = { notification ->
        val allSegments = timeFoldSegments + appFoldSegments.flatten() + ruleFoldSegments.flatten()
        for (seg in allSegments) {
            if (seg.entries.isEmpty()) continue
            val first = seg.entries.first()
            val isFirstMatch = first.latest?.id == notification.id ||
                first.changes.any { it.id == notification.id }
            if (isFirstMatch && seg.entries.size >= 2) {
                val oldKey = "${seg.packageName}_${first.id}"
                if (expandedFoldPackages.value.contains(oldKey)) {
                    val newFirst = seg.entries[1]
                    val newKey = "${seg.packageName}_${newFirst.id}"
                    expandedFoldPackages.value = (expandedFoldPackages.value - oldKey) + newKey
                    foldStateStorage.migrateKey(oldKey, newKey)
                }
                break
            }
        }
        onDeleteNotification(notification)
    }

    // v7.41：totalCount/todayCount 计算已移至 ChartPanel（通用图表面板）
    // v7.36：未知规则组名（在 composable 上下文解析，供 LazyListScope 扩展使用）
    val unknownRuleLabel = stringResource(R.string.unknown_rule_group)
    val lay = MaterialTheme.notixLayout

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = lay.screenHorizontal)) {
        // v7.50：标题行 + 总记录/今日统计（移入 LazyColumn 顶部 item，随滚动滚出）
        val headerNowDate = LocalDate.now()
        val headerTotalCount = remember(entries) { entries.sumOf { it.count } }
        val headerTodayCount = remember(entries, headerNowDate) {
            entries.sumOf { e -> e.changes.count { isSameDay(it.timestamp, headerNowDate) } }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- 内容区：下拉刷新 + 统计行/柱状图/筛选行/通知列表 ---
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    onRefresh()
                    if (!isNotificationListenerEnabled(context)) {
                        showPermissionLostDialog = true
                    }
                    delay(600)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            // v7.37：三 tab 滑动切换——HorizontalPager 承载，每页独立 LazyColumn + 独立滚动状态
            // v7.40：beyondViewportPageCount=1 预组合相邻页，消除 FILTERED 切回空白
            val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            // v8.15.1：搜索/筛选 tab 抽出 pager，作为独立 header 常驻顶部（只渲染一份）。
            // 修复「点搜索触发 tab 切换」根因——SubTabsHeader 原在 pager 每页内各渲染一份，
            // searchExpanded 翻转时三页同时重算扰动 pager layout，settledPage 跳 page1 后由 LaunchedEffect 固化。
            Column(modifier = Modifier.fillMaxSize()) {
                // 固定筛选 tab（常驻顶部，不参与页面滚动）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.notix.surface)
                ) {
                    SubTabsHeader(
                        searchExpanded = searchExpanded,
                        onSearchExpandedChange = { searchExpanded = it },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        searchFocusRequester = searchFocusRequester,
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            coroutineScope.launch { tabPagerState.animateScrollToPage(tab.ordinal) }
                        },
                        onLongClickSearch = { showCrashLogDialog = true }
                    )
                }
                HorizontalPager(
                    state = tabPagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val tab = HistoryTab.entries[page]
                    // v7.48：折叠段收起后回滚段头所需的协程作用域与全局 item index 计数器
                    val listScope = rememberCoroutineScope()
                    // v7.40：横屏时图表固定左栏、通知列表右栏；竖屏保持原滚动结构
                    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                    // v8.4：筛选 tab 改用原生 stickyHeader 吸顶，删除手动浮动层与 derivedStateOf 判定
                    // （双栏/卡顿根因修复；分组头 stickyHeader 自动吸附在 sub_tabs 下方）
                    // 图表头部块：标题行 + 统计 + 柱状图 + 日期筛选行（v7.41：抽为 ChartPanel 通用图表面板）
                    val headerBlock: @Composable () -> Unit = {
                        ChartPanel(
                            entries = entries,
                            selectedDay = selectedDay,
                            onDayClick = { day ->
                                onSelectedDayChange(if (selectedDay == day) null else day)
                            },
                            onClearDay = { onSelectedDayChange(null) },
                            listenerPaused = listenerPaused,
                            onToggleListenerPaused = { showListenerPauseConfirm = true },
                            onBackToCurrentWeek = onBackToCurrentWeek,
                            backToCurrentWeekTrigger = backToCurrentWeekTrigger,
                            pagerState = chartPagerStates[page],
                            // v7.50：竖屏标题行/统计已在 tab 上方渲染，图表内不再重复
                            showTitle = false
                        )
                    }
                    if (isLandscape) {
                        // v8.4：横屏——图表已由外层 TabbedScreen 左栏渲染（ChartPanel），此处仅渲染通知列表
                        // 筛选 tab 已移到 pager 上方常驻（见上方 SubTabsHeader）；分组头 stickyHeader 吸顶在其下方
                        LazyColumn(
                            state = tabListStates[page],
                            modifier = Modifier.fillMaxSize(),
                            // v8.16：移除额外底部滚动余量，仅保留导航条高度
                            contentPadding = PaddingValues(bottom = navBottomPadding)
                        ) {
                            historyListItems(
                                tab = tab,
                                entries = entries,
                                filteredEntries = filteredEntries,
                                filteredBlocked = filteredBlocked,
                                unmonitoredApps = unmonitoredApps,
                                expandedApps = expandedApps,
                                expandedRuleIds = expandedRuleIds,
                                expandedFoldPackages = expandedFoldPackages,
                                onToggleFold = toggleFold,
                                timeFoldSegments = timeFoldSegments,
                                appFoldSegments = appFoldSegments,
                                ruleFoldSegments = ruleFoldSegments,
                                rules = rules,
                                appGrouped = appGrouped,
                                ruleById = ruleById,
                                ruleGrouped = ruleGrouped,
                                unknownRuleLabel = unknownRuleLabel,
                                onEntryHistoryClick = onEntryHistoryClick,
                                onOpenNotification = onOpenNotification,
                                onRestoreNotification = onRestoreNotification,
                                onCreateRuleFromNotification = onCreateRuleFromNotification,
                                onDeleteNotification = handleDeleteNotification,
                                onResumeMonitoring = onResumeMonitoring,
                                onShowStopMonitoringDialog = { showStopMonitoringDialog = it },
                                context = context,
                                itemIndex = IntArray(1),
                                listState = tabListStates[page],
                                scope = listScope,
                                hasMore = hasMore,
                                loadingMore = loadingMore,
                                onLoadMore = onLoadMore
                            )
                        }
                    } else {
                        // v8.4：竖屏——筛选 tab 已移到 pager 上方常驻（见上方 SubTabsHeader）；
                        // title/图表区作为列表顶部 item 随滚动滑出；分组头 stickyHeader 吸顶在 tab 下方
                        LazyColumn(
                            state = tabListStates[page],
                            modifier = Modifier.fillMaxSize(),
                            // v8.16：移除额外底部滚动余量，仅保留导航条高度
                            contentPadding = PaddingValues(bottom = navBottomPadding)
                        ) {
                            // v7.51：标题行作为普通 item 随滚动滑出
                            item(key = "history_title") {
                                HistoryTitleRow(
                                    totalCount = headerTotalCount,
                                    todayCount = headerTodayCount,
                                    listenerPaused = listenerPaused,
                                    onToggleListenerPaused = { showListenerPauseConfirm = true },
                                    onBackToCurrentWeek = onBackToCurrentWeek
                                )
                            }
                            item(key = "history_header") {
                                headerBlock()
                            }
                            historyListItems(
                                tab = tab,
                                entries = entries,
                                filteredEntries = filteredEntries,
                                filteredBlocked = filteredBlocked,
                                unmonitoredApps = unmonitoredApps,
                                expandedApps = expandedApps,
                                expandedRuleIds = expandedRuleIds,
                                expandedFoldPackages = expandedFoldPackages,
                                onToggleFold = toggleFold,
                                timeFoldSegments = timeFoldSegments,
                                appFoldSegments = appFoldSegments,
                                ruleFoldSegments = ruleFoldSegments,
                                rules = rules,
                                appGrouped = appGrouped,
                                ruleById = ruleById,
                                ruleGrouped = ruleGrouped,
                                unknownRuleLabel = unknownRuleLabel,
                                onEntryHistoryClick = onEntryHistoryClick,
                                onOpenNotification = onOpenNotification,
                                onRestoreNotification = onRestoreNotification,
                                onCreateRuleFromNotification = onCreateRuleFromNotification,
                                onDeleteNotification = handleDeleteNotification,
                                onResumeMonitoring = onResumeMonitoring,
                                onShowStopMonitoringDialog = { showStopMonitoringDialog = it },
                                context = context,
                                itemIndex = IntArray(1),
                                listState = tabListStates[page],
                                scope = listScope,
                                hasMore = hasMore,
                                loadingMore = loadingMore,
                                onLoadMore = onLoadMore
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- v7.40：历史页三 tab 列表内容（竖屏/横屏共用） ---
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.historyListItems(
    tab: HistoryTab,
    entries: List<NotificationHistoryEntry>,
    filteredEntries: List<NotificationHistoryEntry>,
    filteredBlocked: List<NotificationHistoryEntry>,
    unmonitoredApps: Set<String>,
    expandedApps: MutableState<Set<String>>,
    expandedRuleIds: MutableState<Set<String>>,
    expandedFoldPackages: MutableState<Set<String>>,
    onToggleFold: (String) -> Unit,
    timeFoldSegments: List<FoldSegment>,
    appFoldSegments: List<List<FoldSegment>>,
    ruleFoldSegments: List<List<FoldSegment>>,
    rules: List<BlockerRule>,
    appGrouped: List<Map.Entry<String, List<NotificationHistoryEntry>>>,
    ruleById: Map<String, BlockerRule>,
    ruleGrouped: List<Map.Entry<String?, List<NotificationHistoryEntry>>>,
    unknownRuleLabel: String,
    onEntryHistoryClick: (NotificationHistoryEntry) -> Unit,
    onOpenNotification: (SimpleNotification) -> Unit,
    onRestoreNotification: (SimpleNotification) -> Unit,
    onCreateRuleFromNotification: (SimpleNotification) -> Unit,
    onDeleteNotification: (SimpleNotification) -> Unit,
    onResumeMonitoring: (String, String) -> Unit,
    onShowStopMonitoringDialog: (Pair<String, String>?) -> Unit,
    context: android.content.Context,
    itemIndex: IntArray,
    listState: LazyListState,
    scope: CoroutineScope,
    // v8.22：分页加载
    hasMore: Boolean = false,
    loadingMore: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    when (tab) {
        HistoryTab.BY_TIME -> {
            if (entries.isEmpty()) {
                item { EmptyStateBox(Icons.Outlined.Inbox, stringResource(R.string.no_notifications_yet), stringResource(R.string.no_notifications_yet_desc)) }
            } else if (filteredEntries.isEmpty()) {
                item { EmptyStateBox(Icons.Outlined.SearchOff, stringResource(R.string.no_results_found), stringResource(R.string.no_results_found_desc)) }
            } else {
                byTimeItems(
                    segments = timeFoldSegments,
                    expandedFoldPackages = expandedFoldPackages,
                    onToggleFold = onToggleFold,
                    onEntryHistoryClick = onEntryHistoryClick,
                    onOpenNotification = onOpenNotification,
                    onRestoreNotification = onRestoreNotification,
                    onCreateRuleFromNotification = onCreateRuleFromNotification,
                    onDeleteNotification = onDeleteNotification,
                    context = context,
                    itemIndex = itemIndex,
                    listState = listState,
                    scope = scope,
                )
            }
        }
        HistoryTab.BY_APP -> {
            if (entries.isEmpty()) {
                item { EmptyStateBox(Icons.Outlined.Inbox, stringResource(R.string.no_notifications_yet), stringResource(R.string.no_notifications_yet_desc)) }
            } else if (filteredEntries.isEmpty()) {
                item { EmptyStateBox(Icons.Outlined.SearchOff, stringResource(R.string.no_results_found), stringResource(R.string.no_results_found_desc)) }
            } else {
                byAppItems(
                    appGrouped = appGrouped,
                    groupFoldSegments = appFoldSegments,
                    unmonitoredApps = unmonitoredApps,
                    expandedApps = expandedApps,
                    expandedFoldPackages = expandedFoldPackages,
                    onToggleFold = onToggleFold,
                    onEntryHistoryClick = onEntryHistoryClick,
                    onOpenNotification = onOpenNotification,
                    onRestoreNotification = onRestoreNotification,
                    onCreateRuleFromNotification = onCreateRuleFromNotification,
                    onDeleteNotification = onDeleteNotification,
                    onResumeMonitoring = onResumeMonitoring,
                    onShowStopMonitoringDialog = onShowStopMonitoringDialog,
                    context = context,
                    itemIndex = itemIndex,
                    listState = listState,
                    scope = scope,
                )
            }
        }
        HistoryTab.FILTERED -> {
            if (filteredBlocked.isEmpty()) {
                item { EmptyStateBox(Icons.Outlined.Inbox, stringResource(R.string.no_notifications_yet), stringResource(R.string.no_notifications_yet_desc)) }
            } else {
                byRuleItems(
                    ruleById = ruleById,
                    ruleGrouped = ruleGrouped,
                    groupFoldSegments = ruleFoldSegments,
                    expandedRuleIds = expandedRuleIds,
                    expandedFoldPackages = expandedFoldPackages,
                    onToggleFold = onToggleFold,
                    unknownGroupLabel = unknownRuleLabel,
                    onEntryHistoryClick = onEntryHistoryClick,
                    onOpenNotification = onOpenNotification,
                    onRestoreNotification = onRestoreNotification,
                    onCreateRuleFromNotification = onCreateRuleFromNotification,
                    onDeleteNotification = onDeleteNotification,
                    context = context,
                    itemIndex = itemIndex,
                    listState = listState,
                    scope = scope,
                )
            }
        }
    }
}

private fun isSameDay(timestamp: Long, day: LocalDate): Boolean {
    return LocalDate.ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()) == day
}

// --- v7.37：创建多个独立图表 PagerState（一个 PagerState 不能同时绑定多个 pager） ---
// v7.40：旋转恢复——图表当前周页码持久化（rememberPagerState + 页码写回兜底）
@Composable
private fun rememberChartPagerStates(
    count: Int,
    initialPage: Int,
    pageCount: () -> Int
): List<PagerState> {
    var savedPages by rememberSaveable { mutableStateOf(List(count) { initialPage }) }
    val states = List(count) { index ->
        rememberPagerState(initialPage = savedPages[index], pageCount = pageCount)
    }
    // 同步：任一图表页 settledPage 变化时写回持久化状态
    LaunchedEffect(states) {
        snapshotFlow { states.map { it.settledPage } }.collect { pages ->
            savedPages = pages
        }
    }
    return states
}

// --- v7.41：通用图表面板（标题行+统计+柱状图+日期筛选行），横屏左栏与竖屏列表头部共用 ---
@Composable
internal fun ChartPanel(
    entries: List<NotificationHistoryEntry>,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    onClearDay: () -> Unit,
    listenerPaused: Boolean,
    onToggleListenerPaused: () -> Unit,
    onBackToCurrentWeek: () -> Unit,
    backToCurrentWeekTrigger: Int,
    pagerState: PagerState? = null,
    showTitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    val sp = MaterialTheme.notixSpacing
    val nowDate = LocalDate.now()
    val thisMonday = nowDate.with(DayOfWeek.MONDAY)
    val weeksBefore = 26
    val weeksAfter = 26
    val totalPages = weeksBefore + 1 + weeksAfter
    val currentWeekPage = weeksBefore
    val firstWeekStart = thisMonday.minusWeeks(weeksBefore.toLong())
    // v7.15：柱状图与今日计数/日期详情统一口径——按历史聚合组内每条 change 的时间戳归属日期统计
    val chartCounts = remember(entries, firstWeekStart) {
        val dayCounts = mutableMapOf<LocalDate, Int>()
        entries.forEach { e ->
            e.changes.forEach { c ->
                val d = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(c.timestamp), ZoneId.systemDefault())
                dayCounts[d] = (dayCounts[d] ?: 0) + 1
            }
        }
        buildMap {
            for (i in 0 until totalPages * 7) {
                val day = firstWeekStart.plusDays(i.toLong())
                put(day, dayCounts[day] ?: 0)
            }
        }
    }
    val chartMax = remember(chartCounts) { chartCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1 }
    // 未传入外部 PagerState（横屏通用面板）时内部自建，页码随旋转保持；竖屏由 HistoryScreen 按页传入独立实例
    val internalPagerState = rememberPagerState(initialPage = currentWeekPage, pageCount = { totalPages })
    val chartPager = pagerState ?: internalPagerState
    // 返回本周：仅内部实例自行滚动（竖屏每页实例由 HistoryScreen 的 LaunchedEffect 驱动）
    LaunchedEffect(pagerState == null, backToCurrentWeekTrigger) {
        if (pagerState == null && backToCurrentWeekTrigger > 0) {
            chartPager.animateScrollToPage(currentWeekPage)
        }
    }
    val totalCount = remember(entries) { entries.sumOf { it.count } }
    // v7.15：今日计数与柱状图/日期详情统一口径——按聚合组内 change 时间戳归属今日计数
    val todayCount = remember(entries, nowDate) {
        entries.sumOf { e -> e.changes.count { isSameDay(it.timestamp, nowDate) } }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        // v7.50：竖屏时标题行 + 统计已移至 tab 上方（showTitle=false）；横屏左栏保留完整面板
        if (showTitle) {
            // 顶部区：「通知历史」标题行 + 总通知/今日统计（v7.49：与柱状图卡片视觉分离）
            HistoryTitleRow(
                totalCount = totalCount,
                todayCount = todayCount,
                listenerPaused = listenerPaused,
                onToggleListenerPaused = onToggleListenerPaused,
                onBackToCurrentWeek = onBackToCurrentWeek
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = sp.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Spacer(modifier = Modifier.weight(1f))
                    // v7.51：总记录/今日合并为单行
                    Text(
                        text = stringResource(R.string.history_total_today, totalCount, todayCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.notix.contentSecondary
                    )
                }
            }
            // v7.49：柱状图独立为圆角深灰卡片；左右小箭头（◀/▶）仅作滑动提示，不可点击
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                shape = NotixCorner.Card,
                color = MaterialTheme.notix.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = MaterialTheme.notix.contentSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                StatsBarChart(
                    modifier = Modifier.weight(1f),
                    pagerState = chartPager,
                    firstWeekStart = firstWeekStart,
                    countsByDay = chartCounts,
                    chartMax = chartMax,
                    currentWeekStart = thisMonday,
                    selectedDay = selectedDay,
                    onDayClick = onDayClick
                )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.notix.contentSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.padding(end = 6.dp)
                    )
            }
        }
        val filterDay = selectedDay
        if (filterDay != null) {
            FilteredDayRow(day = filterDay, onClear = onClearDay)
        }
    }
}

// --- v7.5：通知监听权限是否掉线 ---
private fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    val expected = ComponentName(context, NotificationBlockerService::class.java).flattenToString()
    return flat.split(":").any { it.equals(expected, ignoreCase = true) }
}

// --- 柱状图：以周为单位分页，一屏一周 ---
@Composable
private fun StatsBarChart(
    pagerState: PagerState,
    firstWeekStart: LocalDate,
    countsByDay: Map<LocalDate, Int>,
    chartMax: Int,
    currentWeekStart: LocalDate,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val sp = MaterialTheme.notixSpacing
    val maxBarHeight = 96.dp

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .height(144.dp)
    ) { page ->
        val weekStart = firstWeekStart.plusWeeks(page.toLong())
        val isCurrentWeek = weekStart == currentWeekStart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            (0 until 7).forEach { i ->
                val day = weekStart.plusDays(i.toLong())
                val count = countsByDay[day] ?: 0
                val barHeight = if (count > 0) {
                    maxBarHeight * (count.toFloat() / chartMax)
                } else {
                    2.dp
                }
                val isSelected = selectedDay == day
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable { onDayClick(day) }
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (count > 0) MaterialTheme.notix.contentPrimary else MaterialTheme.notix.contentSecondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = sp.sm, topEnd = sp.sm))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    count > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isCurrentWeek) {
                            val weekDay = day.dayOfWeek.value
                            stringResource(
                                when (weekDay) {
                                    1 -> R.string.week_mon
                                    2 -> R.string.week_tue
                                    3 -> R.string.week_wed
                                    4 -> R.string.week_thu
                                    5 -> R.string.week_fri
                                    6 -> R.string.week_sat
                                    else -> R.string.week_sun
                                }
                            )
                        } else {
                            "${day.monthValue}.${day.dayOfMonth}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.notix.contentSecondary
                    )
                }
            }
        }
    }
}

// --- 筛选提示行：点击柱子后显示"已筛选 年-月-日 的通知"，右侧叉号关闭 ---
@Composable
private fun FilteredDayRow(day: LocalDate, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.history_filtered_day, day.toString()),
            style = MaterialTheme.notixType.button,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClear) {
            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_filter))
        }
    }
}

// --- 空状态占满剩余空间 ---
@Composable
private fun androidx.compose.foundation.lazy.LazyItemScope.EmptyStateBox(
    icon: ImageVector,
    title: String,
    description: String
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        EmptyState(icon = icon, title = title, description = description)
    }
}

// --- v7.51：三个分组 tab + 搜索按钮（吸顶 stickyHeader 区域，随列表滚到顶部即吸附） ---
@Composable
private fun SubTabsHeader(
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchFocusRequester: FocusRequester,
    selectedTab: HistoryTab,
    onTabSelected: (HistoryTab) -> Unit,
    onLongClickSearch: () -> Unit,
) {
    val headerMode = if (searchExpanded) SearchHeaderMode.SEARCH_EXPANDED else SearchHeaderMode.NORMAL
    AnimatedContent(
        targetState = headerMode,
        transitionSpec = {
            // v7.6：平滑缓动曲线；搜索展开/收起从右侧按钮处向左缩放展开
            val isSearchTransition =
                initialState == SearchHeaderMode.SEARCH_EXPANDED || targetState == SearchHeaderMode.SEARCH_EXPANDED
            val tweenSpec = tween<Float>(
                durationMillis = if (isSearchTransition) 300 else 220,
                easing = FastOutSlowInEasing
            )
            if (isSearchTransition) {
                val origin = TransformOrigin(1f, 0.5f)
                (fadeIn(tweenSpec) + scaleIn(tweenSpec, initialScale = 0.9f, transformOrigin = origin))
                    .togetherWith(fadeOut(tweenSpec) + scaleOut(tweenSpec, targetScale = 0.9f, transformOrigin = origin))
                    .using(SizeTransform(clip = false))
            } else {
                fadeIn(tweenSpec) togetherWith fadeOut(tweenSpec)
            }
        },
        label = "search_header"
    ) { mode ->
        when (mode) {
            SearchHeaderMode.NORMAL -> Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // v7.7：常规与吸顶统一紧凑样式——小字号 tab、紧凑高度、搜索按钮在 tab 行最右侧
                HistorySubTabs(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.weight(1f),
                    compact = true
                )
                Spacer(modifier = Modifier.width(4.dp))
                SearchButton(
                    onClick = { onSearchExpandedChange(true) },
                    onLongClick = onLongClickSearch
                )
            }
            SearchHeaderMode.SEARCH_EXPANDED -> Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // v7.7：改用 BasicTextField 自绘紧凑输入框，显式指定文字/光标颜色修复不可见问题
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .focusRequester(searchFocusRequester)
                        .clip(NotixCorner.Card)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp),
                    textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_notifications),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                // v7.7：关闭搜索时清除搜索条件与输入内容，列表恢复显示全部
                IconButton(onClick = {
                    onSearchExpandedChange(false)
                    onSearchQueryChange("")
                }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                }
            }
        }
    }
}

// --- v7.7：标题行（"通知历史" + 总记录/今日统计 + 通知监听铃铛），作为列表项随内容滑出 ---
// v7.50：统计移至标题行右侧（竖屏顶部固定展示；横屏左栏 ChartPanel 内展示）
// v7.51：竖屏标题行移入 LazyColumn 顶部 item，随滚动滚出
@Composable
private fun HistoryTitleRow(
    totalCount: Int,
    todayCount: Int,
    listenerPaused: Boolean,
    onToggleListenerPaused: () -> Unit,
    onBackToCurrentWeek: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sp = MaterialTheme.notixSpacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onBackToCurrentWeek() }
            .padding(top = sp.sm, bottom = 2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.notixType.display,
            modifier = Modifier.alignByBaseline()
        )
        Spacer(modifier = Modifier.width(sp.sm).alignByBaseline())
        // v8.4：总记录/今日合并为单行，与标题按基线对齐、紧凑间距
        Text(
            text = stringResource(R.string.history_total_today, totalCount, todayCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.notix.contentSecondary,
            modifier = Modifier.alignByBaseline()
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { onToggleListenerPaused() }) {
            if (listenerPaused) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = stringResource(R.string.listener_pause),
                    tint = MaterialTheme.colorScheme.error
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.listener_monitor),
                    tint = MaterialTheme.notix.contentSecondary
                )
            }
        }
    }
}

// --- Sub-tab bar ---
@Composable
private fun HistorySubTabs(
    selectedTab: HistoryTab,
    onTabSelected: (HistoryTab) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val sp = MaterialTheme.notixSpacing
    val tabs = listOf(
        HistoryTab.BY_TIME to R.string.history_tab_by_time,
        HistoryTab.BY_APP to R.string.history_tab_by_app,
        HistoryTab.FILTERED to R.string.history_tab_filtered
    )
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(if (compact) sp.xs else sp.sm)
    ) {
        tabs.forEach { (tab, labelRes) ->
            val isSelected = selectedTab == tab
                Card(
                    onClick = { onTabSelected(tab) },
                    shape = NotixCorner.Card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.notix.outlineVariant),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                ) {
                Text(
                    text = stringResource(labelRes),
                    style = if (isSelected) MaterialTheme.notixType.cardTitle else MaterialTheme.notixType.bodySecondary,
                    modifier = Modifier.padding(horizontal = if (compact) sp.md else sp.lg, vertical = if (compact) 5.dp else sp.sm),
                    color = if (isSelected) MaterialTheme.notix.contentPrimary else MaterialTheme.notix.contentSecondary
                )
            }
        }
    }
}

// v7.6：搜索按钮（搜索框改为按钮形态，置于三 tab 行最右侧）
// v7.13：长按打开崩溃日志弹窗
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchButton(onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(NotixCorner.Card)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
    }
}

// --- "By Time" tab --- 聚合条目列表（LazyListScope 扩展，供外层 LazyColumn 使用） ---
// v7.45：改为按折叠分段渲染（连续同 app 且 count 合计 >= 4 时折叠）
private fun LazyListScope.byTimeItems(
    segments: List<FoldSegment>,
    expandedFoldPackages: MutableState<Set<String>>,
    onToggleFold: (String) -> Unit,
    onEntryHistoryClick: (NotificationHistoryEntry) -> Unit,
    onOpenNotification: (SimpleNotification) -> Unit,
    onRestoreNotification: (SimpleNotification) -> Unit,
    onCreateRuleFromNotification: (SimpleNotification) -> Unit,
    onDeleteNotification: (SimpleNotification) -> Unit,
    context: android.content.Context,
    itemIndex: IntArray,
    listState: LazyListState,
    scope: CoroutineScope
) {
    foldSegments(
        segments = segments,
        expandedFoldPackages = expandedFoldPackages,
        onToggleFold = onToggleFold,
        onEntryHistoryClick = onEntryHistoryClick,
        onOpenNotification = onOpenNotification,
        onRestoreNotification = onRestoreNotification,
        onCreateRuleFromNotification = onCreateRuleFromNotification,
        onDeleteNotification = onDeleteNotification,
        context = context,
        itemIndex = itemIndex,
        listState = listState,
        scope = scope,
    )
}

// --- "By App" tab --- 按应用分组；分组标题吸顶，右侧为监控按钮 ---
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.byAppItems(
    appGrouped: List<Map.Entry<String, List<NotificationHistoryEntry>>>,
    groupFoldSegments: List<List<FoldSegment>>,
    unmonitoredApps: Set<String>,
    expandedApps: MutableState<Set<String>>,
    expandedFoldPackages: MutableState<Set<String>>,
    onToggleFold: (String) -> Unit,
    onEntryHistoryClick: (NotificationHistoryEntry) -> Unit,
    onOpenNotification: (SimpleNotification) -> Unit,
    onRestoreNotification: (SimpleNotification) -> Unit,
    onCreateRuleFromNotification: (SimpleNotification) -> Unit,
    onDeleteNotification: (SimpleNotification) -> Unit,
    onResumeMonitoring: (String, String) -> Unit,
    onShowStopMonitoringDialog: (Pair<String, String>?) -> Unit,
    context: android.content.Context,
    itemIndex: IntArray,
    listState: LazyListState,
    scope: CoroutineScope
) {
    // v7.44：分组结果由上层 remember 缓存传入，此处直接遍历
    // v7.45：组内按折叠分段渲染（同 app 组内 count 合计 >= 4 时折叠）
    appGrouped.forEachIndexed { index, (appName, appEntries) ->
        val packageName = appEntries.firstOrNull()?.packageName
        val isExpanded = expandedApps.value.contains(appName)

        itemIndex[0]++
        // v7.54：分组头 stickyHeader 吸顶（浮动 tab 已由 Column 占位，吸顶位置自然位于其下方）
        stickyHeader(key = "header_$appName") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.notix.surface)
            ) {
                AppGroupHeader(
                    appName = appName,
                    count = appEntries.sumOf { it.count },
                    packageName = packageName,
                    isExpanded = isExpanded,
                    isUnmonitored = unmonitoredApps.contains(packageName),
                    onClick = {
                        expandedApps.value = if (isExpanded) expandedApps.value - appName else expandedApps.value + appName
                    },
                    onStopMonitoringClick = {
                        if (packageName != null) onShowStopMonitoringDialog(packageName to appName)
                    },
                    onResumeMonitoringClick = {
                        if (packageName != null) onResumeMonitoring(packageName, appName)
                    }
                )
            }
        }

        if (isExpanded) {
            foldSegments(
                segments = groupFoldSegments.getOrNull(index) ?: emptyList(),
                expandedFoldPackages = expandedFoldPackages,
                onToggleFold = onToggleFold,
                onEntryHistoryClick = onEntryHistoryClick,
                onOpenNotification = onOpenNotification,
                onRestoreNotification = onRestoreNotification,
                onCreateRuleFromNotification = onCreateRuleFromNotification,
                onDeleteNotification = onDeleteNotification,
                context = context,
                itemIndex = itemIndex,
                listState = listState,
                scope = scope,
            )
        }
    }

}

@Composable
private fun AppGroupHeader(
    appName: String,
    count: Int,
    packageName: String?,
    isExpanded: Boolean,
    isUnmonitored: Boolean,
    onClick: () -> Unit,
    onStopMonitoringClick: () -> Unit,
    onResumeMonitoringClick: () -> Unit,
) {
    val sp = MaterialTheme.notixSpacing
    val context = LocalContext.current
    // v7.9：NotificationColorEngine 动态配色（Icon→主色提取→HSL 背景→WCAG 文字），后台线程分析 + 缓存
    val colors by produceState<NotificationColors?>(initialValue = null, key1 = packageName to NotificationColorEngine.colorVersion) {
        value = withContext(Dispatchers.Default) {
            NotificationColorEngine.getNotificationColors(context, packageName)
        }
    }
    val headerBg = colors?.backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    // v7.9：文字颜色由引擎按实际对比度选择（白/黑），加载完成前用主题 onSurface
    val headerFg = colors?.primaryTextColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    // v8.18 优化：左侧色条改用 accentColor（品牌色明亮版），形成暗背景+亮装饰层次
    val accent = colors?.accentColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    var showResumeConfirmDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() }
            .then(if (isUnmonitored) Modifier.alpha(0.6f) else Modifier),
        shape = NotixCorner.ListItem,
        colors = CardDefaults.cardColors(
            containerColor = headerBg
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = sp.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // v7.8：左侧主色深色版装饰条
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(topEnd = sp.xs, bottomEnd = sp.xs))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(11.dp))
            RealAppIcon(
                packageName = packageName,
                appName = appName,
                size = 28.dp,
                modifier = if (isUnmonitored) Modifier.drawWithContent {
                    drawContent()
                    drawLine(
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = 3.dp.toPx()
                    )
                } else Modifier,
                shape = NotixCorner.Sm,
            )
            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = appName,
                style = MaterialTheme.notixType.cardTitle,
                color = headerFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // v7.8：变更计数角标——主色深色版底 + 白色文字
            // v8.18 优化：计数+箭头统一包裹在半透明背景中（与 NotificationCard CountBadge 一致）
            Box(
                modifier = Modifier
                    .clip(NotixCorner.Sm)
                    .background(headerFg.copy(alpha = 0.18f))
                    .padding(horizontal = sp.sm, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = headerFg
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(if (isExpanded) R.string.collapse else R.string.expand),
                        tint = headerFg,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(sp.xs))

            // 监控按钮：放到分组卡片右侧（标题行右侧）
            IconButton(onClick = { if (isUnmonitored) showResumeConfirmDialog = true else onStopMonitoringClick() }) {
                Icon(
                    imageVector = if (isUnmonitored) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                    contentDescription = stringResource(if (isUnmonitored) R.string.resume else R.string.stop_monitoring_short),
                    tint = headerFg.copy(alpha = 0.8f)
                )
            }
        }
    }
    if (showResumeConfirmDialog) {
        NotixConfirmDialog(
            onDismiss = { showResumeConfirmDialog = false },
            onConfirm = {
                onResumeMonitoringClick()
                showResumeConfirmDialog = false
            },
            title = stringResource(R.string.resume_monitoring_title),
            body = stringResource(R.string.resume_monitoring_confirm, appName),
            confirmText = stringResource(R.string.resume)
        )
    }
}

// --- "Filtered" tab --- 按规则分组（v7.36）：组头右侧无操作按钮（仅按应用 tab 保留停止监控）
private fun LazyListScope.byRuleItems(
    ruleById: Map<String, BlockerRule>,
    ruleGrouped: List<Map.Entry<String?, List<NotificationHistoryEntry>>>,
    groupFoldSegments: List<List<FoldSegment>>,
    expandedRuleIds: MutableState<Set<String>>,
    expandedFoldPackages: MutableState<Set<String>>,
    onToggleFold: (String) -> Unit,
    unknownGroupLabel: String,
    onEntryHistoryClick: (NotificationHistoryEntry) -> Unit,
    onOpenNotification: (SimpleNotification) -> Unit,
    onRestoreNotification: (SimpleNotification) -> Unit,
    onCreateRuleFromNotification: (SimpleNotification) -> Unit,
    onDeleteNotification: (SimpleNotification) -> Unit,
    context: android.content.Context,
    itemIndex: IntArray,
    listState: LazyListState,
    scope: CoroutineScope
) {
    // v7.44：ruleById/分组/排序结果由上层 remember 缓存传入，此处直接遍历
    // v7.45：组内按折叠分段渲染（规则组内再按连续同 app 分段）
    ruleGrouped.forEachIndexed { index, (ruleId, groupEntries) ->
        val rule = ruleId?.let { ruleById[it] }
        val first = groupEntries.firstOrNull()
        val sourceApp = rule?.sourcePackages?.firstOrNull()
        val appName = sourceApp?.appName?.takeIf { it.isNotBlank() }
            ?: sourceApp?.packageName
            ?: first?.appLabel
            ?: first?.packageName
        val packageName = sourceApp?.packageName ?: first?.packageName
        val groupKey = ruleId ?: "unknown"
        val title = if (rule != null) buildRuleGroupTitle(rule, appName) else unknownGroupLabel
        val isExpanded = expandedRuleIds.value.contains(groupKey)

        itemIndex[0]++
        // v7.54：规则分组头 stickyHeader 吸顶（浮动 tab 已由 Column 占位，吸顶位置自然位于其下方）
        stickyHeader(key = "rule_header_$groupKey") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.notix.surface)
            ) {
                RuleGroupHeader(
                    title = title,
                    count = groupEntries.sumOf { it.count },
                    packageName = packageName,
                    isExpanded = isExpanded,
                    isUnknown = rule == null,
                    onClick = {
                        expandedRuleIds.value =
                            if (isExpanded) expandedRuleIds.value - groupKey else expandedRuleIds.value + groupKey
                    }
                )
            }
        }

        if (isExpanded) {
            foldSegments(
                segments = groupFoldSegments.getOrNull(index) ?: emptyList(),
                expandedFoldPackages = expandedFoldPackages,
                onToggleFold = onToggleFold,
                onEntryHistoryClick = onEntryHistoryClick,
                onOpenNotification = onOpenNotification,
                onRestoreNotification = onRestoreNotification,
                onCreateRuleFromNotification = onCreateRuleFromNotification,
                onDeleteNotification = onDeleteNotification,
                context = context,
                itemIndex = itemIndex,
                listState = listState,
                scope = scope,
            )
        }
    }
}

// v7.36：组名 = "来源应用 · 关键字摘要"（如"微信 · 广告"）；无关键字时只显示来源应用
private fun buildRuleGroupTitle(rule: BlockerRule, appName: String?): String {
    val source = appName.orEmpty()
    // v7.13：空安全兜底——旧数据可能残留 null 字段
    val cond = rule.condition ?: com.enlpot.notix.RuleCondition()
    val keywords = cond.includeKeywords.filter { it.isNotBlank() }
        .ifEmpty { cond.excludeKeywords.filter { it.isNotBlank() } }
        .take(2)
        .joinToString("、")
    return when {
        source.isNotBlank() && keywords.isNotBlank() -> "$source · $keywords"
        source.isNotBlank() -> source
        keywords.isNotBlank() -> keywords
        else -> ""
    }
}

@Composable
private fun RuleGroupHeader(
    title: String,
    count: Int,
    packageName: String?,
    isExpanded: Boolean,
    isUnknown: Boolean,
    onClick: () -> Unit,
) {
    val sp = MaterialTheme.notixSpacing
    val context = LocalContext.current
    // 未知规则组固定默认灰色配色；其余复用 NotificationColorEngine 动态配色（与按应用一致）
    val colors by produceState<NotificationColors?>(initialValue = null, key1 = (if (isUnknown) null else packageName) to NotificationColorEngine.colorVersion) {
        value = withContext(Dispatchers.Default) {
            if (isUnknown) null
            else NotificationColorEngine.getNotificationColors(context, packageName)
        }
    }
    val headerBg = colors?.backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val headerFg = colors?.primaryTextColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    val fallbackAccent = if (isUnknown) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
    val accent = colors?.backgroundColor?.let { Color(it) } ?: fallbackAccent
    val accentFg = remember(accent) { Color(NotificationColorEngine.chooseTextColor(accent.toArgb())) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        shape = NotixCorner.ListItem,
        colors = CardDefaults.cardColors(
            containerColor = headerBg
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = sp.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(topEnd = sp.xs, bottomEnd = sp.xs))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(11.dp))
            RealAppIcon(
                packageName = packageName,
                appName = title,
                size = 28.dp,
                shape = NotixCorner.Sm,
            )
            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = title,
                style = MaterialTheme.notixType.cardTitle,
                color = headerFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .clip(NotixCorner.Sm)
                    .background(accent)
                    .padding(horizontal = sp.sm, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentFg
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = stringResource(if (isExpanded) R.string.collapse else R.string.expand),
                tint = headerFg.copy(alpha = 0.8f)
            )

            // v7.36：本 tab 组头右侧不放操作按钮（按应用 tab 的停止监控按钮保留）
            Spacer(modifier = Modifier.width(sp.xs))
        }
    }
}

// --- 聚合通知卡片：单击弹菜单；右侧徽标点击打开变更历史 ---
// v6（Stage 6）：History 列表通知卡包装器——使用 NotificationCard 组件（accent 整卡底色），
// 页面层计算 accent/onAccent 注入，详情弹窗由包装器内部管理。
// v7.45：新增 indent 参数——折叠展开后的卡片水平缩进（宽度略缩），与未折叠卡片区分
@Composable
private fun HistoryNotificationCard(
    entry: NotificationHistoryEntry,
    onHistoryClick: () -> Unit,
    onOpen: () -> Unit,
    onRestore: () -> Unit,
    onCreateRule: () -> Unit,
    onDelete: () -> Unit,
    showRestore: Boolean,
    context: android.content.Context,
    compact: Boolean = false,
    indent: Dp = 0.dp,
) {
    val notification = entry.latest ?: return
    val packageName = notification.packageName
    var menuExpanded by remember { mutableStateOf(false) }

    // accent 整卡底色：经 NotificationColorEngine 取色（与 RulesScreen 一致）
    val colors by produceState<NotificationColors?>(initialValue = null, key1 = packageName to NotificationColorEngine.colorVersion) {
        value = withContext(Dispatchers.Default) {
            NotificationColorEngine.getNotificationColors(context, packageName)
        }
    }
    val accent = colors?.backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val onAccent = colors?.primaryTextColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant

    val onAccentTertiary = colors?.tertiaryTextColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val displayAppName = notification.appLabel ?: packageName.orEmpty()
    val title = notification.title.orEmpty()
    val text = notification.text.orEmpty()
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val timeStr = sdf.format(Date(notification.timestamp))

    NotificationCard(
        data = NotificationCardData(
            appName = displayAppName,
            title = if (compact) "" else title,
            summary = text,
            timestamp = timeStr,
            count = entry.count,
        ),
        // v8.16：卡片间垂直空隙与应用分组头（AppGroupHeader）一致（上下各 2dp）
        modifier = Modifier.padding(vertical = 2.dp),
        accent = accent,
        onAccent = onAccent,
        onAccentTertiary = onAccentTertiary,
        packageName = packageName,
        variant = if (entry.count > 1) NotificationCardVariant.Multiple else NotificationCardVariant.Normal,
        onClick = { menuExpanded = true },
        onHistoryClick = onHistoryClick,
        blocked = entry.blocked,
        compact = compact,
        indent = indent,
    )

    if (menuExpanded) {
        NotificationDetailDialog(
            notification = notification,
            blocked = entry.blocked,
            showRestore = showRestore,
            onDismiss = { menuExpanded = false },
            onDelete = onDelete,
            onOpen = onOpen,
            onCreateRule = onCreateRule,
            onRestore = onRestore
        )
    }
}

// ================= v7.45：通知卡片折叠 =================
// 规则：连续收到同一个 app 的通知（packageName 一致 + 列表连续相邻），段内卡片数（聚合条目算 1 张，即 entries.size）>= 4 时折叠。
// 段内最新一条（时间倒序第一位）正常显示，其下方插入折叠卡片；点击展开后其余条目缩宽显示，
// 收起提示卡带吸顶效果（与应用分组头一致）。不修改聚合逻辑，仅在列表层做折叠。

private const val FOLD_THRESHOLD = 3
/** 折叠展开后卡片的水平缩进量（宽度略缩，与未折叠卡片区分） */
private val FoldCardIndent = 20.dp

/** 折叠分段：同一 packageName 且连续相邻的聚合条目段 */
private class FoldSegment(
    val packageName: String?,
    val appLabel: String?,
    val entries: List<NotificationHistoryEntry>
)

/** 按 packageName 连续相邻分段（输入需已按时间倒序；段内第一条即最新一条） */
private fun buildFoldSegments(entries: List<NotificationHistoryEntry>): List<FoldSegment> {
    val result = mutableListOf<FoldSegment>()
    var i = 0
    while (i < entries.size) {
        val pkg = entries[i].packageName
        var j = i
        while (j + 1 < entries.size && entries[j + 1].packageName == pkg) j++
        result.add(FoldSegment(pkg, entries[i].appLabel, entries.subList(i, j + 1)))
        i = j + 1
    }
    return result
}

/**
 * v7.47：BY_APP 组内折叠分段——按「发送时间全局相邻」判定（相邻两条之间若有其他 app 的通知时间落在其间则断开）。
 * 组内条目先按 lastTimestamp 倒序；用身份（IdentityHashMap，避免 data class equals 合并相同内容）在全局时间线
 * globalOrder 中定位每条的位置，仅当后一条位置 == 前一条位置 + 1（全局连续、中间无其他条目）时维持同段。
 * 输入需保证 groupEntries 均来自 globalOrder 所在列表；未命中的条目视为不连续。
 */
private fun buildAppFoldSegments(
    groupEntries: List<NotificationHistoryEntry>,
    globalOrder: List<NotificationHistoryEntry>
): List<FoldSegment> {
    if (groupEntries.isEmpty()) return emptyList()
    val globalPos = IdentityHashMap<NotificationHistoryEntry, Int>()
    globalOrder.forEachIndexed { idx, e -> globalPos[e] = idx }
    val sorted = groupEntries.sortedByDescending { it.lastTimestamp }
    val result = mutableListOf<FoldSegment>()
    var segStart = 0
    for (i in 1 until sorted.size) {
        val posPrev = globalPos[sorted[i - 1]]
        val posCur = globalPos[sorted[i]]
        val contiguous = posPrev != null && posCur != null && posCur == posPrev + 1
        if (!contiguous) {
            result.add(FoldSegment(sorted[segStart].packageName, sorted[segStart].appLabel, sorted.subList(segStart, i)))
            segStart = i
        }
    }
    result.add(FoldSegment(sorted[segStart].packageName, sorted[segStart].appLabel, sorted.subList(segStart, sorted.size)))
    return result
}

/** 折叠/收起提示卡：展开"xxapp"的其余 n 条通知（ExpandMore）/ 收起（ExpandLess） */
@Composable
private fun FoldToggleCard(
    appLabel: String,
    hiddenCount: Int,
    isExpanded: Boolean,
    packageName: String?,
    onClick: () -> Unit
) {
    val sp = MaterialTheme.notixSpacing
    val context = LocalContext.current
    // v8.18 优化：折叠提示卡改用同应用动态取色（品牌色半透明），与整体风格连贯
    val colors by produceState<NotificationColors?>(initialValue = null, key1 = packageName to NotificationColorEngine.colorVersion) {
        value = withContext(Dispatchers.Default) {
            NotificationColorEngine.getNotificationColors(context, packageName)
        }
    }
    val foldBg = colors?.backgroundColor?.let { Color(it).copy(alpha = 0.25f) }
        ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
    val foldFg = colors?.primaryTextColor?.let { Color(it) }
        ?: MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = NotixCorner.ListItem,
        colors = CardDefaults.cardColors(
            containerColor = foldBg
        )
    ) {
        Row(
            modifier = Modifier.padding(start = sp.lg, end = sp.md, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    if (isExpanded) R.string.fold_collapse_hint else R.string.fold_expand_hint,
                    appLabel, hiddenCount
                ),
                style = MaterialTheme.notixType.button,
                color = foldFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(if (isExpanded) R.string.collapse else R.string.expand),
                tint = foldFg
            )
        }
    }
}

/**
 * v7.45：通用折叠分段渲染（LazyListScope 扩展，三个 tab 共用）。
 * - 段 count 合计 < 4 或单条段：正常逐条渲染普通卡片；
 * - 段 count 合计 >= 4：最新一条正常显示，其下方插入折叠提示卡；
 *   展开后其余条目以缩宽卡片渲染，收起提示卡为普通 item（随滚动滚出，仅 sub_tabs 吸顶）。
 */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.foldSegments(
    segments: List<FoldSegment>,
    expandedFoldPackages: MutableState<Set<String>>,
    onToggleFold: (String) -> Unit,
    onEntryHistoryClick: (NotificationHistoryEntry) -> Unit,
    onOpenNotification: (SimpleNotification) -> Unit,
    onRestoreNotification: (SimpleNotification) -> Unit,
    onCreateRuleFromNotification: (SimpleNotification) -> Unit,
    onDeleteNotification: (SimpleNotification) -> Unit,
    context: android.content.Context,
    itemIndex: IntArray,
    listState: LazyListState,
    scope: CoroutineScope
) {
    segments.forEach { seg ->
        val pkg = seg.packageName
        val foldable = pkg != null && seg.entries.size >= FOLD_THRESHOLD
        if (!foldable) {
            // 不满足折叠条件：正常逐条渲染（含角标、时间、点击行为，与原实现一致）
            seg.entries.forEach { entry ->
                itemIndex[0]++
                item(key = entry.id) {
                    HistoryNotificationCard(
                        entry = entry,
                        onHistoryClick = { onEntryHistoryClick(entry) },
                        onOpen = { entry.latest?.let { onOpenNotification(it) } },
                        onRestore = { entry.latest?.let { onRestoreNotification(it) } },
                        onCreateRule = { entry.latest?.let { onCreateRuleFromNotification(it) } },
                        onDelete = { entry.latest?.let { onDeleteNotification(it) } },
                        showRestore = entry.blocked,
                        context = context
                    )
                }
            }
        } else {
            // v7.51：段级唯一标识——同包名但时间不连续被拆成多段时，各段独立展开/收起
            val foldKey = "${pkg}_${seg.entries.first().id}"
            val isExpanded = expandedFoldPackages.value.contains(foldKey)
            val appLabel = seg.appLabel ?: pkg
            // v8.0：折叠卡数字显示该折叠段"隐藏"的通知条数（总条数 - 1，因最新一条始终常显），展开/收起态一致
            val hiddenCount = (seg.entries.sumOf { it.count } - 1).coerceAtLeast(0)
            // 最新一条（时间倒序第一位）正常显示
            val first = seg.entries.first()
            // v7.48：段头 index（该 item 在全局 LazyColumn 中的位置），收起后列表项数变化但段头 index 不变
            val headIndex = itemIndex[0]
            itemIndex[0]++
            item(key = "${first.id}_fold_head") {
                HistoryNotificationCard(
                    entry = first,
                    onHistoryClick = { onEntryHistoryClick(first) },
                    onOpen = { first.latest?.let { onOpenNotification(it) } },
                    onRestore = { first.latest?.let { onRestoreNotification(it) } },
                    onCreateRule = { first.latest?.let { onCreateRuleFromNotification(it) } },
                    onDelete = { first.latest?.let { onDeleteNotification(it) } },
                    showRestore = first.blocked,
                    context = context
                )
            }
            if (isExpanded) {
                // 收起提示卡：stickyHeader 吸顶（浮动 tab 已由 Column 占位，吸顶位置自然位于其下方）
                itemIndex[0]++
                stickyHeader(key = "fold_toggle_${pkg}_${first.id}_expanded") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.notix.surface)
                    ) {
                        FoldToggleCard(
                            appLabel = appLabel,
                            hiddenCount = hiddenCount,
                            isExpanded = true,
                            packageName = pkg,
                            onClick = {
                                // v7.50：仅当段头已滚出视口（靠 stickyHeader 吸顶显示）时才回滚到段头；
                                // 段头仍在视口内时保持原滚动位置，避免突兀跳转
                                val headVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == headIndex }
                                onToggleFold(foldKey)
                                if (!headVisible) {
                                    scope.launch { listState.scrollToItem(headIndex) }
                                }
                            }
                        )
                    }
                }
                // 其余 n 条：普通卡片但宽度略缩（水平缩进）；v8.16 起显示标题（不再 compact 隐藏）
                seg.entries.drop(1).forEachIndexed { idx, entry ->
                    itemIndex[0]++
                    item(key = "${entry.id}_fold_body_$idx") {
                        HistoryNotificationCard(
                            entry = entry,
                            onHistoryClick = { onEntryHistoryClick(entry) },
                            onOpen = { entry.latest?.let { onOpenNotification(it) } },
                            onRestore = { entry.latest?.let { onRestoreNotification(it) } },
                            onCreateRule = { entry.latest?.let { onCreateRuleFromNotification(it) } },
                            onDelete = { entry.latest?.let { onDeleteNotification(it) } },
                            showRestore = entry.blocked,
                            context = context,
                            indent = FoldCardIndent
                        )
                    }
                }
            } else {
                // 折叠提示卡：普通 item，位于最新一条下方
                itemIndex[0]++
                item(key = "fold_toggle_${pkg}_${first.id}_collapsed") {
                    FoldToggleCard(
                        appLabel = appLabel,
                        hiddenCount = hiddenCount,
                        isExpanded = false,
                        packageName = pkg,
                        onClick = { onToggleFold(foldKey) }
                    )
                }
            }
        }
    }
}

