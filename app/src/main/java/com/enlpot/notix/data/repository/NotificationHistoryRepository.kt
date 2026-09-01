package com.enlpot.notix.data.repository

import android.content.Context
import android.util.Log
import com.enlpot.notix.NotificationHistoryEntry
import com.enlpot.notix.SimpleNotification
import com.enlpot.notix.data.dao.NotificationChangeDao
import com.enlpot.notix.data.dao.NotificationGroupDao
import com.enlpot.notix.data.database.AppDatabase
import com.enlpot.notix.data.entity.NotificationChangeEntity
import com.enlpot.notix.data.entity.NotificationGroupEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.enlpot.notix.OngoingMergeStorage
import com.enlpot.notix.data.dao.OngoingAppRow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 通知历史仓库层（Room 实现）。
 *
 * 替代原 [com.enlpot.notix.NotificationHistoryStorage] 的 JSON 全量读写方案，
 * 提供：
 * - ongoing 通知按 sbnKey 全局聚合（修复重复卡片 bug）
 * - 按时间戳排序（更新后自然在最上面）
 * - 分页加载（UI 流畅）
 * - 全量搜索（覆盖所有历史数据）
 */
class NotificationHistoryRepository(context: Context) {

    // v8.42.0：常驻通知更新限流——同一个 sbnKey 30秒内只刷新一次 lastTimestamp
    // 减少频繁更新导致的数据库写入和UI刷新（如下载进度、音乐播放等）
    private val ongoingLastRefresh = mutableMapOf<String, Long>()
    private val ONGOING_REFRESH_INTERVAL_MS = 30_000L

    // v8.48.3：常驻通知生命周期合并设置（全局开关 + 按包名例外）
    private val ongoingMergeStorage = OngoingMergeStorage(context)
    // v8.48.3：生命周期边界——各 sbnKey 最后一次从通知栏移除的时间（由 Service.onNotificationRemoved 写入）。
    // 合并模式下：移除后重新出现且间隔 > LIFECYCLE_GAP_MS 视为新生命周期，记录一条 change（保留状态变化痕迹）。
    private val ongoingRemovedAt = ConcurrentHashMap<String, Long>()
    private val LIFECYCLE_GAP_MS = 5_000L

    private val TAG = "NotificationHistoryRepo"
    private val gson = Gson()
    private val db = AppDatabase.getInstance(context)
    private val groupDao: NotificationGroupDao = db.notificationGroupDao()
    private val changeDao: NotificationChangeDao = db.notificationChangeDao()
    private val wordFrequencyRepository: WordFrequencyRepository = WordFrequencyRepository(context)

    // ========== 核心写入 ==========

    /**
     * 保存一条通知。
     *
     * 聚合逻辑：
     * - ongoing 通知（wasOngoing=true 且 sbnKey 不为空）：按 sbnKey 全局查找现有组，
     *   找到则更新（count+1、追加 change、更新 lastTimestamp），找不到则新建组。
     *   因为查询按 last_timestamp 排序，更新后自然在最上面。
     * - 普通通知：保持原逻辑，仅头部（最新）同 pkg+同 title+同 blocked 才聚合，否则新建。
     *
     * @return 是否新建了聚合组（新建 true，聚合/忽略 false）
     */
    suspend fun saveNotification(notification: SimpleNotification, blocked: Boolean = false): Boolean {
        val sbnKey = notification.sbnKey
        val isOngoing = notification.wasOngoing && sbnKey != null

        return if (isOngoing) {
            saveOngoingNotification(notification, blocked, sbnKey!!)
        } else {
            saveNormalNotification(notification, blocked)
        }
    }

