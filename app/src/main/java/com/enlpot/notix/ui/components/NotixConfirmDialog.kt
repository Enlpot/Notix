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
    NotixDialog(
        onDismiss = onDismiss,
        title = title,
        content = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        },
        buttons = {
            // 取消：次要、全宽、surfaceVariant 底色
            NotixDialogButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                text = cancelText,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            // 确认：主操作、全宽
            NotixDialogButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                text = confirmText,
                containerColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = if (danger) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
            )
        }
    )
}
