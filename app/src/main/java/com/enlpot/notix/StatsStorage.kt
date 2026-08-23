package com.enlpot.notix

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 通知统计存储。
 *
 * 保留原有 blocked 计数；新增按天通知量统计，供历史 tab 柱状图使用。
 * 按天计数以 "yyyy-MM-dd" 为 key 存于 SharedPreferences，
 * 周统计由日计数按周一~周日聚合得出（跨周按自然日归属，如 8.2、8.10）。
 */
class StatsStorage(private val context: Context) {

    companion object {
        private const val TAG = "StatsStorage"
        private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        /** 最多保留 400 天，防止无限增长。 */
        private const val MAX_DAYS = 400
    }

    private val prefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE)
    private val blockedCountKey = "blocked_count"
    private val dailyPrefix = "day_"
    /** v8.0：读改写加锁，避免高频命中时并发 getInt+putInt 丢计数 */
    private val lock = Any()

    fun getBlockedNotificationsCount(): Int {
        return prefs.getInt(blockedCountKey, 0)
    }

    fun incrementBlockedNotificationsCount() {
        synchronized(lock) {
            val currentCount = getBlockedNotificationsCount()
            prefs.edit().putInt(blockedCountKey, currentCount + 1).apply()
        }
    }

    /** 记录一条通知（按通知到达时间归属当天）。 */
    fun recordNotification(timestamp: Long = System.currentTimeMillis()) {
        val day = LocalDate.ofEpochDay(timestamp / 86_400_000L).format(DAY_FORMAT)
        val key = dailyPrefix + day
        synchronized(lock) {
            val current = prefs.getInt(key, 0)
            prefs.edit().putInt(key, current + 1).apply()
        }
        trimOldDays()
    }

    /** 某天的通知量。 */
    fun getCountForDay(date: LocalDate): Int {
        return prefs.getInt(dailyPrefix + date.format(DAY_FORMAT), 0)
    }

    /**
     * 以 [weekStart]（周一）为起点的整周（周一~周日）每日通知量。
     * 返回 7 个 (日期, 数量)，用于柱状图。
     */
    fun getWeekCounts(weekStart: LocalDate): List<Pair<LocalDate, Int>> {
        return (0 until 7).map { offset ->
            val day = weekStart.plusDays(offset.toLong())
            day to getCountForDay(day)
        }
    }

    /** 清除某天计数（柱状图筛选后如无数据也安全）。 */
    fun clearDay(date: LocalDate) {
        prefs.edit().remove(dailyPrefix + date.format(DAY_FORMAT)).apply()
    }

    private fun trimOldDays() {
        val cutoff = LocalDate.now().minusDays(MAX_DAYS.toLong()).format(DAY_FORMAT)
        val all = prefs.all
        val removeKeys = all.keys.filter {
            it.startsWith(dailyPrefix) && it.removePrefix(dailyPrefix) < cutoff
        }
        if (removeKeys.isNotEmpty()) {
            prefs.edit().apply {
                removeKeys.forEach { remove(it) }
            }.apply()
        }
    }
}
