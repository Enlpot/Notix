package com.enlpot.notix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Notix 间距令牌（Spacing System）。
 *
 * 基于全工程 dp 使用统计（2/4/6/8/10/12/14/16/20/24/28），
 * 收敛为 4 基准的等距刻度：xs(4) sm(8) md(12) lg(16) xl(20) xxl(24)。
 * 6/10/14/28 等“特殊值”保留在局部使用，不强行归一（见 DESIGN_SYSTEM.md §4）。
 */
data class NotixSpacing(
    val xs: Dp,    // 4.dp  微间距 / 内联图标间隙
    val sm: Dp,    // 8.dp  紧凑间距 / 列表项内小间隙
    val md: Dp,    // 12.dp 卡片内分段 / 列表项间隙
    val lg: Dp,    // 16.dp 屏幕水平边距 / 卡片内边距
    val xl: Dp,    // 20.dp 区块间距
    val xxl: Dp,   // 24.dp 大区块 / 对话框内边距
)

val NotixSpacingTokens = NotixSpacing(
    xs = 4.dp,
    sm = 8.dp,
    md = 12.dp,
    lg = 16.dp,
    xl = 20.dp,
    xxl = 24.dp,
)

/**
 * 语义间距（布局语义，避免到处写魔法数字）。
 * 取值与现有页面主流约定一致：屏幕 16、卡片 16、列表项 12、对话框 24。
 */
data class NotixLayout(
    val screenHorizontal: Dp,  // 页面左右安全边距
    val cardPadding: Dp,       // 卡片内边距
    val sectionSpacing: Dp,    // 区块之间垂直间距
    val listItemSpacing: Dp,  // 列表项之间间距
    val dialogPadding: Dp,     // 对话框内容内边距
    val contentSpacing: Dp,    // 同一区块内元素间距
)

val NotixLayoutTokens = NotixLayout(
    screenHorizontal = 16.dp,
    cardPadding = 16.dp,
    sectionSpacing = 20.dp,
    listItemSpacing = 12.dp,
    dialogPadding = 24.dp,
    contentSpacing = 12.dp,
)

val LocalNotixSpacing = staticCompositionLocalOf { NotixSpacingTokens }
val LocalNotixLayout = staticCompositionLocalOf { NotixLayoutTokens }

val MaterialTheme.notixSpacing: NotixSpacing
    @Composable get() = LocalNotixSpacing.current

val MaterialTheme.notixLayout: NotixLayout
    @Composable get() = LocalNotixLayout.current
