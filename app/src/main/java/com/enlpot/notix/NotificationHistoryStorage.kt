package com.enlpot.notix

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * 通知历史存储（聚合模型）。
 *
 * 存储格式为 [NotificationHistoryEntry] 列表，按时间倒序（最新聚合组在前）。
 * 聚合规则：新通知到达时，若列表头部（最新）聚合组与它同 pkg 且同标题，
 * 则归入该组（count+1、变更列表插入最新）；否则新建聚合组。
 *
 * 保留“隐藏而非删除”语义：聚合只追加变更，不丢弃任何记录。
 * 兼容旧版 List<SimpleNotification> JSON：读取时自动迁移。
 */
class NotificationHistoryStorage(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHistoryStorage"
    }

    private val gson = Gson()
    private val historyFile = File(context.filesDir, "notification_history.json")
    private val historyTmpFile = File(context.filesDir, "notification_history.json.tmp")
    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val historyDays get() = sharedPreferences.getInt("historyDays", 5)

    /** 读取全部聚合条目，按时间倒序。 */
    fun getEntries(): List<NotificationHistoryEntry> {
        if (!historyFile.exists()) {
            return emptyList()
        }
        return try {
            val json = historyFile.readText()
            try {
                val type = object : TypeToken<List<NotificationHistoryEntry>>() {}.type
                gson.fromJson<List<NotificationHistoryEntry>>(json, type) ?: emptyList()
            } catch (_: JsonSyntaxException) {
                // 旧格式（List<SimpleNotification>）迁移为聚合条目
                migrateLegacy(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading notification history", e)
            emptyList()
        }
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
                // 立即迁移为聚合格式落盘
                writeEntries(migrated)
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
            writeEntries(entries)
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
            writeEntries(entries)
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
        writeEntries(entries)
    }

    /** 删除所有被过滤（blocked）的聚合组。 */
    fun clearBlockedHistory() {
        val filtered = getEntries().filterNot { it.blocked }
        writeEntries(filtered)
    }

    /** 删除包含该通知的聚合组（同 pkg + 同标题）。 */
    fun deleteNotification(notification: SimpleNotification) {
        val entries = getEntries().toMutableList()
        entries.removeAll {
            it.packageName == notification.packageName && it.title == notification.title
        }
        writeEntries(entries)
    }

    /** 删除指定 pkg 的全部历史（仅保留方法，用于设置页清除；监控暂停不再调用）。 */
    fun deleteNotificationsFromPackage(packageName: String) {
        val entries = getEntries().toMutableList()
        entries.removeAll { it.packageName == packageName }
        writeEntries(entries)
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
        writeEntries(updated)
    }

    fun clearHistory() {
        if (historyFile.exists()) {
            historyFile.delete()
        }
    }

    fun clearHistoryBetween(startTime: Long, endTime: Long) {
        val filtered = getEntries().filter { it.lastTimestamp < startTime || it.firstTimestamp > endTime }
        writeEntries(filtered)
    }

    fun clearHistoryByPackages(packages: Set<String>) {
        if (packages.isEmpty()) return
        val filtered = getEntries().filter { it.packageName !in packages }
        writeEntries(filtered)
    }

    private fun writeEntries(entries: List<NotificationHistoryEntry>) {
        val json = gson.toJson(entries)
        historyTmpFile.writeText(json)
        if (historyFile.exists()) {
            historyFile.delete()
        }
        historyTmpFile.renameTo(historyFile)
    }
}
