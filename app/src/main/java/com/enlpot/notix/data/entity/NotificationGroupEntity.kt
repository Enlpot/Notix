package com.enlpot.notix.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 通知聚合组表。
 *
 * 同一条通知（相同 sbnKey）的多次变更聚合为一个组，
 * 组内每条变更存在 [NotificationChangeEntity] 表中。
 */
@Entity(
    tableName = "notification_group",
    indices = [
        Index(value = ["last_timestamp"]),
        Index(value = ["sbn_key"]),
        Index(value = ["package_name"]),
        // v8.22：复合索引——普通通知聚合查找（同包名+同标题+同blocked）
        Index(value = ["package_name", "title", "blocked"])
    ]
)
data class NotificationGroupEntity(
    @PrimaryKey
    val id: String,

    val package_name: String?,

    val app_label: String?,

    val title: String?,

    val count: Int = 1,

    val first_timestamp: Long = 0L,

    val last_timestamp: Long = 0L,

    val blocked: Int = 0,

    /** 该组的 sbnKey（用于 ongoing 通知全局查找聚合）。普通通知可能为 null。 */
    val sbn_key: String? = null,

    /** v8.41：是否为常驻通知聚合组（1=是，0=否）。用于普通通知聚合时跳过常驻通知。 */
    val was_ongoing: Int = 0
)

