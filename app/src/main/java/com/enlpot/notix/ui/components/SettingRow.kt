package com.enlpot.notix.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.enlpot.notix.ui.theme.*

/**
 * 设置行（DESIGN_SYSTEM.md §10 / §11）。
 *
 * 图标(24) + 标题(cardTitle) + 副文(bodySecondary) + 尾部控件；整行可点击。
 * [destructive] 变体用 error 文字，供清除历史等危险项与常规项做视觉分级。
 * 触控目标 min 44dp（见 §16）。
 */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    destructive: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing

    val titleColor = if (destructive) c.error else c.contentPrimary
    val iconColor = if (destructive) c.error else c.contentSecondary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = sp.md, horizontal = sp.lg)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(sp.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = t.cardTitle, color = titleColor)
            if (subtitle != null) {
                Spacer(Modifier.height(sp.xs))
                Text(text = subtitle, style = t.bodySecondary, color = c.contentSecondary)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(sp.md))
            trailing()
        }
    }
}
