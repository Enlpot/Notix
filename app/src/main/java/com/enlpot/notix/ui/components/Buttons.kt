package com.enlpot.notix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.enlpot.notix.ui.theme.*

/**
 * 按钮组件（DESIGN_SYSTEM.md §11）。
 *
 * 圆角 [NotixCorner.Control]，文字 [MaterialTheme.notixType.button]；
 * 颜色仅取 [MaterialTheme.notix]，无硬编码。触控目标 min 44dp。
 * 项目此前无等价组件，本阶段新建（见 STAGE3_PROGRESS）。
 */

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing
    Box(
        modifier = modifier
            .clip(NotixCorner.Control)
            .background(if (enabled) c.primary else c.contentDisabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = sp.lg, vertical = sp.sm)
            .defaultMinSize(minHeight = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = t.button,
            color = if (enabled) c.onPrimary else c.background,
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing
    Box(
        modifier = modifier
            .clip(NotixCorner.Control)
            .border(1.dp, if (enabled) c.outline else c.outlineVariant, NotixCorner.Control)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = sp.lg, vertical = sp.sm)
            .defaultMinSize(minHeight = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = t.button,
            color = if (enabled) c.primary else c.contentDisabled,
        )
    }
}

@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing
    Box(
        modifier = modifier
            .clip(NotixCorner.Control)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = sp.md, vertical = sp.sm)
            .defaultMinSize(minHeight = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = t.button,
            color = if (enabled) c.primary else c.contentDisabled,
        )
    }
}

/**
 * 图标按钮：44×44 命中区，图标色由调用方设置（建议 [MaterialTheme.notix.contentPrimary]）。
 */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .clip(NotixCorner.Full)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
