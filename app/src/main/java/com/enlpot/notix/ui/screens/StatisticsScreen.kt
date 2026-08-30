package com.enlpot.notix.ui.screens

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enlpot.notix.NotificationHistoryEntry
import com.enlpot.notix.BlockerRule
import com.enlpot.notix.ui.theme.NotixCorner
import com.enlpot.notix.ui.theme.notix
import com.enlpot.notix.ui.theme.notixSpacing
import com.enlpot.notix.ui.theme.notixType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 统计页（v8.39 新增，v8.40 增强）。
 *
 * 展示通知相关的统计数据：
 * - 通知总量（今日/本周/本月）
 * - 通知趋势折线图（近7天）
 * - 24小时分布热力图（近7天）
 * - 各 app 通知数量排行
 * - 规则命中次数统计
 * - 已过滤通知数量
 */
@Composable
fun StatisticsScreen(
    historyEntries: List<NotificationHistoryEntry>,
    rules: List<BlockerRule>,
    scrollToTopTrigger: Int = 0,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var selectedHeatmapCell by remember { mutableStateOf<Pair<LocalDate, Int>?>(null) }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
        }
    }

    // 计算统计数据
    val today = LocalDate.now()
    val todayCount = historyEntries.count { entry ->
        entry.lastTimestamp?.let {
            LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) == today
        } ?: false
    }
    val weekCount = historyEntries.count { entry ->
        entry.lastTimestamp?.let {
            val date = LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            !date.isBefore(today.minusDays(6))
        } ?: false
    }
    val monthCount = historyEntries.count { entry ->
        entry.lastTimestamp?.let {
            val date = LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            date.month == today.month && date.year == today.year
        } ?: false
    }
    val filteredCount = historyEntries.count { it.blocked }
    val totalHits = rules.sumOf { it.hitCount }

    // 各 app 通知数量排行（前5）
    val appStats = historyEntries
        .filter { it.packageName != null }
        .groupBy { it.packageName!! }
        .map { (pkg, entries) ->
            Triple(pkg, entries.firstOrNull()?.appLabel, entries.size)
        }
        .sortedByDescending { it.third }
        .take(5)

    // 通知健康度评分
    val healthScore = remember(historyEntries) {
        calculateHealthScore(historyEntries)
    }
    val healthStats = remember(historyEntries) {
        calculateHealthStats(historyEntries)
    }

    // 改进建议
    val suggestions = remember(historyEntries, rules) {
        generateSuggestions(historyEntries, rules)
    }

    // App环形图数据（前6个app，其余归为"其他"）
    val pieData = remember(historyEntries) {
        val appCounts = historyEntries
            .filter { it.packageName != null }
            .groupBy { it.packageName!! }
            .map { (pkg, entries) ->
                Triple(pkg, entries.firstOrNull()?.appLabel ?: pkg, entries.size)
            }
            .sortedByDescending { it.third }
        val top = appCounts.take(5)
        val othersCount = appCounts.drop(5).sumOf { it.third }
        val result = top.toMutableList()
        if (othersCount > 0) {
            result.add(Triple("other", "其他", othersCount))
        }
        result
    }
    val pieTotal = pieData.sumOf { it.third }

    // 规则效果排行数据（按命中次数降序，前5）
    val ruleRanking = remember(rules) {
        rules
            .filter { it.hitCount > 0 }
            .sortedByDescending { it.hitCount }
            .take(5)
    }
    val maxRuleHits = ruleRanking.maxOfOrNull { it.hitCount } ?: 1

    // 通知趋势数据（近7天）
    val trendData = remember(historyEntries) {
        calculateDailyTrend(historyEntries, 7)
    }

    // 24小时热力图数据（近7天）
    val heatmapData = remember(historyEntries) {
        calculateHourlyHeatmap(historyEntries, 7)
    }
    val heatmapMax = remember(heatmapData) {
        heatmapData.values.flatMap { it.values }.maxOrNull() ?: 0
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.notixSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.notixSpacing.md),
    ) {
        // 页面标题
        item {
            Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
            Text(
                text = "统计",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.notix.contentPrimary,
            )
            Spacer(Modifier.height(MaterialTheme.notixSpacing.xs))
            Text(
                text = "通知数据概览",
                style = MaterialTheme.notixType.body,
                color = MaterialTheme.notix.contentSecondary,
            )
        }

        // 通知总量卡片
        item {
            StatCard(
                title = "通知总量",
                icon = Icons.Default.Notifications,
                items = listOf(
                    "今日" to todayCount.toString(),
                    "本周" to weekCount.toString(),
                    "本月" to monthCount.toString(),
                ),
            )
        }

        // 过滤与规则命中卡片
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.notixSpacing.md),
            ) {
                StatCard(
                    title = "已过滤",
                    icon = Icons.Default.FilterAlt,
                    items = listOf("总数" to filteredCount.toString()),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "规则命中",
                    icon = Icons.Default.Rule,
                    items = listOf("总次数" to totalHits.toString()),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 通知健康度评分
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = NotixCorner.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.notix.surfaceElevated),
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.notixSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = healthScoreColor(healthScore),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "通知健康度",
                            style = MaterialTheme.notixType.body,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.notix.contentPrimary,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = healthScoreLabel(healthScore),
                            style = MaterialTheme.notixType.caption,
                            fontWeight = FontWeight.Bold,
                            color = healthScoreColor(healthScore),
                        )
                    }
                    Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
                        val ringBgColor = MaterialTheme.notix.surfaceVariant
                        val ringFgColor = healthScoreColor(healthScore)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 环形进度条
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 10f
                                val radius = (size.minDimension - strokeWidth) / 2f
                                val center = Offset(size.width / 2f, size.height / 2f)
                                // 背景环
                                drawCircle(
                                    color = ringBgColor,
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = strokeWidth),
                                )
                                // 进度环
                                drawArc(
                                    color = ringFgColor,
                                    startAngle = -90f,
                                    sweepAngle = (healthScore / 100f) * 360f,
                                    useCenter = false,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                                    style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = healthScore.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = healthScoreColor(healthScore),
                                )
                                Text(
                                    text = "分",
                                    style = MaterialTheme.notixType.caption,
                                    color = MaterialTheme.notix.contentSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.width(MaterialTheme.notixSpacing.md))
                        // 评分详情
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            HealthDetailRow(
                                label = "7日平均通知量",
                                value = "${healthStats.avgDailyCount}条",
                                score = healthStats.dailyCountScore,
                            )
                            HealthDetailRow(
                                label = "通知来源分散度",
                                value = "${healthStats.appCount}个app",
                                score = healthStats.diversityScore,
                            )
                            HealthDetailRow(
                                label = "规则过滤效率",
                                value = "${healthStats.filterRate}%",
                                score = healthStats.filterScore,
                            )
                        }
                    }
                }
            }
        }

        // 改进建议
        if (suggestions.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = NotixCorner.Card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.notix.surfaceElevated),
                ) {
                    Column(modifier = Modifier.padding(MaterialTheme.notixSpacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.TipsAndUpdates,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.notix.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "改进建议",
                                style = MaterialTheme.notixType.body,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.notix.contentPrimary,
                            )
                        }
                        Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
                        suggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = suggestion.icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp),
                                    tint = suggestion.color,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = suggestion.text,
                                    style = MaterialTheme.notixType.caption,
                                    color = MaterialTheme.notix.contentSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 通知趋势折线图
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = NotixCorner.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.notix.surfaceElevated),
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.notixSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.notix.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "通知趋势（近7天）",
                            style = MaterialTheme.notixType.body,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.notix.contentPrimary,
                        )
                    }
                    Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
                    TrendLineChart(
                        data = trendData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                    )
                }
            }
        }

        // 24小时热力图
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = NotixCorner.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.notix.surfaceElevated),
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.notixSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.notix.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "24小时分布（近7天）",
                            style = MaterialTheme.notixType.body,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.notix.contentPrimary,
                        )
                        Spacer(Modifier.weight(1f))
                        if (selectedHeatmapCell != null) {
                            val (date, hour) = selectedHeatmapCell!!
                            val count = heatmapData[date]?.get(hour) ?: 0
                            Text(
                                text = "${date.format(DateTimeFormatter.ofPattern("MM/dd"))} ${hour}:00 · ${count}条",
                                style = MaterialTheme.notixType.caption,
                                color = MaterialTheme.notix.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
                    HourHeatmap(
                        data = heatmapData,
                        maxCount = heatmapMax,
                        onCellClick = { date, hour ->
                            selectedHeatmapCell = if (selectedHeatmapCell == date to hour) null else date to hour
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // App通知占比环形图
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = NotixCorner.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.notix.surfaceElevated),
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.notixSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.notix.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "通知来源占比",
                            style = MaterialTheme.notixType.body,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.notix.contentPrimary,
                        )
                    }
                    Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
                    if (pieData.isEmpty() || pieTotal == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "暂无数据",
                                style = MaterialTheme.notixType.caption,
                                color = MaterialTheme.notix.contentTertiary,
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 环形图
                            DonutChart(
                                data = pieData,
                                total = pieTotal,
                                modifier = Modifier.size(140.dp),
                            )
                            Spacer(Modifier.width(MaterialTheme.notixSpacing.md))
                            // 图例
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                pieData.forEachIndexed { index, (_, label, count) ->
                                    val percent = if (pieTotal > 0) (count * 100 / pieTotal) else 0
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    color = donutColors[index % donutColors.size],
                                                    shape = RoundedCornerShape(2.dp),
                                                ),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.notixType.caption,
                                            color = MaterialTheme.notix.contentSecondary,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                        )
                                        Text(
                                            text = "${percent}%",
                                            style = MaterialTheme.notixType.caption,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.notix.contentPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 规则效果排行
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = NotixCorner.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.notix.surfaceElevated),
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.notixSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.notix.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "规则效果排行（前5）",
                            style = MaterialTheme.notixType.body,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.notix.contentPrimary,
                        )
                    }
                    Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
                    if (ruleRanking.isEmpty()) {
                        Text(
                            text = "暂无规则命中数据",
                            style = MaterialTheme.notixType.caption,
                            color = MaterialTheme.notix.contentTertiary,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        ruleRanking.forEachIndexed { index, rule ->
                            val percent = if (maxRuleHits > 0) (rule.hitCount * 100 / maxRuleHits) else 0
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.notixType.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.notix.primary,
                                        modifier = Modifier.width(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = rule.name?.ifEmpty { "未命名规则" } ?: "未命名规则",
                                        style = MaterialTheme.notixType.caption,
                                        color = MaterialTheme.notix.contentSecondary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = "${rule.hitCount}次",
                                        style = MaterialTheme.notixType.caption,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.notix.contentPrimary,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                // 进度条
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .background(
                                            color = MaterialTheme.notix.surfaceVariant,
                                            shape = RoundedCornerShape(3.dp),
                                        ),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(percent / 100f)
                                            .height(6.dp)
                                            .background(
                                                color = MaterialTheme.notix.primary.copy(alpha = 0.7f),
                                                shape = RoundedCornerShape(3.dp),
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 各 app 通知排行
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = NotixCorner.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.notix.surfaceElevated),
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.notixSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.notix.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "App 通知排行（前5）",
                            style = MaterialTheme.notixType.body,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.notix.contentPrimary,
                        )
                    }
                    Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
                    if (appStats.isEmpty()) {
                        Text(
                            text = "暂无数据",
                            style = MaterialTheme.notixType.caption,
                            color = MaterialTheme.notix.contentTertiary,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        appStats.forEachIndexed { index, (pkg, label, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.notixType.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.notix.primary,
                                    modifier = Modifier.width(20.dp),
                                )
                                Text(
                                    text = label ?: pkg,
                                    style = MaterialTheme.notixType.caption,
                                    color = MaterialTheme.notix.contentSecondary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                )
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.notixType.caption,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.notix.contentPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 底部留白
        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}

/**
 * 计算每日通知趋势数据。
 */
private fun calculateDailyTrend(
    entries: List<NotificationHistoryEntry>,
    days: Int,
): List<Pair<LocalDate, Int>> {
    val today = LocalDate.now()
    val counts = mutableMapOf<LocalDate, Int>()
    for (i in 0 until days) {
        counts[today.minusDays(i.toLong())] = 0
    }
    entries.forEach { entry ->
        entry.lastTimestamp?.let { ts ->
            val date = LocalDate.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
            if (counts.containsKey(date)) {
                counts[date] = counts[date]!! + 1
            }
        }
    }
    return counts.toList().sortedBy { it.first }
}

/**
 * 计算24小时热力图数据（近N天 × 24小时）。
 */
private fun calculateHourlyHeatmap(
    entries: List<NotificationHistoryEntry>,
    days: Int,
): Map<LocalDate, Map<Int, Int>> {
    val today = LocalDate.now()
    val result = mutableMapOf<LocalDate, MutableMap<Int, Int>>()
    for (i in 0 until days) {
        val date = today.minusDays(i.toLong())
        result[date] = mutableMapOf()
        for (h in 0..23) {
            result[date]!![h] = 0
        }
    }
    entries.forEach { entry ->
        entry.lastTimestamp?.let { ts ->
            val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
            val date = dateTime.toLocalDate()
            val hour = dateTime.hour
            if (result.containsKey(date)) {
                result[date]!![hour] = result[date]!![hour]!! + 1
            }
        }
    }
    return result
}

/**
 * 通知趋势折线图组件。
 * Canvas 绘制折线/填充/数据点，Compose 布局绘制坐标轴标签。
 */
@Composable
private fun TrendLineChart(
    data: List<Pair<LocalDate, Int>>,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.notix.primary
    val gridColor = MaterialTheme.notix.contentTertiary.copy(alpha = 0.3f)

    if (data.isEmpty() || data.all { it.second == 0 }) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.notixType.caption,
                color = MaterialTheme.notix.contentTertiary,
            )
        }
        return
    }

    val maxValue = data.maxOf { it.second }.coerceAtLeast(1)
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")

    Column(modifier = modifier) {
        // 图表区域：Y轴标签 + Canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            // Y轴标签
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                (4 downTo 0).forEach { i ->
                    val value = maxValue * i / 4
                    Text(
                        text = value.toInt().toString(),
                        style = MaterialTheme.notixType.caption,
                        fontSize = 9.sp,
                        color = MaterialTheme.notix.contentSecondary,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            // Canvas 绘制折线、填充、网格线、数据点
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                val chartLeft = 0f
                val chartRight = size.width
                val chartTop = 8f
                val chartBottom = size.height - 8f
                val chartWidth = chartRight - chartLeft
                val chartHeight = chartBottom - chartTop

                // 绘制水平网格线（4条）
                for (i in 0..4) {
                    val y = chartTop + chartHeight * i / 4
                    drawLine(
                        color = gridColor,
                        start = Offset(chartLeft, y),
                        end = Offset(chartRight, y),
                        strokeWidth = 1f,
                    )
                }

                // 计算数据点位置
                val points = data.mapIndexed { index, (_, count) ->
                    val x = chartLeft + chartWidth * index / (data.size - 1).coerceAtLeast(1)
                    val y = chartBottom - chartHeight * count / maxValue
                    Offset(x, y)
                }

                // 绘制渐变填充区域
                if (points.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(points.first().x, chartBottom)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, chartBottom)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f),
                                primaryColor.copy(alpha = 0.02f),
                            ),
                            startY = chartTop,
                            endY = chartBottom,
                        ),
                    )
                }

                // 绘制折线
                if (points.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(width = 3f),
                    )
                }

                // 绘制数据点
                points.forEach { point ->
                    drawCircle(
                        color = primaryColor,
                        radius = 5f,
                        center = point,
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f,
                        center = point,
                    )
                }
            }
        }
        // X轴日期标签
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            data.forEach { (date, _) ->
                Text(
                    text = date.format(dateFormatter),
                    style = MaterialTheme.notixType.caption,
                    fontSize = 9.sp,
                    color = MaterialTheme.notix.contentSecondary,
                )
            }
        }
    }
}

