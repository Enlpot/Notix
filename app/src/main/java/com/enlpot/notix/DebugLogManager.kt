package com.enlpot.notix

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v8.47.0 诊断日志系统。
 *
 * 相比 [CrashLogManager]（只记录崩溃），DebugLog 记录关键业务路径的详细日志：
 * 插件下载/解压/加载、通知接收/记录/聚合/折叠、规则匹配/动作执行、启动流程等，
 * 带时间戳/线程/tag/级别/异常堆栈，便于远程诊断（用户复现后把日志文件发来即可定位）。
 *
 * 开关默认**关**（避免性能与存储开销）。但若检测到上次运行有崩溃（CrashLogManager 记录），
 * 下次启动时**自动开启**，让用户崩溃后复现操作的过程也能被记录。
 *
 * 日志文件（外部目录，普通文件管理器可访问）：
 * /storage/emulated/0/Android/data/<package>/files/debug_log.txt
 */
object DebugLogManager {
    private const val TAG = "DebugLog"
    private const val PREFS = "debug_log_prefs"
    private const val KEY_ENABLED = "debug_log_enabled"
    private const val KEY_PENDING_CRASH = "debug_log_pending_crash"
    private const val FILE_NAME = "debug_log.txt"
    /** 日志文件上限（字节），超出后裁掉最旧，保留最新 [TRIM_TO_BYTES] 字节 */
    private const val MAX_BYTES = 1024 * 1024
    private const val TRIM_TO_BYTES = 512 * 1024

    @Volatile
    private var enabled = false
    @Volatile
    private var initialized = false
    @Volatile
    private var appContext: Context? = null

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 日志抓取开关，默认关（崩溃后自动开）。 */
    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, on: Boolean) {
        val c = context.applicationContext
        prefs(c).edit().putBoolean(KEY_ENABLED, on).apply()
        enabled = on
        if (on) {
            append(c, "I", TAG, "调试日志已手动开启")
        }
    }

    /** 日志文件：外部目录 debug_log.txt。 */
    fun logFile(context: Context): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(dir, FILE_NAME)
    }

    /** 是否存在日志（文件非空）。 */
    fun hasLogs(context: Context): Boolean {
        val file = logFile(context)
        return file.exists() && file.length() > 0L
    }

    /** 读取日志全文；无日志时返回空字符串。 */
    fun readLogs(context: Context): String = try {
        if (hasLogs(context)) logFile(context).readText(Charsets.UTF_8) else ""
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read debug log", e)
        ""
    }

    /** 清空调试日志。 */
    fun clearLogs(context: Context) {
        try {
            val file = logFile(context)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear debug log", e)
        }
    }

    /**
     * 启动初始化：读开关；若检测到上次崩溃未处理则自动开启（便于崩溃后复现诊断）。
     * 在 Application.onCreate 中调用。
     */
    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
            appContext = context.applicationContext
            val c = appContext!!
            val p = prefs(c)
            var on = p.getBoolean(KEY_ENABLED, false)
            if (p.getBoolean(KEY_PENDING_CRASH, false)) {
                on = true
                p.edit().putBoolean(KEY_ENABLED, true).putBoolean(KEY_PENDING_CRASH, false).apply()
                append(c, "I", TAG, "检测到上次崩溃，调试日志已自动开启")
            }
            enabled = on
            if (on) {
                append(c, "I", TAG, "调试日志已启动 | ${deviceInfo(c)}")
            }
        }
    }

    /** 崩溃被记录时调用（CrashLogManager），标记下次启动自动开启调试日志。 */
    fun noteCrash(context: Context) {
        try {
            prefs(context).edit().putBoolean(KEY_PENDING_CRASH, true).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to note crash", e)
        }
    }

    // ---- 日志 API（未开启时零开销返回） ----
    fun d(tag: String, msg: String) = log("D", tag, msg, null)
    fun i(tag: String, msg: String) = log("I", tag, msg, null)
    fun w(tag: String, msg: String) = log("W", tag, msg, null)
    fun e(tag: String, msg: String, tr: Throwable? = null) = log("E", tag, msg, tr)

    private fun log(level: String, tag: String, msg: String, tr: Throwable?) {
        if (!enabled) return
        val c = appContext ?: return
        append(c, level, tag, msg, tr)
    }

    /** 追加一条日志到文件；文件超限裁掉最旧。写文件串行化保证不交错。 */
    private fun append(context: Context, level: String, tag: String, msg: String, tr: Throwable? = null) {
        try {
            val file = logFile(context)
            val sb = StringBuilder()
            sb.append(timestamp())
                .append(" [").append(level).append("] [").append(tag).append("] ")
                .append(Thread.currentThread().name).append(": ").append(msg).append('\n')
            if (tr != null) {
                // 打印完整 cause 链（最多 6 层），否则反射/包装异常（如 InvocationTargetException）的根因看不到
                var cur: Throwable? = tr
                var depth = 0
                while (cur != null && depth < 6) {
                    if (depth > 0) sb.append("    Caused by: ")
                    sb.append(cur.javaClass.name)
                    cur.message?.let { m -> sb.append(": ").append(m) }
                    sb.append('\n')
                    sb.append(cur.stackTrace.joinToString("\n") { "    at $it" }).append('\n')
                    cur = cur.cause
                    depth++
                }
            }
            synchronized(this) {
                val existing = if (file.exists()) file.readText(Charsets.UTF_8) else ""
                var all = sb.toString() + existing
                if (all.length > MAX_BYTES) {
                    all = all.take(TRIM_TO_BYTES)
                }
                file.writeText(all, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write debug log", e)
        }
    }

    /** 设备与运行环境信息（写进日志头，便于定位设备差异）。 */
    fun deviceInfo(context: Context): String {
        val pi = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
        val isDebug = pi?.applicationInfo?.flags?.and(ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        return "brand=${Build.BRAND} model=${Build.MODEL} sdk=${Build.VERSION.SDK_INT} " +
            "ver=${pi?.versionName ?: "?"}(${pi?.versionCode ?: "?"}) build=${if (isDebug) "debug" else "release"} " +
            "totalMem=${memInfo.totalMem} availMem=${memInfo.availMem}"
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
}
