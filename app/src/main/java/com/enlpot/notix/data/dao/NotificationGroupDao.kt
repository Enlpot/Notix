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
}