    /** ongoing 通知：按 sbnKey 全局查找聚合。 */
    private suspend fun saveOngoingNotification(
        notification: SimpleNotification,
        blocked: Boolean,
        sbnKey: String
    ): Boolean {
        val existing = groupDao.findBySbnKey(sbnKey)
        val blockedInt = if (blocked) 1 else 0

        return if (existing != null) {
            val now = System.currentTimeMillis()
            // v8.48.3：生命周期感知的常驻通知合并。
            // 生命周期 = 同一 sbnKey 通知从首次出现在通知栏到从通知栏移除的区间。
            // 合并模式（全局默认开 / 按包名例外）：生命周期内所有刷新合并为一条（只更新最新内容+时间，不增 count）；
            // 移除后重新出现且间隔 > 5s 视为新生命周期 → 记录一条 change（保留连接状态变化痕迹）。
            if (ongoingMergeStorage.shouldMerge(notification.packageName ?: "")) {
                val lastRemovedAt = ongoingRemovedAt[sbnKey] ?: 0L
                val isNewLifecycle = lastRemovedAt > 0L && (now - lastRemovedAt) > LIFECYCLE_GAP_MS
                val latestChange = changeDao.getLatestByGroupId(existing.id)

                if (isNewLifecycle && latestChange != null &&
                    latestChange.title == notification.title &&
                    latestChange.text == notification.text
                ) {
                    // 跨生命周期但内容未变化（低频重复提示，如系统悬浮窗提醒）：仍合并，不新增记录，时间用最新
                    val updatedGroup = existing.copy(
                        app_label = notification.appLabel,
                        title = notification.title,
                        count = existing.count,
                        last_timestamp = notification.timestamp,
                        blocked = blockedInt,
                        was_ongoing = 1
                    )
                    groupDao.update(updatedGroup)
                    val updatedChange = latestChange.copy(
                        title = notification.title,
                        text = notification.text,
                        timestamp = notification.timestamp
                    )
                    changeDao.insert(updatedChange)
                    ongoingRemovedAt[sbnKey] = 0L
                    Log.d(TAG, "Ongoing content same across lifecycle, keep merged: sbnKey=$sbnKey, count=${updatedGroup.count}")
                } else {
                    val updatedGroup = existing.copy(
                        app_label = notification.appLabel,
                        title = notification.title,
                        count = if (isNewLifecycle) existing.count + 1 else existing.count,
                        last_timestamp = notification.timestamp,
                        blocked = blockedInt,
                        was_ongoing = 1
                    )
                    groupDao.update(updatedGroup)

                    if (isNewLifecycle) {
                        // 新生命周期（重连/状态恢复）：记录一条 change，保留状态变化痕迹
                        val change = notification.toChangeEntity(updatedGroup.id)
                        changeDao.insert(change)
                        ongoingRemovedAt[sbnKey] = 0L
                        Log.d(TAG, "Ongoing new lifecycle recorded: sbnKey=$sbnKey, count=${updatedGroup.count}")
                    } else {
                        // 生命周期内：合并——更新最新 change 的内容与时间戳，不新增 change、不增加 count
                        if (latestChange != null) {
                            val updatedChange = latestChange.copy(
                                title = notification.title,
                                text = notification.text,
                                timestamp = notification.timestamp
                            )
                            changeDao.insert(updatedChange)
                        }
                        Log.d(TAG, "Ongoing merged in lifecycle: sbnKey=$sbnKey, count=${updatedGroup.count}")
                    }
                }
                false
            } else {
                // ===== 不合并模式（旧逻辑）：内容去重 + 30s 限流 + count+1 =====
                // v8.42.2：常驻通知内容去重——连续内容相同（标题+内容）的更新只刷新时间戳，不增加计数
                val latestChange = changeDao.getLatestByGroupId(existing.id)
                val contentSame = latestChange != null &&
                    latestChange.title == notification.title &&
                    latestChange.text == notification.text

                if (contentSame) {
                    groupDao.update(existing.copy(last_timestamp = notification.timestamp))
                    val latestUpdated = latestChange.copy(timestamp = notification.timestamp)
                    changeDao.insert(latestUpdated)
                    Log.d(TAG, "Ongoing notification content same, skip count but refresh timestamp: sbnKey=$sbnKey")
                    return false
                }

                // v8.42.0：常驻通知更新限流——30秒内只更新count，不刷新lastTimestamp
                val lastRefresh = ongoingLastRefresh[sbnKey] ?: 0L
                val shouldRefreshTimestamp = (now - lastRefresh) >= ONGOING_REFRESH_INTERVAL_MS

                val updatedGroup = if (shouldRefreshTimestamp) {
                    ongoingLastRefresh[sbnKey] = now
                    existing.copy(
                        count = existing.count + 1,
                        last_timestamp = notification.timestamp,
                        blocked = blockedInt,
                        was_ongoing = 1
                    )
                } else {
                    existing.copy(
                        count = existing.count + 1,
                        blocked = blockedInt,
                        was_ongoing = 1
                    )
                }
                groupDao.update(updatedGroup)

                val change = notification.toChangeEntity(updatedGroup.id)
                changeDao.insert(change)

                Log.d(TAG, "Ongoing notification aggregated: sbnKey=$sbnKey, count=${updatedGroup.count}, refreshTimestamp=$shouldRefreshTimestamp")
                false
            }
        } else {
            // 没找到：新建组 + 新建 change
            val groupId = UUID.randomUUID().toString()
            val newGroup = NotificationGroupEntity(
                id = groupId,
                package_name = notification.packageName,
                app_label = notification.appLabel,
                title = notification.title,
                count = 1,
                first_timestamp = notification.timestamp,
                last_timestamp = notification.timestamp,
                blocked = blockedInt,
                sbn_key = sbnKey,
                was_ongoing = 1
            )
            groupDao.insert(newGroup)

            val change = notification.toChangeEntity(groupId)
            changeDao.insert(change)

            Log.d(TAG, "Ongoing notification new group: sbnKey=$sbnKey")
            true
        }
    }

