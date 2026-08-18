package com.enlpot.notix

import android.app.Notification
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before

/**
 * 阶段2D：Action Flow Instrumentation 集成测试基类。
 *
 * 统一职责：
 * 1. 开启 [NotificationBlockerService.allowOwnPackageNotificationsForTest]（本进程通知可进真实链路）；
 * 2. 保证 NotificationListenerService 已连接（轮询 keepalive 通知 / requestRebind）；
 * 3. 每个用例前后清空 rules.json 与测试通知、剪贴板，避免串扰；
 * 4. 提供通知出现/消失/剪贴板/重发通知轮询断言与 logcat 采集。
 */
abstract class BaseActionFlowTest {

    protected lateinit var context: Context
    protected lateinit var nm: NotificationManager
    protected lateinit var ruleStorage: RuleStorage
    protected lateinit var cm: ClipboardManager

    @Before
    fun baseSetUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        ruleStorage = RuleStorage(context)
        ruleStorage.saveRules(emptyList())
        TestNotificationFactory.ensureChannel(context)
        clearTestNotifications()
        clearClipboard()
        NotificationBlockerService.allowOwnPackageNotificationsForTest = true
        ensureServiceConnected()
    }

    @After
    fun baseTearDown() {
        NotificationBlockerService.allowOwnPackageNotificationsForTest = false
        clearTestNotifications()
        clearClipboard()
        runCatching { ruleStorage.saveRules(emptyList()) }
    }

    // ---------- Service 连接 ----------

    /** 等待 NotificationListenerService 真实连接：轮询本 app keepalive 常驻通知出现 */
    protected fun ensureServiceConnected(timeoutMs: Long = 20000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (isKeepAliveVisible()) return
            // connectedDebugAndroidTest 每次安装 APK 会清除 NotificationListenerService 授权，
            // 测试内自愈：以 shell 身份重新 allow_listener + 写 enabled_notification_listeners（幂等）。
            ensureListenerAllowed()
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(context, NotificationBlockerService::class.java)
                )
            } catch (_: Exception) {
            }
            SystemClock.sleep(500)
        }
        // 最后兜底：再等 3s（keepalive 偶尔延迟拉起）
        SystemClock.sleep(3000)
        assertNotNull("NotificationListenerService 未在 ${timeoutMs}ms 内连接（keepalive 通知未出现）", findKeepAlive())
    }

    /** 以 shell 身份授权 NotificationListenerService（幂等，安装后授权被清除时自愈） */
    private fun ensureListenerAllowed() {
        try {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val listener = BuildConfig.APPLICATION_ID + "/" + NotificationBlockerService::class.java.name
            // executeShellCommand 由 instrumentation 以 shell 权限执行，无需 adopt
            shellOut(automation.executeShellCommand("cmd notification allow_listener $listener"))
            // Android 13+ 重装后 POST_NOTIFICATIONS 丢失会使应用通知 importance=NONE，
            // keepalive 通知被系统静默丢弃，导致 ensureServiceConnected 永远超时。幂等重授。
            shellOut(automation.executeShellCommand("pm grant ${BuildConfig.APPLICATION_ID} android.permission.POST_NOTIFICATIONS"))
            // settings put 会覆盖原值，先读现有值合并追加，避免清掉系统其它监听器
            val current = shellOut(
                automation.executeShellCommand("settings get secure enabled_notification_listeners")
            ).trim()
            val merged = when {
                current.isBlank() -> listener
                current.contains(listener) -> current
                else -> "$current:$listener"
            }
            shellOut(automation.executeShellCommand("settings put secure enabled_notification_listeners $merged"))
        } catch (_: Exception) {
        }
    }

    private fun shellOut(pfd: android.os.ParcelFileDescriptor?): String {
        if (pfd == null) return ""
        return try {
            val reader = java.io.FileInputStream(pfd.fileDescriptor).bufferedReader()
            val text = reader.readText()
            try {
                reader.close()
            } catch (_: Exception) {
            }
            text
        } catch (_: Exception) {
            ""
        } finally {
            try {
                pfd.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun isKeepAliveVisible(): Boolean = findKeepAlive() != null

    private fun findKeepAlive(): StatusBarNotification? {
        return try {
            nm.activeNotifications.firstOrNull {
                it.packageName == BuildConfig.APPLICATION_ID &&
                    it.id == 0x4B41
            }
        } catch (_: Exception) {
            null
        }
    }

    // ---------- 通知轮询断言 ----------

    protected fun findNotification(id: Int): StatusBarNotification? =
        nm.activeNotifications.firstOrNull {
            it.id == id && it.packageName == BuildConfig.APPLICATION_ID
        }

    protected fun waitForNotification(id: Int, timeoutMs: Long = 15000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (findNotification(id) != null) return
            SystemClock.sleep(100)
        }
        fail("通知 id=$id 未在 ${timeoutMs}ms 内出现")
    }

    protected fun waitForNotificationGone(id: Int, timeoutMs: Long = 20000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (findNotification(id) == null) return
            SystemClock.sleep(100)
        }
        fail("通知 id=$id 在 ${timeoutMs}ms 后仍存在（DISMISS/SILENT 未生效）")
    }

    /** 等待 SILENT 重发通知（rule_repost 频道、指定标题）出现 */
    protected fun waitForRepostNotification(title: String, timeoutMs: Long = 15000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val hit = nm.activeNotifications.firstOrNull {
                it.packageName == BuildConfig.APPLICATION_ID &&
                    it.notification.channelId == NotificationBlockerService.RULE_REPOST_CHANNEL_ID &&
                    it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() == title
            }
            if (hit != null) return
            SystemClock.sleep(100)
        }
        fail("SILENT 重发通知（标题='$title'）未在 ${timeoutMs}ms 内出现")
    }

    // ---------- 剪贴板 ----------

    /**
     * Android 13+ 后台/无焦点应用无法读取剪贴板（ClipboardService 拒绝非前台 uid）。
     * 测试进程同样受限，读取时临时 adopt shell 身份（Instrumentation UiAutomation），
     * 产品侧 COPY 写入仍由真实 Service 完成，不改动产品行为。
     */
    private fun <T> withShellPermissions(block: () -> T): T {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        try {
            automation.adoptShellPermissionIdentity()
        } catch (_: Exception) {
        }
        return try {
            block()
        } finally {
            try {
                automation.dropShellPermissionIdentity()
            } catch (_: Exception) {
            }
        }
    }

    protected fun clearClipboard() = withShellPermissions {
        // Android 会忽略空文本 ClipData（setPrimaryClip 静默失败），改用明确哨兵值保证
        // 剪贴板状态可预期，避免旧值残留误导后续断言（阶段 4C-C-B 修复 flaky 根因）。
        runCatching { cm.setPrimaryClip(ClipData.newPlainText("aft-clear", "AFT_CLEARED")) }
    }

    protected fun readClipboard(): String = withShellPermissions {
        try {
            cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    protected fun waitForClipboard(expected: String, timeoutMs: Long = 20000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (readClipboard() == expected) return
            SystemClock.sleep(100)
        }
        fail("剪贴板期望='$expected'，实际='${readClipboard()}'（超时 ${timeoutMs}ms）")
    }

    // ---------- Logcat ----------

    protected fun lastLogcat(tag: String, lines: Int = 600): String {
        return try {
            val p = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "-t", lines.toString(), "-s", "$tag:V")
            )
            p.inputStream.bufferedReader().readText()
        } catch (_: Exception) {
            ""
        }
    }

    // ---------- 清理 ----------

    private fun clearTestNotifications() {
        // 覆盖 13 项用例使用的全部 id + 重发 id 区间
        val ids = mutableListOf<Int>()
        for (i in 5001..5099) ids.add(i)
        // 重发 id： (id and 0xFFFF) + 100000 + seq
        for (i in 5001..5099) ids.add(i + 100000)
        for (i in 5001..5099) ids.add(i + 100001)
        TestNotificationFactory.cancel(context, *ids.toIntArray())
    }
}
