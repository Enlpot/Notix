package com.enlpot.notix

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream

/**
 * 通知历史存储（聚合模型）。
 *
 * 存储格式为 [NotificationHistoryEntry] 列表，按时间倒序（最新聚合组在前）。
 * 聚合规则：新通知到达时，若列表头部（最新）聚合组与它同 pkg 且同标题，
 * 则归入该组（count+1、变更列表插入最新）；否则新建聚合组。
 *
 * 保留“隐藏而非删除”语义：聚合只追加变更，不丢弃任何记录。
 * 兼容旧版 List<SimpleNotification> JSON：读取时自动迁移。
 *
 * v8.0：写盘改用 AtomicFile（与 RuleStorage 一致），避免写入中途进程被杀/断电导致
 * 整个历史文件丢失（原先 delete + renameTo 非原子）。
 */
class NotificationHistoryStorage(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHistoryStorage"

        /**
         * v8.0：进程级内存缓存——Service 与 MainActivity/HistoryScreen 各自持有一个
         * NotificationHistoryStorage 实例（同进程、同文件）。若缓存为实例级，服务写入后只刷新
         * 自身那一份，UI 实例的缓存停留在首次读取时的旧值，广播刷新时 getEntries() 直接命中
         * 陈旧缓存，历史便不再实时更新（H3 引入的回归）。改为伴生对象级（进程级）缓存后，
         * 所有实例共享同一份，任何一次写盘都会让后续读取立即可见，缓存与磁盘始终一致。
         */
        private val lock = Any()
        @Volatile
        private var cachedEntries: List<NotificationHistoryEntry>? = null
    }

    private val gson = Gson()
    private val historyFile = File(context.filesDir, "notification_history.json")
    private val atomicFile = AtomicFile(historyFile)
    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val historyDays get() = sharedPreferences.getInt("historyDays", 5)

    /** 读取全部聚合条目，按时间倒序（命中内存缓存直接返回，避免高频通知下重复解析全量 JSON）。 */
    fun getEntries(): List<NotificationHistoryEntry> {
        synchronized(lock) {
            cachedEntries?.let { return it }
        }
        if (!historyFile.exists()) {
            return emptyList()
        }
        return try {
            val json = atomicFile.readFully().toString(Charsets.UTF_8)
            try {
                val parsed = gson.fromJson<List<NotificationHistoryEntry>>(
                    json, object : TypeToken<List<NotificationHistoryEntry>>() {}.type
                ) ?: emptyList()
                synchronized(lock) { cachedEntries = parsed }
                parsed
            } catch (_: JsonSyntaxException) {
                // 旧格式（List<SimpleNotification>）迁移为聚合条目
                val migrated = migrateLegacy(json)
                synchronized(lock) { cachedEntries = migrated }
                migrated
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading notification history", e)
            emptyList()
        }
    }

    /**
     * v8.0：统一写盘 + 刷新内存缓存（所有写路径必经，保证缓存与磁盘一致，且缓存永不行于磁盘）。
     * 调用方需自行保证对外语义（如删除/聚合），本方法只负责持久化与缓存同步。
     */
    private fun replaceEntries(entries: List<NotificationHistoryEntry>) {
        writeEntries(entries)
        synchronized(lock) { cachedEntries = entries }
    }

    /** 最新通知列表（每个聚合组取 latest），兼容旧调用方。 */
    fun getHistory(): List<SimpleNotification> {
        return getEntries().mapNotNull { it.latest }
    }

    private fun migrateLegacy(json: String): List<NotificationHistoryEntry> {
        return try {
            val type = object : TypeToken<List<SimpleNotification>>() {}.type
            val legacy: List<SimpleNotification> = gson.fromJson(json, type) ?: emptyList()
            legacy.mapNotNull { n ->
                if (n.packageName == null && n.title == null) null
                else NotificationHistoryEntry(
                    packageName = n.packageName,
                    appLabel = n.appLabel,
                    title = n.title,
                    count = 1,
                    firstTimestamp = n.timestamp,
                    lastTimestamp = n.timestamp,
                    changes = listOf(n)
                )
            }.also { migrated ->
                // 立即迁移为聚合格式落盘并同步缓存
                replaceEntries(migrated)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Corrupted notification history file, deleting", e)
            historyFile.delete()
            emptyList()
        }
    }

    /**
     * v7.15 写入一条通知（三保险：服务层防抖 + 存储层同条去重 + 头部聚合）。
     * 1) 同条去重：仅当 sbnKey 相同且 postTime 相同（同一条通知的重复回调）时忽略写入，
     *    不再按 pkg+title+blocked 去重，避免标题相同的连续不同通知被误吞。
     * 2) 聚合：仅头部（最近写入）同 pkg+同 title+同 blocked 聚合展示（v7.11 原逻辑），
     *    去掉 v7.14 的"最近 10s 窗口"放宽，保证不同通知各自成组不遗漏。
     * 返回是否新建组（新建 true，聚合/忽略 false）。
     */
    fun saveNotification(notification: SimpleNotification, blocked: Boolean = false): Boolean {
        val entries = getEntries().toMutableList()
        val head = entries.firstOrNull()

        // 同条去重：同一 sbn.key 同一 postTime 视为同一条通知的重复回调，忽略写入
        if (head != null && notification.sbnKey != null && notification.postTime != null) {
            val headSn = head.changes.firstOrNull()
            if (headSn?.sbnKey == notification.sbnKey && headSn.postTime == notification.postTime) {
                return false
            }
        }

        // 聚合：仅头部同 pkg+同 title+同 blocked 合并展示（changes 保留，不丢数据）
        if (head != null &&
            head.packageName == notification.packageName &&
            head.title == notification.title &&
            head.blocked == blocked
        ) {
            val idx = entries.indexOf(head)
            entries[idx] = head.copy(
                count = head.count + 1,
                lastTimestamp = notification.timestamp,
                changes = listOf(notification) + head.changes
            )
            replaceEntries(entries)
            return false
        } else {
            entries.add(0, NotificationHistoryEntry(
                packageName = notification.packageName,
                appLabel = notification.appLabel,
                title = notification.title,
                count = 1,
                firstTimestamp = notification.timestamp,
                lastTimestamp = notification.timestamp,
                blocked = blocked,
                changes = listOf(notification)
            ))
            replaceEntries(entries)
            return true
        }
    }

    /**
     * 迁移 v7.11 及更早版本的分流数据：将 blocked_notification_history.json
     * 中保存的 SimpleNotification 列表合并进统一历史（带 blocked 标记）。
     * 幂等：同一通知重复合并时会按 pkg+title+时间戳去重（变更列表中已存在则跳过）。
     */
    fun mergeBlockedNotifications(blockedNotifications: List<SimpleNotification>) {
        if (blockedNotifications.isEmpty()) return
        val entries = getEntries().toMutableList()
        blockedNotifications.sortedByDescending { it.timestamp }.forEach { n ->
            val existing = entries.firstOrNull { e ->
                e.blocked &&
                    e.packageName == n.packageName &&
                    e.title == n.title &&
                    e.changes.any { it.timestamp == n.timestamp && it.title == n.title }
            }
            if (existing == null) {
                val head = entries.firstOrNull { e ->
                    e.blocked &&
                        e.packageName == n.packageName &&
                        e.title == n.title
                }
                if (head != null) {
                    val idx = entries.indexOf(head)
                    entries[idx] = head.copy(
                        count = head.count + 1,
                        lastTimestamp = n.timestamp,
                        changes = listOf(n) + head.changes
                    )
                } else {
                    entries.add(0, NotificationHistoryEntry(
                        packageName = n.packageName,
                        appLabel = n.appLabel,
                        title = n.title,
                        count = 1,
                        firstTimestamp = n.timestamp,
                        lastTimestamp = n.timestamp,
                        blocked = true,
                        changes = listOf(n)
                    ))
                }
            }
        }
        replaceEntries(entries)
    }

    /** 删除所有被过滤（blocked）的聚合组。 */
    fun clearBlockedHistory() {
        val filtered = getEntries().filterNot { it.blocked }
        replaceEntries(filtered)
    }

    /** 删除包含该通知的聚合组（同 pkg + 同标题）。 */
    fun deleteNotification(notification: SimpleNotification) {
        val entries = getEntries().toMutableList()
        entries.removeAll {
            it.packageName == notification.packageName && it.title == notification.title
        }
        replaceEntries(entries)
    }

    /** 删除指定 pkg 的全部历史（仅保留方法，用于设置页清除；监控暂停不再调用）。 */
    fun deleteNotificationsFromPackage(packageName: String) {
        val entries = getEntries().toMutableList()
        entries.removeAll { it.packageName == packageName }
        replaceEntries(entries)
    }

    /** 更新某 pkg 下所有聚合组及其变更列表中的 app 名称。 */
    fun updateAppLabelForPackage(packageName: String, newAppLabel: String) {
        val updated = getEntries().map { entry ->
            if (entry.packageName == packageName) {
                entry.copy(
                    appLabel = newAppLabel,
                    changes = entry.changes.map { it.copy(appLabel = newAppLabel) }
                )
            } else {
                entry
            }
        }
        replaceEntries(updated)
    }

    fun clearHistory() {
        if (historyFile.exists()) {
            historyFile.delete()
        }
        synchronized(lock) { cachedEntries = emptyList() }
    }

    fun clearHistoryBetween(startTime: Long, endTime: Long) {
        val filtered = getEntries().filter { it.lastTimestamp < startTime || it.firstTimestamp > endTime }
        replaceEntries(filtered)
    }

    fun clearHistoryByPackages(packages: Set<String>) {
        if (packages.isEmpty()) return
        val filtered = getEntries().filter { it.packageName !in packages }
        replaceEntries(filtered)
    }

    private fun writeEntries(entries: List<NotificationHistoryEntry>) {
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(gson.toJson(entries).toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            stream?.let { atomicFile.failWrite(it) }
            Log.e(TAG, "Failed to write notification history", e)
        }
    }
}