    /** Service 在通知移除时调用，记录该 sbnKey 的生命周期结束时间（内存态，重启丢失可接受）。 */
    fun markOngoingRemoved(sbnKey: String, removedAt: Long = System.currentTimeMillis()) {
        ongoingRemovedAt[sbnKey] = removedAt
    }

    /** 常驻通知涉及的 App 列表（设置页"高频常驻应用"管理用）。 */
    suspend fun getOngoingApps(): List<OngoingAppRow> = groupDao.getOngoingApps()

    /** 常驻通知合并开关实例（设置页读写用）。 */
    fun mergeStorage(): OngoingMergeStorage = ongoingMergeStorage

    /** 普通通知：仅头部同 pkg+同 title+同 blocked 才聚合。 */
    private suspend fun saveNormalNotification(
        notification: SimpleNotification,
        blocked: Boolean
    ): Boolean {
        // v8.41.2：直接查询最新的普通通知聚合组（在数据库层排除常驻通知）
        // 只排除常驻通知（was_ongoing=1），普通通知仍正常打断连续性
        val head = groupDao.getLatestNormal()
        val blockedInt = if (blocked) 1 else 0

        // v8.25：全局去重——同一 sbnKey 同一 postTime 视为同一条通知的重复回调，忽略。
        // 原只检查头部，若相同通知不在头部则会重复入库（BUG-001），改为全局查找。
        if (notification.sbnKey != null && notification.postTime != null) {
            val exists = changeDao.countBySbnKeyAndPostTime(notification.sbnKey, notification.postTime) > 0
            if (exists) {
                Log.d(TAG, "Duplicate notification ignored (global): sbnKey=${notification.sbnKey}")
                // v8.27：如果是被规则处理的通知（blocked=true），且现有记录未标记 blocked，
                // 则更新现有记录的 blocked 状态（修复 applyRulesToActiveNotifications 不更新 blocked 的 bug）
                if (blocked) {
                    groupDao.markBlockedBySbnKeyAndPostTime(notification.sbnKey, notification.postTime)
                    Log.d(TAG, "Updated existing notification to blocked: sbnKey=${notification.sbnKey}")
                }
                return false
            }
        }

        // 聚合：仅头部同 pkg+同 title+同 blocked
        if (head != null &&
            head.package_name == notification.packageName &&
            head.title == notification.title &&
            head.blocked == blockedInt
        ) {
            val updatedGroup = head.copy(
                count = head.count + 1,
                last_timestamp = notification.timestamp
            )
            groupDao.update(updatedGroup)

            val change = notification.toChangeEntity(head.id)
            changeDao.insert(change)

            Log.d(TAG, "Normal notification aggregated: pkg=${notification.packageName}, count=${updatedGroup.count}")
            return false
        } else {
            // 新建组
            val groupId = UUID.randomUUID().toString()
            val newGroup = NotificationGroupEntity(
                id = groupId,
                package_name = notification.packageName,
                app_label = notification.appLabel,
                title = notification.title,
                count = 1,
                first_timestamp = notification.timestamp,
                last_timestamp = notification.timestamp,
                blocked = blockedInt,
                sbn_key = notification.sbnKey
            )
            groupDao.insert(newGroup)

            val change = notification.toChangeEntity(groupId)
            changeDao.insert(change)

            Log.d(TAG, "Normal notification new group: pkg=${notification.packageName}")
            return true
        }
    }

    // ========== 读取 ==========
    /**
     * v8.41.3：修复旧数据中常驻通知组的 was_ongoing 字段。
     * 应在应用启动时调用一次。
     */
    suspend fun fixOngoingGroups() {
        val fixed = groupDao.fixOngoingGroups()
        Log.d(TAG, "Fixed ongoing groups: ")
    }


    /** 获取所有聚合组（按时间倒序）。 */
    suspend fun getEntries(): List<NotificationHistoryEntry> {
        val groups = groupDao.getAllOrderedByTime()
        return groups.map { it.toDomain() }
    }

