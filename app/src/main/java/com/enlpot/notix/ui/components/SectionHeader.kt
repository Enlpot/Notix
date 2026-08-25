package com.enlpot.notix.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enlpot.notix.ui.theme.*

/**
 * 区块标题（DESIGN_SYSTEM.md §10）。
 * sectionTitle + 可选副文；上间距取 sectionSpacing。
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing
    val lay = MaterialTheme.notixLayout

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = lay.sectionSpacing, bottom = sp.xs),
    ) {
        Text(text = title, style = t.sectionTitle, color = c.contentPrimary)
        if (subtitle != null) {
            Spacer(Modifier.height(sp.xs))
            Text(text = subtitle, style = t.bodySecondary, color = c.contentSecondary)
        }
    }
}
