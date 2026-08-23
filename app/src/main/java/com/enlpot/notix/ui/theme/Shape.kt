package com.enlpot.notix.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Notix 统一圆角令牌（UI 一致性规范）。
 * 所有页面、组件、弹窗共用同一套圆角，避免每处各写死数值。
 *
 * - Dialog：顶层弹窗外框，与 Material3 AlertDialog 默认 28dp 对齐
 * - Card：大区块 / 分段卡片
 * - ListItem：列表项 / 行卡片
 * - Control：按钮、输入框、芯片等交互控件
 * - Sm：小图标 / 徽标裁切
 */
object NotixCorner {
    val Dialog = RoundedCornerShape(28.dp)
    val Card = RoundedCornerShape(16.dp)
    val ListItem = RoundedCornerShape(12.dp)
    val Control = RoundedCornerShape(12.dp)
    val Sm = RoundedCornerShape(8.dp)
}
