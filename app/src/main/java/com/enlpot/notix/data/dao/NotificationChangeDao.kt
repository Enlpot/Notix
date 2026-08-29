package com.enlpot.notix.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enlpot.notix.data.entity.NotificationChangeEntity

@Dao
interface NotificationChangeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(change: NotificationChangeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(changes: List<NotificationChangeEntity>)

    /** 获取某个聚合组下的所有变更记录，按时间倒序。 */
    @Query("SELECT * FROM notification_change WHERE group_id = :groupId ORDER BY timestamp DESC")
    suspend fun getChangesByGroupId(groupId: String): List<NotificationChangeEntity>

    @Query("DELETE FROM notification_change WHERE group_id = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Query("SELECT COUNT(*) FROM notification_change")
    suspend fun count(): Int

    /**
     * 全量搜索通知内容（标题或内容匹配关键词），按时间倒序。
     * 搜索覆盖所有历史数据，不受分页限制。
     */
    @Query(
        """
        SELECT * FROM notification_change 
        WHERE (title LIKE '%' || :keyword || '%' OR text LIKE '%' || :keyword || '%')
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun search(keyword: String, limit: Int, offset: Int): List<NotificationChangeEntity>

    /** 搜索结果总数。 */
    @Query(
        """
        SELECT COUNT(*) FROM notification_change 
        WHERE title LIKE '%' || :keyword || '%' OR text LIKE '%' || :keyword || '%'
        """
    )
    suspend fun searchCount(keyword: String): Int

    /**
     * v8.25：全局按 sbnKey + postTime 查找是否存在相同的通知变更记录（防重复入库）。
     * 系统可能对同一条通知多次触发 onNotificationPosted，全局查找避免只检查头部导致的重复。
     * @return 匹配的记录数（>0 表示已存在）
     */
    @Query("SELECT COUNT(*) FROM notification_change WHERE sbn_key = :sbnKey AND post_time = :postTime")
    suspend fun countBySbnKeyAndPostTime(sbnKey: String, postTime: Long): Int
}