/**
 * 24小时热力图组件。
 */
@Composable
private fun HourHeatmap(
    data: Map<LocalDate, Map<Int, Int>>,
    maxCount: Int,
    onCellClick: (LocalDate, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.notix.primary
    val dates = data.keys.sorted()
    val hours = (0..23).toList()
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")

    if (dates.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.notixType.caption,
                color = MaterialTheme.notix.contentTertiary,
            )
        }
        return
    }

    Column(modifier = modifier) {
        // 小时标签行
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 42.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf(0, 6, 12, 18, 23).forEach { hour ->
                Text(
                    text = "${hour}时",
                    style = MaterialTheme.notixType.caption,
                    fontSize = 9.sp,
                    color = MaterialTheme.notix.contentTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(28.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // 热力图网格
        dates.forEach { date ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 日期标签
                Text(
                    text = date.format(dateFormatter),
                    style = MaterialTheme.notixType.caption,
                    fontSize = 9.sp,
                    color = MaterialTheme.notix.contentSecondary,
                    modifier = Modifier.width(38.dp),
                )
                Spacer(Modifier.width(4.dp))
                // 24个小时格子
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    hours.forEach { hour ->
                        val count = data[date]?.get(hour) ?: 0
                        val alpha = if (maxCount > 0) {
                            0.12f + (count.toFloat() / maxCount) * 0.88f
                        } else 0.12f
                        val cellColor = if (count > 0) {
                            primaryColor.copy(alpha = alpha)
                        } else {
                            MaterialTheme.notix.surfaceVariant.copy(alpha = 0.5f)
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = cellColor,
                                    shape = RoundedCornerShape(2.dp),
                                )
                                .clickable { onCellClick(date, hour) },
                        )
                    }
                }
            }
        }

        // 图例
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "少",
                style = MaterialTheme.notixType.caption,
                fontSize = 9.sp,
                color = MaterialTheme.notix.contentTertiary,
            )
            Spacer(Modifier.width(4.dp))
            (0..4).forEach { i ->
                val alpha = 0.12f + (i / 4f) * 0.88f
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = primaryColor.copy(alpha = alpha),
                            shape = RoundedCornerShape(2.dp),
                        ),
                )
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text = "多",
                style = MaterialTheme.notixType.caption,
                fontSize = 9.sp,
                color = MaterialTheme.notix.contentTertiary,
            )
        }
    }
}