    /** 分页获取聚合组（按时间倒序）。 */
    suspend fun getPagedEntries(limit: Int, offset: Int): List<NotificationHistoryEntry> {
        val groups = groupDao.getPaged(limit, offset)
        return groups.map { it.toDomain() }
    }

    /** 获取聚合组总数。 */
    suspend fun getGroupCount(): Int = groupDao.count()

    /** 获取变更记录总数。 */
    suspend fun getChangeCount(): Int = changeDao.count()

    /** 获取某个聚合组的完整信息（含变更列表）。 */
    suspend fun getEntryWithChanges(groupId: String): NotificationHistoryEntry? {
        val group = groupDao.findById(groupId) ?: return null
        val changes = changeDao.getChangesByGroupId(groupId)
        return group.toDomain(changes)
    }

    // ========== 搜索 ==========

    /**
     * 全量搜索通知内容（标题或内容匹配关键词），按时间倒序分页。
     * 搜索覆盖所有历史数据，不受列表分页限制。
     */
    suspend fun searchNotifications(keyword: String, limit: Int = 50, offset: Int = 0): List<SimpleNotification> {
        if (keyword.isBlank()) return emptyList()
        val changes = changeDao.search(keyword, limit, offset)
        return changes.map { it.toDomain() }
    }

    /** 搜索结果总数。 */
    suspend fun searchCount(keyword: String): Int {
        if (keyword.isBlank()) return 0
        return changeDao.searchCount(keyword)
    }

    /** v8.49：增强搜索——多字段 AND 组合 + 时间范围，全量覆盖所有历史。 */
    suspend fun advancedSearch(
        filters: AdvancedSearchFilters,
        limit: Int = 50,
        offset: Int = 0
    ): List<NotificationSearchResult> {
        if (filters.isEmpty) return emptyList()
        val rows = changeDao.advancedSearch(
            app = filters.app.trim(),
            pkg = filters.packageName.trim(),
            title = filters.title.trim(),
            text = filters.text.trim(),
            channel = filters.channelId.trim(),
            startTime = filters.startTime,
            endTime = filters.endTime,
            limit = limit,
            offset = offset
        )
        return rows.map { row ->
            NotificationSearchResult(
                notification = row.change.toDomain(),
                blocked = row.blocked == 1
            )
        }
    }

    // ========== 删除 ==========

    /** 清除全部历史。 */
    suspend fun clearHistory() {
        groupDao.clearAll()
        // v8.43.0：清空词频表
        wordFrequencyRepository.clearAll()
        // change 表通过外键 CASCADE 自动删除
        Log.i(TAG, "All history cleared")
    }

    /** 按包名删除历史。 */
    suspend fun deleteByPackage(packageName: String) {
        groupDao.deleteByPackage(packageName)
        Log.i(TAG, "History deleted for package: $packageName")
    }

    /** 删除已过滤（blocked）的历史。 */
    suspend fun deleteBlocked() {
        groupDao.deleteBlocked()
        Log.i(TAG, "Blocked history deleted")
    }

    /** 按时间范围清除历史（保留范围外的组）。 */
    suspend fun clearHistoryBetween(startTime: Long, endTime: Long) {
        val allGroups = groupDao.getAllOrderedByTime()
        val toDelete = allGroups.filter { it.last_timestamp >= startTime && it.first_timestamp <= endTime }
        toDelete.forEach { groupDao.delete(it) }
        Log.i(TAG, "History cleared between $startTime and $endTime, deleted ${toDelete.size} groups")
    }

    /** 按包名集合清除历史。 */
    suspend fun clearHistoryByPackages(packages: Set<String>) {
        if (packages.isEmpty()) return
        val allGroups = groupDao.getAllOrderedByTime()
        val toDelete = allGroups.filter { it.package_name in packages }
        toDelete.forEach { groupDao.delete(it) }
        Log.i(TAG, "History cleared for packages: $packages, deleted ${toDelete.size} groups")
    }

    /** 合并旧版被过滤通知（从 BlockedNotificationHistoryStorage 迁移）。 */
    suspend fun mergeBlockedNotifications(blockedNotifications: List<SimpleNotification>) {
        if (blockedNotifications.isEmpty()) return
        Log.i(TAG, "Merging ${blockedNotifications.size} blocked notifications")
        blockedNotifications.sortedByDescending { it.timestamp }.forEach { notification ->
            saveNotification(notification, blocked = true)
        }
    }

