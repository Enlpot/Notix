package com.enlpot.notix.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.enlpot.notix.AppInfoStorage
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// v7.44：进程内图标内存缓存——滑动切 tab / 滚动回收后不再重复 PackageManager 加载，消除卡片延迟出现
private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
private const val ICON_CACHE_MAX = 256

/**
 * Unified real app icon:
 * 1. Launcher icon via PackageManager.getApplicationIcon (real colored icon)
 * 2. Fallback: icon stored by the notification listener (AppInfoStorage)
 * 3. Last resort: first letter of the app name
 */
@Composable
fun RealAppIcon(
    packageName: String?,
    appName: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
) {
    val context = LocalContext.current
    val appInfoStorage = remember { AppInfoStorage(context) }
    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
        value = withContext(Dispatchers.IO) {
            if (packageName == null) {
                null
            } else {
                // v7.44：命中缓存直接返回，避免重复加载
                iconCache[packageName] ?: run {
                    val launcher = try {
                        context.packageManager
                            .getApplicationIcon(packageName)
                            .toBitmap(96, 96)
                            .asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                    val icon = launcher ?: appInfoStorage.getAppIcon(packageName)?.asImageBitmap()
                    if (icon != null) {
                        if (iconCache.size >= ICON_CACHE_MAX) iconCache.clear()
                        iconCache[packageName] = icon
                    }
                    icon
                }
            }
        }
    }

    val letterSource = appName?.trim()?.take(1)?.uppercase()
        ?: packageName?.trim()?.take(1)?.uppercase()
        ?: "?"

    if (icon != null) {
        Image(
            bitmap = icon!!,
            contentDescription = null,
            modifier = modifier.size(size).clip(shape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letterSource,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
