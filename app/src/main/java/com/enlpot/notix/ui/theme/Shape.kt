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
 * - Full：圆形（头像 / FAB / 药丸按钮 / 开关轨道）
 *
 * 使用约束：
 * - 卡片、列表项、控件、对话框统一引用上述令牌，禁止再写 RoundedCornerShape(16.dp) 等硬编码；
 * - 4.dp / 6.dp / 10.dp / 14.dp 等“紧凑/特殊值”仅在边框描边、缩略图等局部保留。
 */
object NotixCorner {
    val Dialog = RoundedCornerShape(28.dp)
    val Card = RoundedCornerShape(16.dp)
    val ListItem = RoundedCornerShape(12.dp)
    val Control = RoundedCornerShape(12.dp)
    val Sm = RoundedCornerShape(8.dp)
    val Full = RoundedCornerShape(999.dp)
}