/**
 * 环形图颜色列表。
 */
private val donutColors = listOf(
    Color(0xFF6366F1),  // 靛蓝
    Color(0xFFEC4899),  // 粉红
    Color(0xFF10B981),  // 翠绿
    Color(0xFFF59E0B),  // 琥珀
    Color(0xFF3B82F6),  // 蓝色
    Color(0xFF8B5CF6),  // 紫色
    Color(0xFFEF4444),  // 红色
    Color(0xFF14B8A6),  // 青色
)

/**
 * App通知占比环形图组件。
 */
@Composable
private fun DonutChart(
    data: List<Triple<String, String, Int>>,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.notix.primary

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.25f
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            var startAngle = -90f

            data.forEachIndexed { index, (_, _, count) ->
                if (count <= 0 || total <= 0) return@forEachIndexed
                val sweepAngle = (count.toFloat() / total) * 360f
                drawArc(
                    color = donutColors[index % donutColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth),
                )
                startAngle += sweepAngle
            }
        }
        // 中间显示总数
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = total.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
            )
            Text(
                text = "总通知",
                style = MaterialTheme.notixType.caption,
                color = MaterialTheme.notix.contentSecondary,
            )
        }
    }
}


/**
 * 健康度统计数据。
 */
private data class HealthStats(
    val avgDailyCount: Int,
    val appCount: Int,
    val filterRate: Int,
    val dailyCountScore: Int,
    val diversityScore: Int,
    val filterScore: Int,
)

