package com.enlpot.notix.data.repository

import android.content.Context
import android.util.Log
import com.enlpot.notix.data.dao.NotificationChangeDao
import com.enlpot.notix.data.dao.WordFrequencyDao
import com.enlpot.notix.data.database.AppDatabase
import com.enlpot.notix.plugin.WordTokenizerManager
import com.enlpot.notix.plugin.WordFrequencyRebuildWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 词频统计仓库（v8.43.0：通知热词词云功能）
 *
 * 负责通知词频的增量更新、全量重建、查询。
 * 新通知到达时增量更新词频，用户打开统计页直接读数据库，0 计算。
 *
 * 时间范围：today（今日）/ week（本周）/ month（本月）/ all（全部）
 */
class WordFrequencyRepository(context: Context) {

    companion object {
        private const val TAG = "WordFrequencyRepo"

        // 时间范围常量
        const val TIME_RANGE_TODAY = "today"
        const val TIME_RANGE_WEEK = "week"
        const val TIME_RANGE_MONTH = "month"
        const val TIME_RANGE_ALL = "all"

        // 全量重建时最多分析的通知数（避免几万条全量分析卡顿）
        private const val MAX_NOTIFICATIONS_FOR_REBUILD = 5000

        // 设置相关
        private const val PREFS_NAME = "word_frequency_settings"
        private const val KEY_DAILY_REBUILD_ENABLED = "daily_rebuild_enabled"

        /** 每日全量重建是否开启（默认关闭） */
        fun isDailyRebuildEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_DAILY_REBUILD_ENABLED, false)
        }

        /** 设置每日全量重建开关 */
        fun setDailyRebuildEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_DAILY_REBUILD_ENABLED, enabled).apply()

            if (enabled) {
                WordFrequencyRebuildWorker.enqueue(context)
            } else {
                WordFrequencyRebuildWorker.cancel(context)
            }
        }
    }

    private val wordFrequencyDao: WordFrequencyDao
    private val notificationChangeDao: NotificationChangeDao

    init {
        val db = AppDatabase.getInstance(context.applicationContext)
        wordFrequencyDao = db.wordFrequencyDao()
        notificationChangeDao = db.notificationChangeDao()
    }

    /**
     * 增量更新：新通知到达时调用，对这一条通知的标题+内容分词，更新四个时间范围的词频。
     * 每条通知 < 10ms，用户无感知。
     */
    suspend fun incrementForNotification(title: String?, text: String?) = withContext(Dispatchers.IO) {
        val combinedText = listOfNotNull(title, text).joinToString(" ")
        if (combinedText.isBlank()) return@withContext

        val tokenizer = WordTokenizerManager.getTokenizer() ?: return@withContext
        val words = tokenizer.segment(combinedText)
        if (words.isEmpty()) return@withContext

        val timestamp = System.currentTimeMillis()
        val timeRanges = listOf(TIME_RANGE_TODAY, TIME_RANGE_WEEK, TIME_RANGE_MONTH, TIME_RANGE_ALL)

        for (word in words) {
            for (timeRange in timeRanges) {
                try {
                    wordFrequencyDao.incrementWord(word, timeRange, timestamp)
                } catch (e: Exception) {
                    Log.e(TAG, "增量更新词频失败: word=$word, timeRange=$timeRange", e)
                }
            }
        }

        Log.d(TAG, "增量更新词频完成: ${words.size} 个词")
    }

    /**
     * 全量重建：从通知历史重新计算所有时间范围的词频。
     * 用于兜底（防止增量更新累积误差）和手动刷新。
     * 最多分析 MAX_NOTIFICATIONS_FOR_REBUILD 条通知。
     */
    suspend fun rebuildAll() = withContext(Dispatchers.IO) {
        Log.i(TAG, "开始全量重建词频...")

        try {
            // 清空所有词频
            wordFrequencyDao.clearAll()

            // 查询最近的通知（最多 MAX_NOTIFICATIONS_FOR_REBUILD 条）
            val changes = notificationChangeDao.getRecentChanges(MAX_NOTIFICATIONS_FOR_REBUILD)
            Log.i(TAG, "查询到 ${changes.size} 条通知用于全量重建")

            val timestamp = System.currentTimeMillis()
            val timeRanges = listOf(TIME_RANGE_TODAY, TIME_RANGE_WEEK, TIME_RANGE_MONTH, TIME_RANGE_ALL)

            var processed = 0
            for (change in changes) {
                val combinedText = listOfNotNull(change.title, change.text).joinToString(" ")
                if (combinedText.isBlank()) continue

                val words = WordTokenizerManager.getTokenizer()?.segment(combinedText) ?: continue
                for (word in words) {
                    for (timeRange in timeRanges) {
                        try {
                            wordFrequencyDao.incrementWord(word, timeRange, timestamp)
                        } catch (e: Exception) {
                            // 忽略单个词的错误
                        }
                    }
                }
                processed++
            }

            // 清理词频为0的记录
            wordFrequencyDao.deleteZeroCount()

            Log.i(TAG, "全量重建词频完成: 处理 $processed 条通知")
        } catch (e: Exception) {
            Log.e(TAG, "全量重建词频失败", e)
        }
    }

    /**
     * 删除通知时调用，对被删除通知分词，对应词语词频 -1。
     */
    suspend fun decrementForNotification(title: String?, text: String?) = withContext(Dispatchers.IO) {
        val combinedText = listOfNotNull(title, text).joinToString(" ")
        if (combinedText.isBlank()) return@withContext

        val tokenizer = WordTokenizerManager.getTokenizer() ?: return@withContext
        val words = tokenizer.segment(combinedText)
        if (words.isEmpty()) return@withContext

        val timestamp = System.currentTimeMillis()
        val timeRanges = listOf(TIME_RANGE_TODAY, TIME_RANGE_WEEK, TIME_RANGE_MONTH, TIME_RANGE_ALL)

        for (word in words) {
            for (timeRange in timeRanges) {
                try {
                    wordFrequencyDao.decrementWord(word, timeRange, timestamp)
                } catch (e: Exception) {
                    Log.e(TAG, "递减词频失败: word=$word", e)
                }
            }
        }

        // 清理词频为0的记录
        wordFrequencyDao.deleteZeroCount()

        Log.d(TAG, "递减词频完成: ${words.size} 个词")
    }

    /** 查询某个时间范围的 Top N 热词 */
    suspend fun getTopWords(timeRange: String, limit: Int = 50) = withContext(Dispatchers.IO) {
        wordFrequencyDao.getTopWords(timeRange, limit)
    }

    /** 查询某个时间范围的词语总数 */
    suspend fun getWordCount(timeRange: String) = withContext(Dispatchers.IO) {
        wordFrequencyDao.countByTimeRange(timeRange)
    }

    /** 清空所有词频 */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        wordFrequencyDao.clearAll()
    }
}

