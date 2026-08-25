package com.enlpot.notix.ui.components

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.enlpot.notix.R
import com.enlpot.notix.ui.theme.notix
import com.enlpot.notix.ui.theme.notixSpacing
import com.enlpot.notix.ui.theme.notixType

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    val appName = context.getString(context.applicationInfo.labelRes)
    val appVersion = packageInfo?.versionName ?: stringResource(R.string.not_applicable)
    val developerEmail = "aj@Notix.com"

    val c = MaterialTheme.notix
    val sp = MaterialTheme.notixSpacing
    NotixDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.about),
        content = {
            Column {
                Text(
                    text = stringResource(R.string.about_app, appName),
                    style = MaterialTheme.notixType.bodySecondary,
                    color = c.contentSecondary
                )
                Spacer(Modifier.height(sp.sm))
                Text(
                    text = stringResource(R.string.about_version, appVersion),
                    style = MaterialTheme.notixType.bodySecondary,
                    color = c.contentSecondary
                )
                Spacer(Modifier.height(sp.sm))
                Text(
                    text = stringResource(R.string.about_developer, developerEmail),
                    style = MaterialTheme.notixType.bodySecondary,
                    color = c.contentSecondary
                )
            }
            Spacer(Modifier.height(sp.lg))
        },
        buttons = {
            NotixDialogButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.close)
            )
        }
    )
}
