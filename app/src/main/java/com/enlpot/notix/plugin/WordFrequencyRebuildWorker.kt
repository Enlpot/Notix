package com.enlpot.notix.plugin

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.enlpot.notix.data.repository.WordFrequencyRepository
import java.util.concurrent.TimeUnit

/**
 * 词频全量重建 Worker（v8.43.0）
 *
 * 每天凌晨 3 点执行一次全量重建，防止增量更新累积误差。
 * 默认关闭，用户可在设置页开启。
 */
class WordFrequencyRebuildWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "WordFreqRebuildWorker"
        private const val WORK_NAME = "word_frequency_daily_rebuild"

        /** 启动每日定时重建（每天凌晨3点） */
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<WordFrequencyRebuildWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "已启动每日词频全量重建任务")
        }

        /** 取消每日定时重建 */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "已取消每日词频全量重建任务")
        }

        /** 计算到今天凌晨3点的延迟（如果已过，则明天凌晨3点） */
        private fun calculateInitialDelay(): Long {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 3)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            var target = calendar.timeInMillis
            if (target <= now) {
                target += 24 * 60 * 60 * 1000 // 明天凌晨3点
            }
            return target - now
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "开始执行词频全量重建...")
            val repository = WordFrequencyRepository(applicationContext)
            repository.rebuildAll()
            Log.i(TAG, "词频全量重建完成")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "词频全量重建失败", e)
            Result.retry()
        }
    }
}
