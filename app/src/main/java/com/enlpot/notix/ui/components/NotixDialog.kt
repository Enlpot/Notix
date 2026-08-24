package com.enlpot.notix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.enlpot.notix.R
import com.enlpot.notix.ui.theme.NotixCorner

/**
 * 通用弹窗容器：标题栏（左侧标题 + 右侧 X 关闭）+ 自定义内容区 + 底部按钮区。
 *
 * 视觉风格与崩溃日志弹窗保持一致：自定义 Dialog 关闭平台默认窄窗口，
 * 弹窗宽度按屏幕真实 92% 生效、高度上限为屏幕 85%（过长时内容区自行滚动），
 * 半透明遮罩点击外部即关闭。
 *
 * @param buttons 底部按钮区，接收 [ColumnScope]，可自由组合并排或全宽按钮。
 *                建议危险/主操作放最下方并全宽，次要操作并排或作为列表项。
 */
@Composable
fun NotixDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
    buttons: @Composable ColumnScope.() -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismiss,
        // 关闭平台默认窄窗口，让宽度按屏幕 92% 真实生效；
        // 同时禁用系统自带的点击外部关闭，改由遮罩层自行判断，避免与弹窗内部
        // Switch/Chip 等控件的事件冲突。
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 手动补半透明遮罩（视觉效果 + 点击外部关闭）
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints {
                val maxDialogHeight = this.maxHeight * 0.85f
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .heightIn(max = maxDialogHeight)
                        // 吞掉弹窗内部点击，避免冒泡到外部遮罩导致误关闭
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = NotixCorner.Dialog,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Column {
                            content()
                            buttons()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 弹窗内部通用按钮：圆角 12dp、图标+文字、文字强制单行。
 *
 * 当一行需要并排两个按钮时，给每个按钮加 [Modifier.weight(1f)]；
 * 当一行只需要一个按钮时，使用 [Modifier.fillMaxWidth()]。
 */
@Composable
fun NotixDialogButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 弹窗内部危险按钮（红色填充）：用于删除/清空/清除等不可逆操作，文字强制单行。
 */
@Composable
fun NotixDangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String,
) {
    NotixDialogButton(
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        text = text,
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError
    )
}
