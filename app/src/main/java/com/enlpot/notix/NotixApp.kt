package com.enlpot.notix

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.enlpot.notix.health.HealthCheckWorker

class NotixApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // v7.13：崩溃日志收集（默认开启，写入应用私有目录 crash_logs.txt）
        CrashLogManager.install(this)
        createHealthChannel()
        HealthCheckWorker.enqueue(this)
    }

    private fun createHealthChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            HealthCheckWorker.CHANNEL_ID,
            getString(R.string.health_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.health_channel_description)
        }
        nm.createNotificationChannel(channel)
    }
}
