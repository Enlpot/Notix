package com.enlpot.notix.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 词云组件（v8.43.0：通知热词词云功能）
 *
 * 使用阿基米德螺旋算法 + AABB 碰撞检测布局词语。
 * 词语按词频降序排列，先放大词，再放小词。
 * 第一个词放在画布中心，后续每个词从中心开始沿螺旋向外移动，每步做碰撞检测。
 *
 * @param words 词语列表（词, 词频），已按词频降序排列
 * @param onWordClick 点击词语回调
 * @param modifier 修饰符
 */
@Composable
fun WordCloud(
    words: List<Pair<String, Int>>,
    onWordClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (words.isEmpty()) {
        Box(modifier = modifier.fillMaxSize()) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    // 计算每个词的字体大小和颜色
    val maxCount = words.maxOf { it.second }.coerceAtLeast(1)
    val minCount = words.minOf { it.second }.coerceAtLeast(1)

    val wordStyles = remember(words) {
        words.map { (word, count) ->
            // 词频越高，字体越大（12sp - 24sp 线性映射）
            val fontSize = if (maxCount == minCount) {
                18f
            } else {
                12f + (count - minCount).toFloat() / (maxCount - minCount) * 12f
            }
            // 词频越高，颜色越深（主题色从深到浅渐变）
            val colorRatio = if (maxCount == minCount) {
                0.7f
            } else {
                0.3f + (count - minCount).toFloat() / (maxCount - minCount) * 0.7f
            }
            WordStyle(word, fontSize, colorRatio)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Layout(
        content = {
            wordStyles.forEach { style ->
                val color = lerpColor(
                    Color(0xFF9CA3AF), // 浅灰
                    primaryColor,
                    style.colorRatio
                )
                Text(
                    text = style.word,
                    style = TextStyle(
                        fontSize = style.fontSize.sp,
                        fontWeight = FontWeight.Medium,
                        color = color
                    ),
                    modifier = Modifier.clickable { onWordClick(style.word) }
                )
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val centerX = width / 2f
        val centerY = height / 2f

        // 测量每个词
        val placeables = measurables.map { measurable ->
            measurable.measure(Constraints())
        }

        // 阿基米德螺旋布局
        val positions = calculateSpiralPositions(
            placeables = placeables,
            centerX = centerX,
            centerY = centerY,
            maxWidth = width,
            maxHeight = height
        )

        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                // 放置失败（找不到不重叠位置）的词直接跳过，避免堆叠在中心造成重叠
                val position = positions[index] ?: return@forEachIndexed
                placeable.placeRelative(
                    x = position.x.toInt(),
                    y = position.y.toInt()
                )
            }
        }
    }
}

/** 词语样式 */
private data class WordStyle(
    val word: String,
    val fontSize: Float,
    val colorRatio: Float
)

/**
 * 分层环形扫描布局算法（v8.48.2：修复词云重叠）。
 *
 * 第一个词放在画布中心，后续每个词从中心开始逐层向外（半径逐层增加），
 * 每一层沿圆周均匀取多个候选点做 AABB 碰撞检测，找到第一个不重叠且
 * 落在画布内的位置即放置。相比原阿基米德螺旋（半径增长慢、中心区域
 * 很难转出去），本算法覆盖面更均匀、搜索效率更高。
 *
 * 若某个词在整个画布内都找不到不重叠位置，则返回 null（渲染时跳过该词），
 * 绝不回退到中心导致重叠。
 *
 * @return 与 placeables 一一对应的位置列表；放不下的词对应 null
 */
private fun calculateSpiralPositions(
    placeables: List<Placeable>,
    centerX: Float,
    centerY: Float,
    maxWidth: Int,
    maxHeight: Int
): List<Offset?> {
    val positions = MutableList<Offset?>(placeables.size) { null }
    val placedRects = mutableListOf<Rect>()

    // 半径从中心逐层向外搜索
    val radiusStep = 6f // 每层半径增量（像素）
    val maxRadius = sqrt(centerX * centerX + centerY * centerY)

    placeables.forEachIndexed { index, placeable ->
        val w = placeable.width.toFloat()
        val h = placeable.height.toFloat()

        if (index == 0) {
            // 第一个词放在中心
            val x = centerX - w / 2
            val y = centerY - h / 2
            positions[index] = Offset(x, y)
            placedRects.add(Rect(x, y, x + w, y + h))
            return@forEachIndexed
        }

        // 从中心开始逐层向外搜索
        var radius = radiusStep
        search@ while (radius < maxRadius) {
            // 该半径上的候选点数按周长自适应，保证每层约每 12px 一个候选点
            val circumference = 2 * Math.PI * radius
            val steps = maxOf(12, (circumference / 12).toInt())
            for (i in 0 until steps) {
                val angle = 2 * Math.PI * i / steps
                val x = (centerX + radius * cos(angle)).toFloat() - w / 2
                val y = (centerY + radius * sin(angle)).toFloat() - h / 2

                // 检查是否在画布内
                if (x >= 0 && y >= 0 && x + w <= maxWidth && y + h <= maxHeight) {
                    val newRect = Rect(x, y, x + w, y + h)
                    val overlaps = placedRects.any { rect ->
                        rectsOverlap(rect, newRect, padding = 6f)
                    }
                    if (!overlaps) {
                        positions[index] = Offset(x, y)
                        placedRects.add(newRect)
                        break@search
                    }
                }
            }
            radius += radiusStep
        }
        // 整个画布都找不到位置 → 保持 null，渲染时跳过
    }

    return positions
}

/** 矩形 */
private data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/** 检查两个矩形是否重叠（带 padding） */
private fun rectsOverlap(a: Rect, b: Rect, padding: Float = 0f): Boolean {
    return a.left - padding < b.right &&
            a.right + padding > b.left &&
            a.top - padding < b.bottom &&
            a.bottom + padding > b.top
}

/** 颜色线性插值 */
private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}
