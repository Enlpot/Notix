package com.enlpot.notix

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.PendingIntent
import androidx.core.content.ContextCompat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 阶段2D：验证 PendingIntent.send() 是否被真实触发的测试接收器。
 *
 * 为 CLICK_BUTTON 动作构造的 Notification.Action 携带指向本接收器的 PendingIntent；
 * 若执行器正确匹配按钮并调用 actionIntent.send()，则 [await] 会在超时前返回 true。
 */
class TestPendingIntentReceiver {

    private val latch = CountDownLatch(1)

    @Volatile
    var receivedIntent: Intent? = null
        private set

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            receivedIntent = intent
            latch.countDown()
        }
    }

    fun register(context: Context): TestPendingIntentReceiver {
        // 必须 EXPORTED：CLICK_BUTTON 的 PendingIntent 由 Service 进程（不同 uid）send，
        // Android 13+ 对跨 uid 广播要求动态 receiver 显式导出，否则送达被系统拦截。
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ACTION),
            ContextCompat.RECEIVER_EXPORTED
        )
        return this
    }

    fun unregister(context: Context) {
        runCatching { context.unregisterReceiver(receiver) }
    }

    fun await(timeoutMs: Long): Boolean = latch.await(timeoutMs, TimeUnit.MILLISECONDS)

    fun pendingIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val ACTION = "com.enlpot.notix.test.PENDING_INTENT"
    }
}
