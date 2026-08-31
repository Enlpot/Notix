package com.enlpot.notix.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enlpot.notix.data.entity.NotificationGroupEntity

@Dao
interface NotificationGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: NotificationGroupEntity)

    @Update
    suspend fun update(group: NotificationGroupEntity)

    @Delete
    suspend fun delete(group: NotificationGroupEntity)

    /** 按 sbnKey 查找聚合组（ongoing 通知全局聚合用）。 */
    @Query("SELECT * FROM notification_group WHERE sbn_key = :sbnKey LIMIT 1")
    suspend fun findBySbnKey(sbnKey: String): NotificationGroupEntity?

    /** 按时间倒序获取所有聚合组。 */
    @Query("SELECT * FROM notification_group ORDER BY last_timestamp DESC")
    suspend fun getAllOrderedByTime(): List<NotificationGroupEntity>

    /** 分页获取聚合组。 */
    @Query("SELECT * FROM notification_group ORDER BY last_timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPaged(limit: Int, offset: Int): List<NotificationGroupEntity>

    /**
     * v8.41.2：直接获取最新的普通通知聚合组（排除常驻通知）。
     * 用于普通通知聚合判断，避免查询多条后在代码中过滤常驻通知。
     */
    @Query("SELECT * FROM notification_group WHERE was_ongoing = 0 ORDER BY last_timestamp DESC LIMIT 1")
    suspend fun getLatestNormal(): NotificationGroupEntity?

    /**
     * v8.41.3：修复旧数据中常驻通知组的 was_ongoing 字段。
     * 历史迁移时未设置 was_ongoing，导致常驻通知组被误判为普通通知。
     * 通过子查询检查组内是否有 was_ongoing=1 的 change，有则标记组为常驻通知。
     */
    @Query(
        """
        UPDATE notification_group SET was_ongoing = 1 
        WHERE was_ongoing = 0 AND id IN (
            SELECT DISTINCT group_id FROM notification_change WHERE was_ongoing = 1
        )
        """
    )
    suspend fun fixOngoingGroups()

    @Query("SELECT COUNT(*) FROM notification_group")
    suspend fun count(): Int

    @Query("DELETE FROM notification_group")
    suspend fun clearAll()

    @Query("DELETE FROM notification_group WHERE package_name = :pkg")
    suspend fun deleteByPackage(pkg: String)

    @Query("DELETE FROM notification_group WHERE blocked = 1")
    suspend fun deleteBlocked()

    @Query("SELECT * FROM notification_group WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): NotificationGroupEntity?

    /**
     * v8.27：通过 sbnKey + postTime 将对应聚合组标记为已过滤（blocked=1）。
     * 用于 applyRulesToActiveNotifications 处理已存在通知时更新 blocked 状态，
     * 修复全局去重跳过导致 blocked 不更新的 bug。
     */
    @Query(
        """
        UPDATE notification_group SET blocked = 1 
        WHERE id IN (SELECT group_id FROM notification_change WHERE sbn_key = :sbnKey AND post_time = :postTime)
        AND blocked = 0
        """
    )
    suspend fun markBlockedBySbnKeyAndPostTime(sbnKey: String, postTime: Long)
}


