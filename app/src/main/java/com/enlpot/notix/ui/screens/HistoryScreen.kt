package com.enlpot.notix.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.enlpot.notix.BlockerRule
import com.enlpot.notix.NotificationBlockerService
import com.enlpot.notix.NotificationColorEngine
import com.enlpot.notix.NotificationColors
import com.enlpot.notix.NotificationHistoryEntry
import com.enlpot.notix.R
import com.enlpot.notix.SimpleNotification
import com.enlpot.notix.StatsStorage
import com.enlpot.notix.ui.components.CrashLogDialog
import com.enlpot.notix.ui.components.NotificationDetailDialog
import com.enlpot.notix.ui.components.EmptyState
import com.enlpot.notix.ui.components.RealAppIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    onResumeMonitoring: (String) -> Unit,
    onToggleListenerPaused: (Boolean) -> Unit,
    listenerPaused: Boolean,
    onClearBlockedHistory: () -> Unit = {},
    // v7.36：规则列表（Filtered tab 按规则分组依据；规则被删除后条目归「未知规则」组）
    rules: List<BlockerRule> = emptyList()
) {
    var selectedTab by remember { mutableStateOf(HistoryTab.BY_TIME) }
    var searchQuery by remember { mutableStateOf("") }
    var showStopMonitoringDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    // v7.8：通知监听铃铛二次确认对话框
    var showListenerPauseConfirm by remember { mutableStateOf(false) }
    // v7.13：长按搜索按钮打开崩溃日志弹窗
    var showCrashLogDialog by remember { mutableStateOf(false) }
    // v7.24：权限掉线弹窗中"打开系统设置"失败的应用内提示（不再使用系统 Toast）
    var openSettingsFailed by remember { mutableStateOf(false) }
    val expandedApps = remember { mutableStateOf(setOf<String>()) }
    // v7.36：Filtered tab 按规则分组的展开状态（默认收起，与按应用一致）
    val expandedRuleIds = remember { mutableStateOf(setOf<String>()) }

    // --- v7.5：列表滚动状态 / 吸顶搜索区 / 下拉刷新 / 回顶 / 权限掉线提示 ---
    val listState = rememberLazyListState()
    var searchExpanded by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showPermissionLostDialog by remember { mutableStateOf(false) }

    // 单击底部"历史"tab 回顶
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
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
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    val nowDate = LocalDate.now()
    val thisMonday = nowDate.with(DayOfWeek.MONDAY)
    val weeksBefore = 26
    val weeksAfter = 26
    val totalPages = weeksBefore + 1 + weeksAfter
    val currentWeekPage = weeksBefore
    val firstWeekStart = thisMonday.minusWeeks(weeksBefore.toLong())
    // v7.15：柱状图与今日计数/日期详情统一口径——按历史聚合组内每条 change 的时间戳归属日期统计，
    // 不再使用 StatsStorage 独立统计，避免三处数字不一致
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
    val chartPagerState = rememberPagerState(initialPage = currentWeekPage, pageCount = { totalPages })
    val coroutineScope = rememberCoroutineScope()

    // 点击顶部"通知历史"返回本周
    LaunchedEffect(backToCurrentWeekTrigger) {
        if (backToCurrentWeekTrigger > 0) {
            selectedDay = null
            coroutineScope.launch { chartPagerState.animateScrollToPage(currentWeekPage) }
        }
    }

    // --- Stop monitoring dialog ---
    showStopMonitoringDialog?.let { (packageName, appName) ->
        Dialog(onDismissRequest = { showStopMonitoringDialog = null }) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.stop_monitoring_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Text(stringResource(R.string.stop_monitoring_confirm, appName), modifier = Modifier.padding(bottom = 16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showStopMonitoringDialog = null }, modifier = Modifier.padding(end = 8.dp)) { Text(stringResource(R.string.cancel)) }
                        TextButton(onClick = { onStopMonitoring(packageName, appName); showStopMonitoringDialog = null }) { Text(stringResource(R.string.stop)) }
                    }
                }
            }
        }
    }

    // --- v7.8：通知监听暂停/恢复二次确认 ---
    if (showListenerPauseConfirm) {
        AlertDialog(
            onDismissRequest = { showListenerPauseConfirm = false },
            title = {
                Text(
                    stringResource(
                        if (listenerPaused) R.string.listener_resume_confirm_title
                        else R.string.listener_pause_confirm_title
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (listenerPaused) R.string.listener_resume_confirm_message
                        else R.string.listener_pause_confirm_message
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showListenerPauseConfirm = false
                    onToggleListenerPaused(!listenerPaused)
                }) {
                    Text(stringResource(if (listenerPaused) R.string.listener_resume else R.string.listener_pause))
                }
            },
            dismissButton = {
                TextButton(onClick = { showListenerPauseConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // --- v7.5：通知监听权限掉线引导重新授权 ---
    if (showPermissionLostDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionLostDialog = false },
            title = { Text(stringResource(R.string.listener_permission_lost_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.listener_permission_lost_message))
                    // v7.24：打开设置失败时在弹窗内展示提示（不再使用系统 Toast）
                    if (openSettingsFailed) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.open_link_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    openSettingsFailed = false
                    try {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        showPermissionLostDialog = false
                    } catch (_: Exception) {
                        openSettingsFailed = true
                    }
                }) { Text(stringResource(R.string.grant_now)) }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionLostDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // --- v7.13：长按搜索按钮打开崩溃日志弹窗（查看/开关/打开位置） ---
    if (showCrashLogDialog) {
        CrashLogDialog(onDismiss = { showCrashLogDialog = false })
    }

    // --- 过滤数据（v7.12：三个 tab 统一基于 entries，被过滤标记在 NotificationHistoryEntry.blocked） ---
    val activeEntries = when (selectedTab) {
        HistoryTab.BY_TIME -> entries
        HistoryTab.BY_APP -> entries
        HistoryTab.FILTERED -> entries.filter { it.blocked }
    }

    // v7.15：日期详情按组内任一 change 时间戳归属日过滤（避免跨天聚合组导致某日详情"未找到结果"）
    val dayFilteredEntries = remember(activeEntries, selectedDay) {
        val day = selectedDay
        if (day == null) activeEntries
        else activeEntries.filter { entry -> entry.changes.any { isSameDay(it.timestamp, day) } }
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

    val totalCount = remember(entries) { entries.sumOf { it.count } }
    // v7.36：未知规则组名（在 composable 上下文解析，供 LazyListScope 扩展使用）
    val unknownRuleLabel = stringResource(R.string.unknown_rule_group)
    // v7.15：今日计数与柱状图/日期详情统一口径——按聚合组内 change 时间戳归属今日计数
    val todayCount = remember(entries, nowDate) {
        entries.sumOf { e -> e.changes.count { isSameDay(it.timestamp, nowDate) } }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // --- v7.7 吸顶搜索区：常规与吸顶统一紧凑样式（无动态缩放），仅区分展开/收起 ---
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
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier.weight(1f),
                        compact = true
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SearchButton(
                        onClick = { searchExpanded = true },
                        onLongClick = { showCrashLogDialog = true }
                    )
                }
                SearchHeaderMode.SEARCH_EXPANDED -> Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // v7.7：改用 BasicTextField 自绘紧凑输入框，显式指定文字/光标颜色修复不可见问题
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
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
                        searchExpanded = false
                        searchQuery = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                    }
                }
            }
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
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = WindowInsets.navigationBars.asPaddingValues()
            ) {
            // v7.7：标题行作为列表项随内容滑出（"通知历史" + 通知监听铃铛）
            item(key = "history_title") {
                HistoryTitleRow(
                    listenerPaused = listenerPaused,
                    onToggleListenerPaused = { showListenerPauseConfirm = true },
                    onBackToCurrentWeek = onBackToCurrentWeek
                )
            }

            item(key = "stats_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.history_total, totalCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.history_today, todayCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(key = "chart") {
                StatsBarChart(
                    pagerState = chartPagerState,
                    firstWeekStart = firstWeekStart,
                    countsByDay = chartCounts,
                    chartMax = chartMax,
                    currentWeekStart = thisMonday,
                    selectedDay = selectedDay,
                    onDayClick = { day ->
                        selectedDay = if (selectedDay == day) null else day
                    }
                )
            }

            val filterDay = selectedDay
            if (filterDay != null) {
                item(key = "filter_day") {
                    FilteredDayRow(day = filterDay, onClear = { selectedDay = null })
                }
            }

            when (selectedTab) {
                HistoryTab.BY_TIME -> {
                    if (entries.isEmpty()) {
                        item { EmptyStateBox(Icons.Outlined.Inbox, stringResource(R.string.no_notifications_yet), stringResource(R.string.no_notifications_yet_desc)) }
                    } else if (filteredEntries.isEmpty()) {
                        item { EmptyStateBox(Icons.Outlined.SearchOff, stringResource(R.string.no_results_found), stringResource(R.string.no_results_found_desc)) }
                    } else {
                        byTimeItems(
                            entries = filteredEntries.sortedByDescending { it.lastTimestamp },
                            onEntryHistoryClick = onEntryHistoryClick,
                            onOpenNotification = onOpenNotification,
                            onRestoreNotification = onRestoreNotification,
                            onCreateRuleFromNotification = onCreateRuleFromNotification,
                            onDeleteNotification = onDeleteNotification,
                            context = context
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
                            entries = filteredEntries,
                            unmonitoredApps = unmonitoredApps,
                            expandedApps = expandedApps,
                            onEntryHistoryClick = onEntryHistoryClick,
                            onOpenNotification = onOpenNotification,
                            onRestoreNotification = onRestoreNotification,
                            onCreateRuleFromNotification = onCreateRuleFromNotification,
                            onDeleteNotification = onDeleteNotification,
                            onResumeMonitoring = onResumeMonitoring,
                            onShowStopMonitoringDialog = { showStopMonitoringDialog = it },
                            context = context
                        )
                    }
                }
                HistoryTab.FILTERED -> {
                    if (filteredBlocked.isEmpty()) {
                        item { EmptyStateBox(Icons.Outlined.Inbox, stringResource(R.string.no_notifications_yet), stringResource(R.string.no_notifications_yet_desc)) }
                    } else {
                        byRuleItems(
                            entries = filteredBlocked,
                            rules = rules,
                            expandedRuleIds = expandedRuleIds,
                            unknownGroupLabel = unknownRuleLabel,
                            onEntryHistoryClick = onEntryHistoryClick,
                            onOpenNotification = onOpenNotification,
                            onRestoreNotification = onRestoreNotification,
                            onCreateRuleFromNotification = onCreateRuleFromNotification,
                            onDeleteNotification = onDeleteNotification,
                            context = context
                        )
                    }
                }
            }
        }
        }
    }
}

