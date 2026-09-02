package com.enlpot.notix

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Keep
@Parcelize
data class SimpleNotification(
    val appLabel: String?,
    val packageName: String?,
    val title: String?,
    val text: String?,
    val timestamp: Long,
    val wasOngoing: Boolean = false,
    val id: String? = UUID.randomUUID().toString(),
    // v7.15：标识"同一条通知"（服务层防抖与存储层去重依据），旧数据为 null 时不做重复回调去重
    val sbnKey: String? = null,
    val postTime: Long? = null,
    // v7.36：命中规则 id（被过滤历史按规则分组依据），旧数据为空列表时归「未知规则」组
    val matchedRuleIds: List<String> = emptyList(),
    // v8.43.0：通知渠道 ID（用于按渠道聚合分析和详情展示）
    val channelId: String? = null,
    // v8.50.0：通知被移除时的取消原因（系统 reason code，null=未记录/仍在通知栏）
    val cancelReason: Int? = null,
    // v8.50.0：打开详情时该通知是否仍在系统通知栏（用于「当前状态」展示）
    val isActive: Boolean = false
) : Parcelable

