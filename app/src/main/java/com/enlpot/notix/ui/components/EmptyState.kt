package com.enlpot.notix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.enlpot.notix.ui.theme.*

/**
 * 空状态（DESIGN_SYSTEM.md §11）。
 *
 * Stage 3：对齐到语义 Token（[MaterialTheme.notix] / [MaterialTheme.notixType]）；
 * 操作按钮改用 [PrimaryButton]。签名保持不变，现有调用方无需改动。
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String = "",
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = sp.xxl, horizontal = sp.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = c.contentTertiary,
        )
        Spacer(modifier = Modifier.height(sp.md))
        Text(
            text = title,
            style = t.display,
            textAlign = TextAlign.Center,
            color = c.contentPrimary
        )
        if (description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(sp.sm))
            Text(
                text = description,
                style = t.bodySecondary,
                textAlign = TextAlign.Center,
                color = c.contentSecondary
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(sp.xl))
            PrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}