/**
 * 改进建议。
 */
private data class Suggestion(
    val text: String,
    val icon: ImageVector,
    val color: Color,
)

/**
 * 计算通知健康度评分（0-100）。
 */
private fun calculateHealthScore(entries: List<NotificationHistoryEntry>): Int {
    if (entries.isEmpty()) return 85  // 无数据时默认良好

    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()

    // 7日平均通知量
    val last7Days = entries.filter { entry ->
        entry.lastTimestamp?.let {
            val date = LocalDate.ofInstant(Instant.ofEpochMilli(it), zone)
            !date.isBefore(today.minusDays(6))
        } ?: false
    }
    val avgDailyCount = last7Days.size / 7

    // 通知来源数量
    val appCount = entries.mapNotNull { it.packageName }.distinct().size

    // 过滤比例
    val filteredCount = entries.count { it.blocked }
    val filterRate = if (entries.isNotEmpty()) (filteredCount * 100 / entries.size) else 0

    // 评分计算
    // 通知量分（40分）：日均<=10得满分，每多5条扣5分，最低0分
    val dailyCountScore = (40 - ((avgDailyCount - 10) / 5) * 5).coerceIn(0, 40)
    // 分散度分（30分）：app>=10得满分，每少1个扣3分
    val diversityScore = (30 - (10 - appCount) * 3).coerceIn(0, 30)
    // 过滤效率分（30分）：过滤率在10%-40%得满分，过高或过低扣分
    val filterScore = when {
        filterRate in 10..40 -> 30
        filterRate < 10 -> 30 - (10 - filterRate) * 2
        else -> 30 - (filterRate - 40) / 2
    }.coerceIn(0, 30)

    return dailyCountScore + diversityScore + filterScore
}

