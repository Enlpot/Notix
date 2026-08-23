package com.enlpot.notix.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.enlpot.notix.R

/**
 * 删除确认弹窗（兼容旧 API）。
 * 内部已迁移到统一的 [NotixConfirmDialog]，保持与崩溃日志弹窗一致的视觉风格。
 */
@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    NotixConfirmDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        title = stringResource(R.string.delete_item_title),
        body = stringResource(R.string.delete_item_confirm, itemName),
        confirmText = stringResource(R.string.delete),
        danger = true
    )
}
