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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.enlpot.notix.R
import com.enlpot.notix.ui.theme.notix
import com.enlpot.notix.ui.theme.notixSpacing
import com.enlpot.notix.ui.theme.notixType

@Composable
fun AutoAddedRulesDialog(
    addedApps: List<String>,
    onDismiss: () -> Unit,
    onDoNotShowAgain: () -> Unit
) {
    val c = MaterialTheme.notix
    val sp = MaterialTheme.notixSpacing
    NotixDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.rules_added_automatically),
        content = {
            Text(
                text = stringResource(R.string.rules_added_automatically_desc),
                style = MaterialTheme.notixType.bodySecondary,
                color = c.contentSecondary
            )
            Spacer(modifier = Modifier.height(sp.sm))

            // Show up to 5 apps, then "+ X more"
            val displayList = if (addedApps.size > 5) addedApps.take(5) else addedApps
            displayList.forEach { appName ->
                Text(
                    text = "• $appName",
                    style = MaterialTheme.notixType.bodySecondary,
                    color = c.contentSecondary,
                    modifier = Modifier.padding(start = sp.sm, bottom = 4.dp)
                )
            }
            if (addedApps.size > 5) {
                Text(
                    text = stringResource(R.string.and_more, addedApps.size - 5),
                    style = MaterialTheme.notixType.bodySecondary,
                    color = c.contentSecondary,
                    modifier = Modifier.padding(start = sp.sm)
                )
            }

            Spacer(modifier = Modifier.height(sp.xxl))
        },
        buttons = {
            // 不再显示：次要、全宽
            NotixDialogButton(
                onClick = onDoNotShowAgain,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.do_not_show_again),
                containerColor = c.surfaceVariant,
                contentColor = c.contentPrimary
            )
            Spacer(modifier = Modifier.height(sp.sm))
            // 确定：主操作、全宽
            NotixDialogButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.ok),
                containerColor = c.primary,
                contentColor = c.onPrimary
            )
        }
    )
}