    /** 删除包含该通知的聚合组（同 pkg + 同标题）。 */
    suspend fun deleteNotification(notification: SimpleNotification) {
        // 先找到匹配的组，再逐个删除（change 通过外键级联删除）
        val allGroups = groupDao.getAllOrderedByTime()
        val toDelete = allGroups.filter {
            it.package_name == notification.packageName && it.title == notification.title
        }
        // v8.43.0：词频递减
        toDelete.forEach { group ->
            wordFrequencyRepository.decrementForNotification(group.title, null)
        }
        toDelete.forEach { groupDao.delete(it) }
        Log.i(TAG, "Notification deleted: pkg=${notification.packageName}, title=${notification.title}")
    }

    /** 更新某 pkg 下所有聚合组的 app 名称。 */
    suspend fun updateAppLabelForPackage(packageName: String, newAppLabel: String) {
        val groups = groupDao.getAllOrderedByTime()
        groups.filter { it.package_name == packageName }.forEach { group ->
            val updated = group.copy(app_label = newAppLabel)
            groupDao.update(updated)
            // 同时更新该组下所有 change 的 app_label
            val changes = changeDao.getChangesByGroupId(group.id)
            changes.forEach { change ->
                val updatedChange = change.copy(app_label = newAppLabel)
                // NotificationChangeDao 没有 update 方法，用 insert REPLACE
                changeDao.insert(updatedChange)
            }
        }
        Log.i(TAG, "App label updated for package: $packageName -> $newAppLabel")
    }

    // ========== 转换 ==========

    private fun SimpleNotification.toChangeEntity(groupId: String): NotificationChangeEntity {
        return NotificationChangeEntity(
            id = this.id ?: UUID.randomUUID().toString(),
            group_id = groupId,
            app_label = this.appLabel,
            package_name = this.packageName,
            title = this.title,
            text = this.text,
            timestamp = this.timestamp,
            was_ongoing = if (this.wasOngoing) 1 else 0,
            sbn_key = this.sbnKey,
            post_time = this.postTime,
            matched_rule_ids = if (this.matchedRuleIds.isNotEmpty()) {
                gson.toJson(this.matchedRuleIds)
            } else null,
            channel_id = this.channelId
        )
    }

    private fun NotificationChangeEntity.toDomain(): SimpleNotification {
        val matchedIds: List<String> = this.matched_rule_ids?.let { json ->
            try {
                gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        return SimpleNotification(
            appLabel = this.app_label,
            packageName = this.package_name,
            title = this.title,
            text = this.text,
            timestamp = this.timestamp,
            wasOngoing = this.was_ongoing == 1,
            id = this.id,
            sbnKey = this.sbn_key,
            postTime = this.post_time,
            matchedRuleIds = matchedIds,
            channelId = this.channel_id
        )
    }

    private suspend fun NotificationGroupEntity.toDomain(): NotificationHistoryEntry {
        val changes = changeDao.getChangesByGroupId(this.id)
        return toDomain(changes)
    }

    private fun NotificationGroupEntity.toDomain(changes: List<NotificationChangeEntity>): NotificationHistoryEntry {
        return NotificationHistoryEntry(
            id = this.id,
            packageName = this.package_name,
            appLabel = this.app_label,
            title = this.title,
            count = this.count,
            firstTimestamp = this.first_timestamp,
            lastTimestamp = this.last_timestamp,
            blocked = this.blocked == 1,
            changes = changes.map { it.toDomain() }
        )
    }

    /**
     * v8.24：检查指定 sbnKey 的通知是否已存在于历史中（防漏通知同步时去重用）。
     * @return true=已存在，false=不存在
     */
    suspend fun existsBySbnKey(sbnKey: String?): Boolean {
        if (sbnKey.isNullOrBlank()) return false
        return groupDao.findBySbnKey(sbnKey) != null
    }
}


/** v8.49：增强搜索筛选条件（空字段表示不过滤）。 */
data class AdvancedSearchFilters(
    val app: String = "",
    val packageName: String = "",
    val title: String = "",
    val text: String = "",
    val channelId: String = "",
    val startTime: Long? = null,
    val endTime: Long? = null
) {
    val isEmpty: Boolean
        get() = app.isBlank() && packageName.isBlank() && title.isBlank() &&
            text.isBlank() && channelId.isBlank() && startTime == null && endTime == null
}

/** v8.49：增强搜索结果——通知 + 所属组 blocked 标记。 */
data class NotificationSearchResult(
    val notification: SimpleNotification,
    val blocked: Boolean
)







