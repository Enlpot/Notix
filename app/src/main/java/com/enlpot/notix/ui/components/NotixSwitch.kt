package com.enlpot.notix.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.enlpot.notix.ui.theme.NotixCorner
import com.enlpot.notix.ui.theme.notix

/**
 * 自定义紧凑 Switch 组件（v8.37）。
 *
 * 尺寸：40dp × 22dp（Material3 默认 Switch 含触摸目标约 56dp 高）。
 * 保持开关的滑动动画和交互习惯，只是缩小尺寸，更紧凑。
 *
 * 开启状态：填充主题色背景，白色滑块。
 * 关闭状态：透明背景 + 中灰胶囊边框，中灰滑块。
 *
 * @param checked 是否开启
 * @param onCheckedChange 状态变化回调
 * @param modifier 修饰符
 * @param checkedColor 开启时的背景色，默认用主题色
 * @param uncheckedBorderColor 关闭时的边框色，默认用中灰
 * @param checkedThumbColor 开启时的滑块颜色，默认白色
 * @param uncheckedThumbColor 关闭时的滑块颜色，默认中灰
 */
@Composable
fun NotixSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    checkedColor: Color = MaterialTheme.notix.primary,
    uncheckedBorderColor: Color = MaterialTheme.notix.outline,
    checkedThumbColor: Color = Color.White,
    uncheckedThumbColor: Color = MaterialTheme.notix.outline,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "switch_thumb",
    )

    val thumbColor = if (checked) checkedThumbColor else uncheckedThumbColor

    Box(
        modifier = modifier
            .size(width = 40.dp, height = 22.dp)
            .clip(NotixCorner.Full)
            .background(if (checked) checkedColor else Color.Transparent)
            .border(
                width = if (checked) 0.dp else 1.5.dp,
                color = if (checked) Color.Transparent else uncheckedBorderColor,
                shape = NotixCorner.Full,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .offset(x = thumbOffset)
                .size(18.dp)
                .clip(CircleShape)
                .background(thumbColor),
        )
    }
}
