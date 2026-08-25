package com.enlpot.notix.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

/**
 * 二次确认弹窗：标题 + 正文 + 取消/确认两个按钮。
 *
 * 视觉风格与崩溃日志弹窗保持一致。为避免按钮文字换行，两个按钮垂直排列：
 * 上方「取消」（surfaceVariant 底色），下方「确认」（红色/主题色填充），均占满宽度。
 *
 * @param danger 为 true 时确认按钮使用 error 配色（删除/清空/清除场景）。
 */
@Composable
fun NotixConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    body: String,
    confirmText: String = stringResource(R.string.confirm),
    cancelText: String = stringResource(R.string.cancel),
    danger: Boolean = true,
) {
    val c = MaterialTheme.notix
    val sp = MaterialTheme.notixSpacing
    NotixDialog(
        onDismiss = onDismiss,
        title = title,
        content = {
            Text(
                text = body,
                style = MaterialTheme.notixType.bodySecondary,
                color = c.contentSecondary
            )
            Spacer(Modifier.height(sp.lg))
        },
        buttons = {
            // 取消：次要、全宽、surfaceVariant 底色
            NotixDialogButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                text = cancelText,
                containerColor = c.surfaceVariant,
                contentColor = c.contentPrimary
            )
            Spacer(Modifier.height(sp.sm))
            // 确认：主操作、全宽
            NotixDialogButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                text = confirmText,
                containerColor = if (danger) c.error else c.primary,
                contentColor = if (danger) c.onError else c.onPrimary
            )
        }
    )
}
