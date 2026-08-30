package com.enlpot.notix.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.enlpot.notix.NotificationHistoryEntry
import com.enlpot.notix.BlockerRule
import com.enlpot.notix.ui.theme.NotixCorner
import com.enlpot.notix.ui.theme.notix
import com.enlpot.notix.ui.theme.notixSpacing
import com.enlpot.notix.ui.theme.notixType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 统计页（v8.39 新增）。
 *
 * 展示通知相关的统计数据：
 * - 通知总量（今日/本周/本月）
 * - 各 app 通知数量排行
 * - 规则命中次数统计
 * - 已过滤通知数量
 *
 * 基础框架，后续可逐步完善图表和详细数据。
 */
@Composable
fun StatisticsScreen(
    historyEntries: List<NotificationHistoryEntry>,
    rules: List<BlockerRule>,
    scrollToTopTrigger: Int = 0,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

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
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(5)

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
                        appStats.forEachIndexed { index, (pkg, count) ->
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
                                    text = pkg,
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