/**
 * 计算健康度统计详情。
 */
private fun calculateHealthStats(entries: List<NotificationHistoryEntry>): HealthStats {
    if (entries.isEmpty()) return HealthStats(0, 0, 0, 0, 0, 0)

    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()

    val last7Days = entries.filter { entry ->
        entry.lastTimestamp?.let {
            val date = LocalDate.ofInstant(Instant.ofEpochMilli(it), zone)
            !date.isBefore(today.minusDays(6))
        } ?: false
    }
    val avgDailyCount = last7Days.size / 7
    val appCount = entries.mapNotNull { it.packageName }.distinct().size
    val filteredCount = entries.count { it.blocked }
    val filterRate = if (entries.isNotEmpty()) (filteredCount * 100 / entries.size) else 0

    val dailyCountScore = (40 - ((avgDailyCount - 10) / 5) * 5).coerceIn(0, 40)
    val diversityScore = (30 - (10 - appCount) * 3).coerceIn(0, 30)
    val filterScore = when {
        filterRate in 10..40 -> 30
        filterRate < 10 -> 30 - (10 - filterRate) * 2
        else -> 30 - (filterRate - 40) / 2
    }.coerceIn(0, 30)

    return HealthStats(avgDailyCount, appCount, filterRate, dailyCountScore, diversityScore, filterScore)
}

