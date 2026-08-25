package com.enlpot.notix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Notix 语义化颜色令牌（Semantic Color Tokens）。
 *
 * 设计原则（见 ui-ref/DESIGN_SYSTEM.md §2）：
 * - Token 表达“用途”而非“颜色”，页面代码应通过 [MaterialTheme.notix] 使用，
 *   不依赖 Light / Dark 判断；
 * - 复用 Material3 colorScheme 已有语义（primary / background / surface /
 *   surfaceVariant / outline / error 等），不做重复定义；
 * - 新增 MD3 未直接提供的语义层：contentSecondary / contentTertiary /
 *   contentDisabled / surfaceElevated / success / warning / info。
 *
 * 动态通知卡片颜色（NotificationColorEngine）不属于本令牌体系，
 * 见 DESIGN_SYSTEM.md §2.4 / §13，单独管理。
 */
@Immutable
data class NotixColors(
    // 背景与表面
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,   // 弹窗 / 浮层背景，需明显浮起于 surface

    // 内容文字
    val contentPrimary: Color,
    val contentSecondary: Color,
    val contentTertiary: Color,
    val contentDisabled: Color,

    // 品牌与强调
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,

    // 描边
    val outline: Color,
    val outlineVariant: Color,

    // 状态色
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val info: Color,
    val onInfo: Color,
)

// Light Theme —— 在 md_theme_light_* 基础上补齐语义层。
val LightNotixColors = NotixColors(
    background = md_theme_light_background,
    surface = md_theme_light_surface,
    surfaceVariant = md_theme_light_surfaceVariant,
    surfaceElevated = Color(0xFFFFFFFF),

    contentPrimary = md_theme_light_onBackground,
    contentSecondary = md_theme_light_onSurfaceVariant,
    contentTertiary = Color(0xFF8A8A90),
    contentDisabled = Color(0xFFC0C0C8),

    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,

    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,

    success = Color(0xFF146C2E),
    onSuccess = Color(0xFFFFFFFF),
    warning = Color(0xFF8A5A00),
    onWarning = Color(0xFFFFFFFF),
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    info = md_theme_light_primary,
    onInfo = md_theme_light_onPrimary,
)

// Dark Theme —— 在 md_theme_dark_* 基础上补齐语义层。
val DarkNotixColors = NotixColors(
    background = md_theme_dark_background,
    surface = md_theme_dark_surface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    surfaceElevated = Color(0xFF2A2E35),

    contentPrimary = md_theme_dark_onBackground,
    contentSecondary = md_theme_dark_onSurfaceVariant,
    contentTertiary = Color(0xFF6E6E76),
    contentDisabled = Color(0xFF4A4A52),

    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,

    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,

    success = Color(0xFF7FD896),
    onSuccess = Color(0xFF00391A),
    warning = Color(0xFFFFB74D),
    onWarning = Color(0xFF3A2400),
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    info = md_theme_dark_primary,
    onInfo = md_theme_dark_onPrimary,
)

val LocalNotixColors = staticCompositionLocalOf { LightNotixColors }

/** 语义化颜色入口：页面优先使用 [MaterialTheme.notix.contentPrimary] 等。 */
val MaterialTheme.notix: NotixColors
    @Composable
    get() = LocalNotixColors.current
