package com.enlpot.notix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
 * 芯片（DESIGN_SYSTEM.md §11）。
 * 圆角 [NotixCorner.Control]；选中 primaryContainer/onPrimaryContainer，未选 surfaceVariant/contentSecondary。
 *
 * 注：§3 将 Full 列为“药丸”用途、而 §11 明确 Chip 用 Control 圆角，本组件遵循组件级规范 §11。
 * 与 Stage 2 Preview 原型（Full 形）的差异见 STAGE3_PROGRESS Open Decision。
 */
@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing
    Box(
        modifier = modifier
            .clip(NotixCorner.Control)
            .background(if (selected) c.primaryContainer else c.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = sp.md, vertical = sp.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = t.label,
            color = if (selected) c.onPrimaryContainer else c.contentSecondary,
        )
    }
}
