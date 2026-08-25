package com.enlpot.notix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.enlpot.notix.ui.theme.*

/**
 * 搜索 / 文本输入（DESIGN_SYSTEM.md §11）。
 * [NotixCorner.Control] + surfaceVariant 底 + outline 描边；指示器 primary。
 * 纯展示输入控件：值由参数双向注入，组件内不含业务逻辑。
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
) {
    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(NotixCorner.Control)
            .background(c.surfaceVariant)
            .border(1.dp, c.outline, NotixCorner.Control)
            .padding(horizontal = sp.md, vertical = sp.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = c.contentSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(sp.sm))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = t.body.copy(color = c.contentPrimary),
            cursorBrush = SolidColor(c.primary),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = t.body, color = c.contentSecondary)
                    }
                    innerTextField()
                }
            },
        )
    }
}