private fun isSameDay(timestamp: Long, day: LocalDate): Boolean {
    return LocalDate.ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()) == day
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
    onDayClick: (LocalDate) -> Unit
) {
    val maxBarHeight = 64.dp

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
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
                        color = if (count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
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
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
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
    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(icon = icon, title = title, description = description)
    }
}

// --- v7.7：标题行（"通知历史" + 通知监听铃铛），作为列表项随内容滑出 ---
@Composable
private fun HistoryTitleRow(
    listenerPaused: Boolean,
    onToggleListenerPaused: () -> Unit,
    onBackToCurrentWeek: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onBackToCurrentWeek() }
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onToggleListenerPaused() }) {
            if (listenerPaused) {
                Icon(
                    imageVector = Icons.Default.NotificationsOff,
                    contentDescription = stringResource(R.string.listener_pause),
                    tint = MaterialTheme.colorScheme.error
                )
            } else {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = stringResource(R.string.listener_monitor),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
    val tabs = listOf(
        HistoryTab.BY_TIME to R.string.history_tab_by_time,
        HistoryTab.BY_APP to R.string.history_tab_by_app,
        HistoryTab.FILTERED to R.string.history_tab_filtered
    )
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
    ) {
        tabs.forEach { (tab, labelRes) ->
            val isSelected = selectedTab == tab
            Card(
                onClick = { onTabSelected(tab) },
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else Color.Transparent
                )
            ) {
                Text(
                    text = stringResource(labelRes),
                    modifier = Modifier.padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 5.dp else 8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = if (compact) 13.sp else 14.sp
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
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.search),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

// --- "By Time" tab --- 聚合条目列表（LazyListScope 扩展，供外层 LazyColumn 使用） ---
private fun LazyListScope.byTimeItems(
    entries: List<NotificationHistoryEntry>,
    onEntryHistoryClick: (NotificationHistoryEntry) -> Unit,
    onOpenNotification: (SimpleNotification) -> Unit,
    onRestoreNotification: (SimpleNotification) -> Unit,
    onCreateRuleFromNotification: (SimpleNotification) -> Unit,
    onDeleteNotification: (SimpleNotification) -> Unit,
    context: android.content.Context
) {
    itemsIndexed(entries, key = { _, it -> it.id }) { _, entry ->
        NotificationCard(
            entry = entry,
            onHistoryClick = { onEntryHistoryClick(entry) },
            onOpen = {
                entry.latest?.let { onOpenNotification(it) }
            },
            onRestore = {
                entry.latest?.let { onRestoreNotification(it) }
            },
            onCreateRule = {
                entry.latest?.let { onCreateRuleFromNotification(it) }
            },
            onDelete = {
                entry.latest?.let { onDeleteNotification(it) }
            },
            // v7.12：被过滤条目菜单保留「还原」
            showRestore = entry.blocked,
            context = context
        )
    }
}

// --- "By App" tab --- 按应用分组；分组标题吸顶，右侧为监控按钮 ---
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.byAppItems(
    entries: List<NotificationHistoryEntry>,
    unmonitoredApps: Set<String>,
    expandedApps: MutableState<Set<String>>,
    onEntryHistoryClick: (NotificationHistoryEntry) -> Unit,
    onOpenNotification: (SimpleNotification) -> Unit,
    onRestoreNotification: (SimpleNotification) -> Unit,
    onCreateRuleFromNotification: (SimpleNotification) -> Unit,
    onDeleteNotification: (SimpleNotification) -> Unit,
    onResumeMonitoring: (String) -> Unit,
    onShowStopMonitoringDialog: (Pair<String, String>?) -> Unit,
    context: android.content.Context
) {
    val grouped = entries.groupBy { it.appLabel ?: it.packageName.orEmpty() }
        .entries.sortedByDescending { (_, list) -> list.maxOf { it.lastTimestamp } }

    grouped.forEach { (appName, appEntries) ->
        val packageName = appEntries.firstOrNull()?.packageName
        val isExpanded = expandedApps.value.contains(appName)

        stickyHeader(key = "header_$appName") {
            AppGroupHeader(
                appName = appName,
                count = appEntries.sumOf { it.count },
                packageName = packageName,
                isExpanded = isExpanded,
                onClick = {
                    expandedApps.value = if (isExpanded) expandedApps.value - appName else expandedApps.value + appName
                },
                onStopMonitoringClick = {
                    if (packageName != null) onShowStopMonitoringDialog(packageName to appName)
                }
            )
        }

        if (isExpanded) {
            itemsIndexed(appEntries, key = { idx, e -> "${appName}_${idx}_${e.id}" }) { _, entry ->
                NotificationCard(
                    entry = entry,
                    onHistoryClick = { onEntryHistoryClick(entry) },
                    onOpen = { entry.latest?.let { onOpenNotification(it) } },
                    onRestore = { entry.latest?.let { onRestoreNotification(it) } },
                    onCreateRule = { entry.latest?.let { onCreateRuleFromNotification(it) } },
                    onDelete = { entry.latest?.let { onDeleteNotification(it) } },
                    // v7.12：被过滤条目菜单保留「还原」
                    showRestore = entry.blocked,
                    context = context,
                    compact = false
                )
            }
        }
    }

    // Unmonitored apps section
    if (unmonitoredApps.isNotEmpty()) {
        item(key = "unmonitored_header") {
            var isUnmonitoredExpanded by remember { mutableStateOf(false) }
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isUnmonitoredExpanded = !isUnmonitoredExpanded }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.unmonitored_apps, unmonitoredApps.size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { isUnmonitoredExpanded = !isUnmonitoredExpanded }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = if (isUnmonitoredExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand)
                        )
                    }
                }
                if (isUnmonitoredExpanded) {
                    val packageManager = context.packageManager
                    unmonitoredApps.forEach { pkg ->
                        val label = remember(pkg) {
                            try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() } catch (_: Exception) { pkg }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { onResumeMonitoring(pkg) }) { Text(stringResource(R.string.resume)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppGroupHeader(
    appName: String,
    count: Int,
    packageName: String?,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onStopMonitoringClick: () -> Unit,
) {
    val context = LocalContext.current
    // v7.9：NotificationColorEngine 动态配色（Icon→主色提取→HSL 背景→WCAG 文字），后台线程分析 + 缓存
    val colors by produceState<NotificationColors?>(initialValue = null, key1 = packageName) {
        value = withContext(Dispatchers.Default) {
            NotificationColorEngine.getNotificationColors(context, packageName)
        }
    }
    val headerBg = colors?.backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    // v7.9：文字颜色由引擎按实际对比度选择（白/黑），加载完成前用主题 onSurface
    val headerFg = colors?.primaryTextColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    // v7.9：强调色（左侧色条/角标底）由引擎生成
    val fallbackAccent = MaterialTheme.colorScheme.primary
    val accent = colors?.accentColor?.let { Color(it) } ?: fallbackAccent
    // v7.9：角标文字复用引擎对比度逻辑（对 accent 实际对比度选黑/白），禁止硬编码
    val accentFg = remember(accent) { Color(NotificationColorEngine.chooseTextColor(accent.toArgb())) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = headerBg
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // v7.8：左侧主色深色版装饰条
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(11.dp))
            RealAppIcon(
                packageName = packageName,
                appName = appName,
                size = 28.dp,
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = headerFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // v7.8：变更计数角标——主色深色版底 + 白色文字
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
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

            Spacer(modifier = Modifier.width(4.dp))

            // 监控按钮：放到分组卡片右侧（标题行右侧）
            IconButton(onClick = onStopMonitoringClick) {
                Icon(
                    imageVector = Icons.Default.NotificationsOff,
                    contentDescription = stringResource(R.string.stop_monitoring_short),
                    tint = headerFg.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// --- "Filtered" tab --- 按规则分组（v7.36）：组头右侧无操作按钮（仅按应用 tab 保留停止监控）
private fun LazyListScope.byRuleItems(
    entries: List<NotificationHistoryEntry>,
    rules: List<BlockerRule>,
    expandedRuleIds: MutableState<Set<String>>,
    unknownGroupLabel: String,
    onEntryHistoryClick: (NotificationHistoryEntry) -> Unit,
    onOpenNotification: (SimpleNotification) -> Unit,
    onRestoreNotification: (SimpleNotification) -> Unit,
    onCreateRuleFromNotification: (SimpleNotification) -> Unit,
    onDeleteNotification: (SimpleNotification) -> Unit,
    context: android.content.Context
) {
    val ruleById = rules.associateBy { it.id }

    // 每条被过滤条目取其最新一条变更的命中规则 id；规则已删除/旧数据无记录则归「未知规则」组
    val grouped = entries.groupBy { entry ->
        entry.latest?.matchedRuleIds?.firstOrNull()
            ?: entry.changes.firstOrNull()?.matchedRuleIds?.firstOrNull()
    }

    // 组间按组内最新时间倒序；未知规则组固定排最后
    val sortedGroups = grouped.entries.sortedWith(
        compareByDescending<Map.Entry<String?, List<NotificationHistoryEntry>>> { (_, list) -> list.maxOf { it.lastTimestamp } }
            .thenByDescending { it.key != null }
    )

    sortedGroups.forEach { (ruleId, groupEntries) ->
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

        stickyHeader(key = "rule_header_$groupKey") {
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

        if (isExpanded) {
            itemsIndexed(groupEntries, key = { idx, e -> "${groupKey}_${idx}_${e.id}" }) { _, entry ->
                NotificationCard(
                    entry = entry,
                    onHistoryClick = { onEntryHistoryClick(entry) },
                    onOpen = { entry.latest?.let { onOpenNotification(it) } },
                    onRestore = { entry.latest?.let { onRestoreNotification(it) } },
                    onCreateRule = { entry.latest?.let { onCreateRuleFromNotification(it) } },
                    onDelete = { entry.latest?.let { onDeleteNotification(it) } },
                    showRestore = true,
                    context = context,
                    compact = false
                )
            }
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
    val context = LocalContext.current
    // 未知规则组固定默认灰色配色；其余复用 NotificationColorEngine 动态配色（与按应用一致）
    val colors by produceState<NotificationColors?>(initialValue = null, key1 = if (isUnknown) null else packageName) {
        value = withContext(Dispatchers.Default) {
            if (isUnknown) null
            else NotificationColorEngine.getNotificationColors(context, packageName)
        }
    }
    val headerBg = colors?.backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val headerFg = colors?.primaryTextColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    val fallbackAccent = if (isUnknown) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
    val accent = colors?.accentColor?.let { Color(it) } ?: fallbackAccent
    val accentFg = remember(accent) { Color(NotificationColorEngine.chooseTextColor(accent.toArgb())) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = headerBg
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(11.dp))
            RealAppIcon(
                packageName = packageName,
                appName = title,
                size = 28.dp,
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = headerFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
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
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

// --- 聚合通知卡片：单击弹菜单；右侧徽标点击打开变更历史 ---
@Composable
private fun NotificationCard(
    entry: NotificationHistoryEntry,
    onHistoryClick: () -> Unit,
    onOpen: () -> Unit,
    onRestore: () -> Unit,
    onCreateRule: () -> Unit,
    onDelete: () -> Unit,
    showRestore: Boolean,
    context: android.content.Context,
    compact: Boolean = false
) {
    val notification = entry.latest ?: return
    var menuExpanded by remember { mutableStateOf(false) }
    val packageName = notification.packageName

    // v7.10：复用 NotificationColorEngine 配色（accent 底 + 对比度文字色）
    val colors by produceState<NotificationColors?>(initialValue = null, key1 = packageName) {
        value = withContext(Dispatchers.Default) {
            NotificationColorEngine.getNotificationColors(context, packageName)
        }
    }
    val accent = colors?.accentColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val accentFg = remember(accent) { Color(NotificationColorEngine.chooseTextColor(accent.toArgb())) }
    // v7.14：已过滤标签使用 error 实底 + 对比度文字色（与变更计数角标一致）
    val errorColor = MaterialTheme.colorScheme.error
    val errorFg = remember(errorColor) { Color(NotificationColorEngine.chooseTextColor(errorColor.toArgb())) }

    val displayAppName = notification.appLabel ?: packageName.orEmpty()
    val title = notification.title.orEmpty()
    val text = notification.text.orEmpty()
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val timeStr = sdf.format(Date(notification.timestamp))

    Box {
        Card(
            onClick = { menuExpanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                RealAppIcon(
                    packageName = packageName,
                    appName = displayAppName,
                    size = 36.dp,
                    shape = RoundedCornerShape(8.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayAppName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (title.isNotEmpty() && !compact) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (text.isNotEmpty()) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (compact) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // v7.10：变更次数角标——多条（count>1）时显示「数字 + 下拉三角」，点击打开历史变更窗口；单条不显示
                if (entry.count > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent)
                            .clickable { onHistoryClick() }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.count.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentFg
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(R.string.open_history),
                                tint = accentFg
                            )
                        }
                    }
                }
            }
        }

        // v7.15：已过滤标签——固定卡片右下角（BottomEnd），右边缘与变更计数角标右侧竖直线对齐（end=12.dp 同卡片内边距）
        if (entry.blocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(errorColor)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = errorFg,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = stringResource(R.string.history_blocked_badge),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = errorFg
                    )
                }
            }
        }

        // v7.35：抽取为可复用组件 NotificationDetailDialog（历史列表与聚合窗口共用）
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
}
