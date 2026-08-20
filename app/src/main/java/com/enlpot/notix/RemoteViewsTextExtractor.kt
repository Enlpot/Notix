package com.enlpot.notix

import android.app.Notification
import android.util.Log

/**
 * v7.45：无标题无正文通知的文字提取器。
 *
 * 部分应用（如中国移动云盘 com.chinamobile.mcloud）的通知不设置
 * android.title / android.text，只通过 Notification.Action 按钮与
 * RemoteViews 自定义视图渲染（签到/福利/拍照上传/相册备份等），
 * 导致 Notix 原有逻辑直接忽略这类通知。
 *
 * 本提取器在设置开关「提取无文本通知的按钮文字」（默认关）开启时被调用：
 * - 优先走公开 API：Notification.Action[].title（通知可见按钮文字）；
 * - 补充反射解析 RemoteViews 的 setText / setContentDescription 动作；
 * 提取结果拼入 text 参与 RuleMatcher 关键字匹配与历史记录。
 *
 * 兼容性策略（API 24–36 / ColorOS 等厂商分支）：
 * 全程逐层 try-catch 降级，任何异常仅记录日志并返回 null，
 * 绝不因提取失败影响通知监听主流程。
 */
object RemoteViewsTextExtractor {

    private const val TAG = "RemoteViewsTextExtractor"

    /** ReflectionAction 中与文字相关的字段名 */
    private const val FIELD_SET_TEXT = "setText"
    private const val FIELD_SET_CONTENT_DESCRIPTION = "setContentDescription"

    /**
     * 从通知中提取可见文字，返回去除空白后的拼接文本；
     * 无任何可提取内容时返回 null。
     */
    fun extract(notification: Notification): String? {
        val parts = LinkedHashSet<String>()

        // 1) 公开 API：通知按钮（Notification.Action）标题
        try {
            notification.actions?.forEach { action ->
                val label = action.title?.toString()?.trim()
                if (!label.isNullOrBlank()) parts.add(label)
            }
        } catch (e: Exception) {
            Log.w(TAG, "extract Notification.actions failed", e)
        }

        // 2) 反射：RemoteViews 自定义视图中的文字
        try {
            notification.contentView?.let { collectRemoteViewsText(it, parts) }
            notification.bigContentView?.let { collectRemoteViewsText(it, parts) }
            notification.headsUpContentView?.let { collectRemoteViewsText(it, parts) }
        } catch (e: Exception) {
            Log.w(TAG, "extract RemoteViews failed", e)
        }

        return parts.joinToString(" ").ifBlank { null }
    }

    /**
     * 解析单个 RemoteViews 的 mActions 列表，收集文字类动作的值。
     * 反射读取失败整体降级（由调用方 try-catch 兜底）。
     */
    private fun collectRemoteViewsText(remoteViews: Any, out: MutableSet<String>) {
        val clazz = remoteViews.javaClass
        val actionsField = clazz.getDeclaredField("mActions")
        actionsField.isAccessible = true
        val actions = actionsField.get(remoteViews) as? List<*> ?: return
        for (action in actions) {
            if (action == null) continue
            collectActionText(action, out)
        }
    }

    /**
     * 从单个 RemoteViews.Action 中提取文字。
     * ReflectionAction 携带 fieldName（setText / setContentDescription 等）与
     * value（CharSequence）；仅收集文字类动作，忽略点击等无文字动作。
     */
    private fun collectActionText(action: Any, out: MutableSet<String>) {
        try {
            val clazz = action.javaClass
            val fieldNameField = clazz.getDeclaredField("fieldName")
            fieldNameField.isAccessible = true
            val fieldName = fieldNameField.get(action) as? String
            if (fieldName != FIELD_SET_TEXT && fieldName != FIELD_SET_CONTENT_DESCRIPTION) return

            val valueField = clazz.getDeclaredField("value")
            valueField.isAccessible = true
            val value = valueField.get(action)
            if (value is CharSequence) {
                val s = value.toString().trim()
                if (s.isNotBlank()) out.add(s)
            }
        } catch (e: Exception) {
            // 单个 action 解析失败不影响整体提取
            Log.d(TAG, "skip action ${action.javaClass.simpleName}", e)
        }
    }
}
