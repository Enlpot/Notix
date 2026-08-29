package com.enlpot.notix.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 通知变更记录表。
 *
 * 每个聚合组下的每次通知到达/更新对应一条变更记录，
 * 按时间倒序排列，第一条即最新通知。
 */
@Entity(
    tableName = "notification_change",
    foreignKeys = [
        ForeignKey(
            entity = NotificationGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["group_id"]),
        Index(value = ["timestamp"]),
        Index(value = ["sbn_key"]),
        // v8.22：复合索引——组内变更按时间排序查询
        Index(value = ["group_id", "timestamp"])
    ]
)
data class NotificationChangeEntity(
    @PrimaryKey
    val id: String,

    val group_id: String,

    val app_label: String?,

    val package_name: String?,

    val title: String?,

    val text: String?,

    val timestamp: Long,

    val was_ongoing: Int = 0,

    val sbn_key: String?,

    val post_time: Long? = null,

    /** 命中规则 ID 列表，以 JSON 字符串存储。 */
    val matched_rule_ids: String? = null
)
