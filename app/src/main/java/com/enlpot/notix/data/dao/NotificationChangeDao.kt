package com.enlpot.notix.data.dao

import androidx.room.Dao
import androidx.room.Embedded
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
     * v8.49：增强搜索——多字段 AND 组合 + 时间范围，全量覆盖所有历史。
     * 文本字段传空串表示不过滤；时间字段传 null 表示不限。
     * COALESCE 处理 NULL 字段，避免 NULL LIKE 不命中导致 app_label 为空的通知漏掉。
     * LEFT JOIN group 表带出 blocked 标记（供「已过滤」tab 展示）。
     */
    @Query(
        """
        SELECT c.*, g.blocked AS blocked 
        FROM notification_change c
        LEFT JOIN notification_group g ON g.id = c.group_id
        WHERE COALESCE(c.app_label, '') LIKE '%' || :app || '%'
          AND COALESCE(c.package_name, '') LIKE '%' || :pkg || '%'
          AND COALESCE(c.title, '') LIKE '%' || :title || '%'
          AND COALESCE(c.text, '') LIKE '%' || :text || '%'
          AND COALESCE(c.channel_id, '') LIKE '%' || :channel || '%'
          AND (:startTime IS NULL OR c.timestamp >= :startTime)
          AND (:endTime IS NULL OR c.timestamp <= :endTime)
        ORDER BY c.timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun advancedSearch(
        app: String, pkg: String, title: String, text: String, channel: String,
        startTime: Long?, endTime: Long?,
        limit: Int, offset: Int
    ): List<ChangeWithBlocked>

    /**
     * v8.25：全局按 sbnKey + postTime 查找是否存在相同的通知变更记录（防重复入库）。
     * 系统可能对同一条通知多次触发 onNotificationPosted，全局查找避免只检查头部导致的重复。
     * @return 匹配的记录数（>0 表示已存在）
     */
    @Query("SELECT COUNT(*) FROM notification_change WHERE sbn_key = :sbnKey AND post_time = :postTime")
    suspend fun countBySbnKeyAndPostTime(sbnKey: String, postTime: Long): Int

    /**
     * v8.42.2：获取某个聚合组下最新的一条变更记录（用于内容去重判断）。
     */
    @Query("SELECT * FROM notification_change WHERE group_id = :groupId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestByGroupId(groupId: String): NotificationChangeEntity?

    /**
     * v8.50.0：按 sbnKey 获取最新的一条变更记录（用于写入通知取消原因）。
     */
    @Query("SELECT * FROM notification_change WHERE sbn_key = :sbnKey ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBySbnKey(sbnKey: String): NotificationChangeEntity?

    /**
     * v8.52.x：按 sbnKey 批量更新取消原因，覆盖同 key 的全部记录。
     * 修复：同 sbnKey（如高频刷新/聚合折叠）存在多条记录时，仅更新最新一条导致其余记录
     * cancel_reason 为 null，详情显示「已结束」。规则命中（100=RULE_HIT_REASON）优先不覆盖。
     */
    @Query("UPDATE notification_change SET cancel_reason = :reason WHERE sbn_key = :sbnKey AND (cancel_reason IS NULL OR cancel_reason != 100)")
    suspend fun updateCancelReasonBySbnKey(sbnKey: String, reason: Int)

    /**
     * v8.43.0：查询最近的 N 条通知变更（用于词频全量重建）。
     */
    @Query("SELECT * FROM notification_change ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentChanges(limit: Int): List<NotificationChangeEntity>
}


/** v8.49：增强搜索结果行——通知变更 + 所属聚合组的 blocked 标记。 */
data class ChangeWithBlocked(
    @Embedded val change: NotificationChangeEntity,
    val blocked: Int
)

