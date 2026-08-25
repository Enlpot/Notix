package com.enlpot.notix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Notix 表面层级（Surface Hierarchy / Elevation）。
 *
 * 问题：深色 background == surface == #1B1B1F，浅色 background == surface == #FDFCFF，
 * 仅靠背景色无法区分“页面 → 卡片 → 对话框”的层级（见 DESIGN_SYSTEM.md §6）。
 *
 * 方案：用明确的表面色令牌表达层级（MD3 在深色用 tonal elevation 叠加，此处用
 * 显式色值等价表达，便于页面直接取用，不必自行算叠加）：
 * - Base：页面背景
 * - Raised：悬浮控件 / 次级表面（略亮于 background）
 * - Card：内容卡片 / 分组块
 * - Dialog：模态弹窗背景（最高层级，明显浮起）
 *
 * 注：本层级与 MaterialTheme 的 elevation/shadow 互补——本工程统一用“表面色”
 * 而非阴影来表达层级（更符合 Notix 扁平深色风格），阴影仅保留在必要浮层。
 */
enum class NotixSurfaceLevel { Base, Raised, Card, Dialog }

data class NotixElevation(
    val base: Color,
    val raised: Color,
    val card: Color,
    val dialog: Color,
)

val LightNotixElevation = NotixElevation(
    base = md_theme_light_background,
    raised = Color(0xFFF2F3F7),
    card = Color(0xFFFFFFFF),
    dialog = Color(0xFFFFFFFF),
)

val DarkNotixElevation = NotixElevation(
    base = md_theme_dark_background,
    raised = Color(0xFF262A31),
    card = Color(0xFF2A2E35),
    dialog = Color(0xFF2A2E35),
)

val LocalNotixElevation = staticCompositionLocalOf { DarkNotixElevation }

val MaterialTheme.notixElevation: NotixElevation
    @Composable get() = LocalNotixElevation.current
