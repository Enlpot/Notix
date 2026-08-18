package com.enlpot.notix

import android.content.Context
import android.content.SharedPreferences
import android.os.Process
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.13 崩溃日志收集。
 *
 * 应用启动时（Application.onCreate）注册默认未捕获异常处理器；崩溃时将时间戳、
 * 异常类型与完整堆栈追加写入应用私有目录 crash_logs.txt（UTF-8），保留最近
 * [MAX_ENTRIES] 条，超出滚动覆盖最旧。写入完成后链式调用原 handler，保证系统
 * 默认崩溃流程不受影响。
 *
 * 抓取开关默认开启（SharedPreferences 持久化）；用户关闭后，本次运行周期内即使
 * 崩溃也不写入日志，重新开启后恢复。
 */
object CrashLogManager {
    private const val TAG = "CrashLogManager"
    private const val PREFS = "crash_log_prefs"
    private const val KEY_ENABLED = "crash_log_enabled"
    private const val FILE_NAME = "crash_logs.txt"
    private const val MAX_ENTRIES = 20
    private const val ENTRY_SEPARATOR = "===== "

    @Volatile
    private var installed = false

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 日志抓取开关，默认开启。 */
    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * 日志文件（v7.29 起存外部目录，普通文件管理器可访问）：
     * /storage/emulated/0/Android/data/<package>/files/crash_logs.txt
     */
    fun logFile(context: Context): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(dir, FILE_NAME)
    }

    /** 旧版日志位置（v7.13~v7.28 内部私有目录），用于一次性迁移。 */
    private fun legacyLogFile(context: Context): File = File(context.filesDir, FILE_NAME)

    /** 是否存在崩溃记录（文件非空）。 */
    fun hasCrashes(context: Context): Boolean {
        val file = logFile(context)
        return file.exists() && file.length() > 0L
    }

    /**
     * 一次性迁移旧版内部目录日志到外部目录。
     * 仅当新位置无日志且旧位置有日志时执行：复制旧文件到新位置后删除旧文件。
     */
    fun migrateLegacyLog(context: Context) {
        try {
            val legacy = legacyLogFile(context)
            val current = logFile(context)
            if (legacy.exists() && legacy.length() > 0L && (!current.exists() || current.length() == 0L)) {
                legacy.copyTo(current, overwrite = true)
            }
            if (legacy.exists()) legacy.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate crash log", e)
        }
    }

    /** 清空全部崩溃日志（删除日志文件）。 */
    fun clearLogs(context: Context) {
        try {
            val file = logFile(context)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear crash log", e)
        }
    }

    /** 读取日志全文；无日志时返回空字符串。 */
    fun readLogs(context: Context): String = try {
        if (hasCrashes(context)) logFile(context).readText(Charsets.UTF_8) else ""
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read crash log", e)
        ""
    }

    /**
     * 安装默认未捕获异常处理器（幂等）。链式保存原 handler：写入日志后调用原
     * handler；原 handler 为空时按系统默认行为终止进程。
     */
    fun install(context: Context) {
        if (installed) return
        synchronized(this) {
            if (installed) return
            installed = true
            val appContext = context.applicationContext
            // v7.29：启动时一次性迁移旧版内部目录日志到外部目录
            migrateLegacyLog(appContext)
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                if (isEnabled(appContext)) {
                    appendCrash(appContext, thread, throwable)
                }
                previous?.uncaughtException(thread, throwable) ?: run {
                    Process.killProcess(Process.myPid())
                    System.exit(10)
                }
            }
        }
    }

    private fun appendCrash(context: Context, thread: Thread, throwable: Throwable) {
        try {
            val file = logFile(context)
            val existing = if (file.exists()) file.readText(Charsets.UTF_8) else ""
            val sb = StringBuilder()
            sb.append(ENTRY_SEPARATOR).append(timestamp()).append(" =====\n")
            sb.append("Thread: ").append(thread.name).append('\n')
            sb.append("Exception: ").append(throwable.javaClass.name)
            throwable.message?.let { sb.append(": ").append(it) }
            sb.append('\n')
            sb.append(throwable.stackTrace.joinToString("\n") { "    at $it" })
            sb.append('\n')
            val all = sb.toString() + existing
            // 按分隔块切分，最新在前，滚动保留最近 MAX_ENTRIES 条
            val blocks = all.split(ENTRY_SEPARATOR).filter { it.isNotBlank() }
            val kept = blocks.take(MAX_ENTRIES).joinToString(separator = ENTRY_SEPARATOR) { ENTRY_SEPARATOR + it }
            file.writeText(kept, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log", e)
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
}
