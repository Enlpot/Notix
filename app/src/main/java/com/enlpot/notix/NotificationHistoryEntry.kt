package com.enlpot.notix

import androidx.annotation.Keep
import java.util.UUID

/**
 * 同 pkg + 同标题连续通知的聚合条目。
 *
 * [count] 为真实聚合次数（展示时直接显示实际次数，无 9+ 上限），
 * [changes] 为变更列表，按时间倒序，第一项即最新通知。
 * [blocked] 标记该聚合组是否被规则过滤（已过滤通知与普通通知不合并聚合）。
 * 存储层保留“隐藏而非删除”语义：聚合不会丢弃历史记录。
 */
@Keep
data class NotificationHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String? = null,
    val appLabel: String? = null,
    val title: String? = null,
    val count: Int = 1,
    val firstTimestamp: Long = 0L,
    val lastTimestamp: Long = 0L,
    val blocked: Boolean = false,
    val changes: List<SimpleNotification> = emptyList()
) {
    /** 最新一条通知（聚合卡片展示的主体）。 */
    val latest: SimpleNotification?
        get() = changes.firstOrNull()

    /** 展示用变更次数：直接显示实际次数（无 9+ 上限）。 */
    val displayCount: String
        get() = count.toString()
}
