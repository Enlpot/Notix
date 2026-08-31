package com.enlpot.notix.data.migration

import android.content.Context
import android.util.Log
import com.enlpot.notix.NotificationHistoryEntry
import com.enlpot.notix.SimpleNotification
import com.enlpot.notix.data.dao.NotificationChangeDao
import com.enlpot.notix.data.dao.NotificationGroupDao
import com.enlpot.notix.data.entity.NotificationChangeEntity
import com.enlpot.notix.data.entity.NotificationGroupEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

/**
 * 旧 JSON 历史数据迁移到 Room。
 *
 * 迁移流程：
 * 1. 检测旧文件 notification_history.json 是否存在
 * 2. 读取并解析为 List<NotificationHistoryEntry>
 * 3. 转换为 Entity 写入 Room（group + changes）
 * 4. 验证写入数量
 * 5. 迁移成功后直接删除旧文件（Room 已有完整数据）
 */
class HistoryMigration(
    private val context: Context,
    private val groupDao: NotificationGroupDao,
    private val changeDao: NotificationChangeDao
) {

    private val TAG = "HistoryMigration"
    private val gson = Gson()
    private val oldFile = File(context.filesDir, "notification_history.json")
    private val backupFile = File(context.filesDir, "notification_history.json.bak")

    /** 是否需要迁移（旧文件存在且尚未迁移）。 */
    fun needsMigration(): Boolean {
        return oldFile.exists()
    }

    /**
     * 执行迁移。
     * @return 迁移结果（成功/失败/无需迁移）
     */
    suspend fun migrate(): MigrationResult {
        if (!needsMigration()) {
            Log.i(TAG, "No migration needed: old file not found")
            return MigrationResult.SKIPPED
        }

        return try {
            Log.i(TAG, "Starting migration from JSON to Room")

            // 1. 读取旧数据
            val json = oldFile.readText(Charsets.UTF_8)
            val oldEntries: List<NotificationHistoryEntry> = try {
                gson.fromJson(json, object : TypeToken<List<NotificationHistoryEntry>>() {}.type)
                    ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse old JSON, attempting legacy format", e)
                // 旧格式（List<SimpleNotification>）迁移
                migrateLegacyFormat(json)
                return MigrationResult.SUCCESS
            }

            if (oldEntries.isEmpty()) {
                Log.i(TAG, "Old history is empty, just deleting file")
                oldFile.delete()
                return MigrationResult.SUCCESS
            }

            Log.i(TAG, "Found ${oldEntries.size} old groups to migrate")

            // 2. 转换并写入
            var groupCount = 0
            var changeCount = 0

            for (entry in oldEntries) {
                val groupId = entry.id.ifBlank { UUID.randomUUID().toString() }

                // 写入 group
                val entryWasOngoing = entry.changes.firstOrNull()?.wasOngoing ?: false
                val groupEntity = NotificationGroupEntity(
                    id = groupId,
                    package_name = entry.packageName,
                    app_label = entry.appLabel,
                    title = entry.title,
                    count = entry.count,
                    first_timestamp = entry.firstTimestamp,
                    last_timestamp = entry.lastTimestamp,
                    blocked = if (entry.blocked) 1 else 0,
                    sbn_key = entry.changes.firstOrNull()?.sbnKey,
                    was_ongoing = if (entryWasOngoing) 1 else 0
                )
                groupDao.insert(groupEntity)
                groupCount++

                // 写入 changes
                val changeEntities = entry.changes.map { notification ->
                    notification.toChangeEntity(groupId)
                }
                changeDao.insertAll(changeEntities)
                changeCount += changeEntities.size
            }

            // 3. 验证
            val dbGroupCount = groupDao.count()
            val dbChangeCount = changeDao.count()
            Log.i(TAG, "Migration complete: groups=$groupCount (db=$dbGroupCount), changes=$changeCount (db=$dbChangeCount)")

            if (dbGroupCount != groupCount || dbChangeCount != changeCount) {
                Log.w(TAG, "Migration count mismatch! Expected groups=$groupCount changes=$changeCount, actual groups=$dbGroupCount changes=$dbChangeCount")
            }

            // 4. 删除旧文件（Room 已有完整数据）
            if (backupFile.exists()) {
                backupFile.delete()
            }
            oldFile.renameTo(backupFile)
            Log.i(TAG, "Old file backed up to ${backupFile.name}")

            MigrationResult.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            MigrationResult.FAILED(e.message ?: "Unknown error")
        }
    }

    /** 旧格式（List<SimpleNotification>）迁移。 */
    private suspend fun migrateLegacyFormat(json: String) {
        try {
            val type = object : TypeToken<List<SimpleNotification>>() {}.type
            val legacy: List<SimpleNotification> = gson.fromJson(json, type) ?: emptyList()

            Log.i(TAG, "Legacy format: ${legacy.size} notifications")

            // 简单迁移：每条通知作为一个独立的 group
            for (notification in legacy) {
                val groupId = UUID.randomUUID().toString()
                val groupEntity = NotificationGroupEntity(
                    id = groupId,
                    package_name = notification.packageName,
                    app_label = notification.appLabel,
                    title = notification.title,
                    count = 1,
                    first_timestamp = notification.timestamp,
                    last_timestamp = notification.timestamp,
                    blocked = 0,
                    sbn_key = notification.sbnKey,
                    was_ongoing = if (notification.wasOngoing) 1 else 0
                )
                groupDao.insert(groupEntity)
                changeDao.insert(notification.toChangeEntity(groupId))
            }

            oldFile.delete()
            Log.i(TAG, "Legacy migration complete, old file deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Legacy migration failed", e)
            throw e
        }
    }

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
            } else null
        )
    }

    sealed class MigrationResult {
        object SUCCESS : MigrationResult()
        object SKIPPED : MigrationResult()
        data class FAILED(val message: String) : MigrationResult()
    }
}

