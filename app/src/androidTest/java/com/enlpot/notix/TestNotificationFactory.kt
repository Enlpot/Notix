package com.enlpot.notix

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon

/**
 * 阶段2D：真实 Notification 构造工厂。
 *
 * 模拟器 emulator-5554 的 `cmd notification post` 不可用（NameNotFoundException: root），
 * 集成测试通过本进程（Notix 自身包名）的 NotificationManager 直接 notify 真实通知，
 * 由 NotificationListenerService 回调驱动 Service→RuleMatcher→ActionFlowExecutor 真实链路。
 */
object TestNotificationFactory {

    const val CHANNEL_ID = "action_flow_test_channel"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Action Flow Test",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    fun createNotification(
        context: Context,
        title: String,
        text: String?,
        actions: List<Notification.Action>? = null
    ): Notification {
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_stack)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(false)
            .setWhen(System.currentTimeMillis())
        actions?.forEach { builder.addAction(it) }
        return builder.build()
    }

    fun actionButton(context: Context, label: String, intent: android.app.PendingIntent): Notification.Action =
        Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_stat_stack),
            label,
            intent
        ).build()

    fun notify(context: Context, id: Int, notification: Notification) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, notification)
    }

    fun cancel(context: Context, vararg ids: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ids.forEach { runCatching { nm.cancel(it) } }
    }
}