/**
 * 健康度评分颜色。
 */
private fun healthScoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF10B981)  // 绿色-优秀
    score >= 60 -> Color(0xFFF59E0B)  // 黄色-良好
    score >= 40 -> Color(0xFFF97316)  // 橙色-一般
    else -> Color(0xFFEF4444)  // 红色-较差
}

/**
 * 健康度评分标签。
 */
private fun healthScoreLabel(score: Int): String = when {
    score >= 80 -> "优秀"
    score >= 60 -> "良好"
    score >= 40 -> "一般"
    else -> "较差"
}

/**
 * 生成改进建议。
 */
private fun generateSuggestions(
    entries: List<NotificationHistoryEntry>,
    rules: List<BlockerRule>,
): List<Suggestion> {
    val suggestions = mutableListOf<Suggestion>()
    if (entries.isEmpty()) return suggestions

    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()

    // 7日平均通知量
    val last7Days = entries.filter { entry ->
        entry.lastTimestamp?.let {
            val date = LocalDate.ofInstant(Instant.ofEpochMilli(it), zone)
            !date.isBefore(today.minusDays(6))
        } ?: false
    }
    val avgDailyCount = last7Days.size / 7

    // 通知最多的app
    val topApp = entries
        .filter { it.packageName != null }
        .groupBy { it.packageName!! }
        .mapValues { it.value.size }
        .maxByOrNull { it.value }

    // 常驻通知数量
    val ongoingCount = 0

    // 建议1：通知量过高
    if (avgDailyCount > 20) {
        suggestions.add(
            Suggestion(
                text = "近7日日均通知${avgDailyCount}条，通知量偏多，建议检查是否有app频繁发送无用通知",
                icon = Icons.Default.Warning,
                color = Color(0xFFF59E0B),
            )
        )
    }

    // 建议2：某个app通知占比过高
    if (topApp != null && entries.isNotEmpty()) {
        val topRatio = topApp.value * 100 / entries.size
        if (topRatio > 50) {
            val appName = entries.firstOrNull { it.packageName == topApp.key }?.appLabel ?: topApp.key
            suggestions.add(
                Suggestion(
                    text = "$appName 通知占比${topRatio}%，过于集中，建议为该app创建过滤规则",
                    icon = Icons.Default.Warning,
                    color = Color(0xFFF97316),
                )
            )
        }
    }

    // 建议3：规则较少或命中率低
    val activeRules = rules.count { it.isEnabled }
    val totalHits = rules.sumOf { it.hitCount }
    if (activeRules == 0) {
        suggestions.add(
            Suggestion(
                text = "尚未创建任何过滤规则，建议创建规则自动过滤无用通知",
                icon = Icons.Default.TipsAndUpdates,
                color = Color(0xFF3B82F6),
            )
        )
    } else if (totalHits < 5 && entries.size > 20) {
        suggestions.add(
            Suggestion(
                text = "规则命中率较低，建议优化规则条件或增加更多规则",
                icon = Icons.Default.TipsAndUpdates,
                color = Color(0xFF3B82F6),
            )
        )
    }

    // 建议4：常驻通知较多
    if (false) {
        suggestions.add(
            Suggestion(
                text = "常驻通知有${ongoingCount}条，建议清理不需要的常驻通知",
                icon = Icons.Default.Warning,
                color = Color(0xFFF59E0B),
            )
        )
    }

    // 建议5：通知管理良好
    if (suggestions.isEmpty() && avgDailyCount in 5..20 && activeRules > 0) {
        suggestions.add(
            Suggestion(
                text = "通知管理良好，继续保持！",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF10B981),
            )
        )
    }

    return suggestions.take(4)
}

/**
 * 健康度详情行组件。
 */
@Composable
private fun HealthDetailRow(
    label: String,
    value: String,
    score: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.notixType.caption,
            color = MaterialTheme.notix.contentSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.notixType.caption,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.notix.contentPrimary,
        )
        Spacer(Modifier.width(8.dp))
        // 小进度条
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(
                    color = MaterialTheme.notix.surfaceVariant,
                    shape = RoundedCornerShape(2.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score / 40f)
                    .height(4.dp)
                    .background(
                        color = when {
                            score >= 30 -> Color(0xFF10B981)
                            score >= 20 -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        },
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

/**
 * 统计卡片组件。
 */
@Composable
private fun StatCard(
    title: String,
    icon: ImageVector,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = NotixCorner.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.notix.surfaceElevated),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.notixSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.notix.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.notixType.body,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.notix.contentPrimary,
                )
            }
            Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                items.forEach { (label, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.notix.primary,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.notixType.caption,
                            color = MaterialTheme.notix.contentSecondary,
                        )
                    }
                }
            }
        }
    }
}







