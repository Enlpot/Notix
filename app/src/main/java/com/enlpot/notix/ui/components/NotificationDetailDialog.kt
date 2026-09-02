package com.enlpot.notix.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.media.session.MediaController
import android.media.MediaMetadata
import android.media.Rating
import android.media.session.PlaybackState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.app.Notification
import android.os.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import com.enlpot.notix.NotificationBlockerService
import com.enlpot.notix.R
import com.enlpot.notix.SimpleNotification
import com.enlpot.notix.ui.theme.NotixCorner
import com.enlpot.notix.ui.theme.notix
import com.enlpot.notix.ui.theme.notixSpacing
import com.enlpot.notix.ui.theme.notixType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 单条通知详情弹窗（v7.35 抽取为可复用组件，历史列表与聚合窗口共用）。
 *
 * 高度自适应：上限窗口 80%，内容短则压缩到内容高度，超出则 80% 内滚动；
 * 标题/正文可光标选择复制；底部操作按钮（删除 / 打开 / 还原 / 创建规则）一排并排显示。
 */
@Composable
fun NotificationDetailDialog(
    notification: SimpleNotification,
    blocked: Boolean,
    showRestore: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    onCreateRule: () -> Unit,
    onRestore: (() -> Unit)? = null
) {
    val c = MaterialTheme.notix
    val sp = MaterialTheme.notixSpacing
    val packageName = notification.packageName
    val displayAppName = notification.appLabel ?: packageName.orEmpty()
    val title = notification.title.orEmpty()
    val text = notification.text.orEmpty()
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val timeStr = sdf.format(Date(notification.timestamp))
    val isOngoing = notification.wasOngoing

    // v7.14：已过滤标签使用 error 实底 + 对比度文字色（与变更计数角标一致）
    val errorColor = c.error

    // v8.6：删除通知前二次确认（与崩溃日志弹窗统一风格）
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        // v8.3：关闭平台默认窄窗口，让弹窗按屏幕宽度 90% 真实生效；
        // 同时禁用系统默认的点击外部关闭，改由遮罩层自行判断，避免与弹窗内部控件事件冲突
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // v8.3：关闭平台默认窗口后，原生 scrim 失效，这里手动补半透明遮罩
                .background(Color.Black.copy(alpha = 0.32f))
                // 点击遮罩（弹窗外部）即关闭
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
        BoxWithConstraints {
            val maxDialogHeight = this.maxHeight * 0.9f
            val maxContentHeight = this.maxHeight * 0.6f
            Surface(
                modifier = Modifier
                    // v8.3：弹窗宽度改为屏幕 90%（关闭平台窄窗口后真实生效）
                    .fillMaxWidth(0.9f)
                    .heightIn(max = maxDialogHeight)
                    // 点弹窗内部不关闭（吞掉点击，避免穿透到外部遮罩）
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = NotixCorner.Dialog,
                color = c.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 头部：图标 + 应用名 + 时间（固定）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = sp.xl, vertical = sp.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RealAppIcon(
                            packageName = packageName,
                            appName = displayAppName,
                            size = 36.dp,
                            shape = NotixCorner.Sm,
                        )
                        Spacer(modifier = Modifier.width(sp.md))
                        Column {
                            Text(
                                text = displayAppName,
                                style = MaterialTheme.typography.titleMedium,
                                color = c.contentPrimary
                            )
                            Text(
                                text = timeStr,
                                style = MaterialTheme.notixType.caption,
                                color = c.contentSecondary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isOngoing) "常驻通知" else "普通通知",
                                    style = MaterialTheme.notixType.caption,
                                    color = c.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (blocked) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    // v8.51.0：「已过滤」tag 改纯漏斗图标（去文字）
                                    Icon(
                                        imageVector = Icons.Filled.FilterAlt,
                                        contentDescription = stringResource(R.string.history_blocked_badge),
                                        tint = errorColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = c.outlineVariant)
                    // 内容区：上限 80% 高度内可滚动（内容短时压缩到内容高度）+ 光标拖动选择复制
                    Column(
                        modifier = Modifier
                            .heightIn(max = maxContentHeight)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = sp.xl, vertical = sp.md)
                    ) {
                        SelectionContainer {
                            Column {
                                if (title.isNotEmpty()) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.notixType.body,
                                        fontWeight = FontWeight.SemiBold,
                                        color = c.contentPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                if (text.isNotEmpty()) {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.notixType.bodySecondary,
                                        color = c.contentSecondary
                                    )
                                }
                                // v8.43.0：显示通知渠道和 sbnKey
                                val channelIdStr = notification.channelId
                                val sbnKeyStr = notification.sbnKey
                                if (channelIdStr != null || sbnKeyStr != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = c.outlineVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (channelIdStr != null) {
                                        Text(
                                            text = "渠道：$channelIdStr",
                                            style = MaterialTheme.notixType.caption,
                                            color = c.contentSecondary
                                        )
                                    }
                                    if (sbnKeyStr != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "sbnKey：$sbnKeyStr",
                                            style = MaterialTheme.notixType.caption,
                                            color = c.contentSecondary
                                        )
                                    }
                                }
                                // v8.53.1：媒体播放控制（方案A）——当前在通知栏的媒体通知显示 上一首/播放暂停/下一首
                                if (notification.isActive && sbnKeyStr != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = c.outlineVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    MediaControlsSection(sbnKey = sbnKeyStr, packageName = packageName)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                // v8.51.0：当前状态（取消原因优先：规则命中/系统原因 > 正显示 > 已结束）
                                // v8.55.1：媒体正在播放 → 定制"正在播放"状态（优先于取消原因，解决系统收纳媒体通知后误显示"应用取消"）
                                val statusText = when {
                                    packageName != null &&
                                        (NotificationBlockerService.instance?.isPackageMediaPlaying(packageName) == true) ->
                                        stringResource(R.string.notification_status_playing)
                                    notification.cancelReason != null -> cancelReasonText(notification.cancelReason!!)
                                    notification.isActive -> stringResource(R.string.notification_status_active)
                                    else -> stringResource(R.string.notification_status_ended)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.notification_status_label, statusText),
                                    style = MaterialTheme.notixType.caption,
                                    color = c.contentSecondary
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = c.outlineVariant)
                    // 底部按钮区：上排「删除/打开/还原」三个次级按钮，下排「创建规则」主题色主按钮
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = sp.xl, vertical = sp.md)
                    ) {
                        // 上排：删除 / 打开 / 还原（三个等宽次级按钮）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(sp.sm, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 删除：保留破坏性语义，但用 error 文字色而非实心红块
                            NotixDialogButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.notification_delete),
                                containerColor = c.surfaceVariant,
                                contentColor = errorColor
                            )
                            // 打开：次级按钮
                            NotixDialogButton(
                                onClick = {
                                    onDismiss()
                                    onOpen()
                                },
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.notification_open),
                                containerColor = c.surfaceVariant,
                                contentColor = c.contentSecondary
                            )
                            // 还原：次级按钮
                            NotixDialogButton(
                                onClick = {
                                    onDismiss()
                                    onRestore?.invoke()
                                },
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.notification_restore),
                                containerColor = c.surfaceVariant,
                                contentColor = c.contentSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(sp.sm))
                        // 下排：创建规则（主题色主按钮，单独一行、整宽）
                        NotixDialogButton(
                            onClick = {
                                onDismiss()
                                onCreateRule()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.notification_create_rule),
                            containerColor = c.primary,
                            contentColor = c.onPrimary
                        )
                    }
                }
            }
        }
        }
    }

    if (showDeleteConfirm) {
        NotixConfirmDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDismiss()
                onDelete()
            },
            title = stringResource(R.string.delete_item_title),
            body = stringResource(R.string.delete_item_confirm, displayAppName),
            confirmText = stringResource(R.string.delete),
            danger = true
        )
    }
}

/**
 * v8.51.0：通知取消原因 code -> 中文文案。
 * 与 [NotificationListenerService] 的 REASON_* 常量（公开 SDK 三参回调 int reason）对应；
 * 100 = 自定义"规则命中"。
 */
@Composable
private fun cancelReasonText(reason: Int): String = when (reason) {
    1 -> stringResource(R.string.reason_click)
    2 -> stringResource(R.string.reason_cancel)
    3 -> stringResource(R.string.reason_cancel_all)
    4 -> stringResource(R.string.reason_error)
    5 -> stringResource(R.string.reason_package_changed)
    6 -> stringResource(R.string.reason_user_stopped)
    7 -> stringResource(R.string.reason_package_banned)
    8 -> stringResource(R.string.reason_app_cancel)
    9 -> stringResource(R.string.reason_app_cancel_all)
    10 -> stringResource(R.string.reason_listener_cancel)
    11 -> stringResource(R.string.reason_listener_cancel_all)
    12 -> stringResource(R.string.reason_group_summary_canceled)
    13 -> stringResource(R.string.reason_group_optimization)
    14 -> stringResource(R.string.reason_package_suspended)
    15 -> stringResource(R.string.reason_profile_turned_off)
    16 -> stringResource(R.string.reason_unautobundled)
    17 -> stringResource(R.string.reason_channel_banned)
    18 -> stringResource(R.string.reason_snoozed)
    19 -> stringResource(R.string.reason_timeout)
    20 -> stringResource(R.string.reason_channel_removed)
    21 -> stringResource(R.string.reason_clear_data)
    22 -> stringResource(R.string.reason_assistant_cancel)
    23 -> stringResource(R.string.reason_lockdown)
    100 -> stringResource(R.string.reason_rule_hit)
    else -> stringResource(R.string.reason_unknown)
}


/**
 * v8.53.2/v8.54.1：通知 action 按钮 + 播放进度条。
 * 从 Service 缓存取该 sbnKey 的 Notification.Action 列表，渲染可点击按钮（如网易云 喜欢/上一首/播放/下一首/词）。
 * 点击等价于点系统通知上的 action 按钮（PendingIntent.send）。
 * 另连接 MediaSession 只读播放状态：
 *  - 播放/暂停按钮按状态切换 Pause/PlayArrow 图标
 *  - 显示可拖动进度条（seekTo）
 * token 缓存可能因服务重启清空，打开弹窗后每 1 秒轮询重试拿 token（弹窗关闭即停）。
 * 无 action 时返回空 UI；无 token/时长时不显示进度条。
 */
@Composable
private fun MediaControlsSection(sbnKey: String?, packageName: String?) {
    val context = LocalContext.current
    val c = MaterialTheme.notix
    var actions by remember { mutableStateOf<List<Notification.Action>?>(null) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var playing by remember { mutableStateOf(false) }
    var durationMs by remember { mutableStateOf(0L) }
    var positionMs by remember { mutableStateOf(0L) }
    var callback by remember { mutableStateOf<MediaController.Callback?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    // v8.55.2：当前歌曲是否已标红心（来自 PlaybackState.customActions 推断，实测后校准）
    var liked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 1) 拿 actions + 建立 MediaController（refreshKey 变化时重连）
    LaunchedEffect(sbnKey, packageName, refreshKey) {
        if (sbnKey == null && packageName == null) return@LaunchedEffect
        actions = if (sbnKey != null) try {
            NotificationBlockerService.instance?.getNotificationActions(sbnKey)?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) { null } else null

        // v8.55.0 方案B：优先主动枚举活跃 MediaSession（getActiveSessions），不依赖通知 posted 缓存——
        // ColorOS 收纳媒体通知后会 removed 清空 token 缓存，但 MediaSession 仍活着，仍可枚举控制。
        // 拿不到再回退到 token 缓存；都拿不到则 1s 轮询重试（弹窗关闭自动取消）。
        var mc: MediaController? = null
        while (mc == null) {
            if (packageName != null) {
                mc = try {
                    NotificationBlockerService.instance?.getActiveMediaController(packageName)
                } catch (e: Exception) { null }
            }
            if (mc == null && sbnKey != null) {
                val token = try {
                    NotificationBlockerService.instance?.getMediaSessionToken(sbnKey)
                } catch (e: Exception) { null }
                if (token != null) {
                    mc = try { MediaController(context.applicationContext, token) } catch (e: Exception) { null }
                }
            }
            if (mc == null) delay(1000)
        }
        val media = mc
        val cb = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                playing = state?.state == PlaybackState.STATE_PLAYING
                positionMs = state?.position ?: 0L
            }
        }
        try {
            media.registerCallback(cb)
            callback = cb
            controller = media
            val st = media.playbackState
            playing = st?.state == PlaybackState.STATE_PLAYING
            positionMs = st?.position ?: 0L
            durationMs = try {
                media.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            } catch (e: Exception) { 0L }
            readLikedState(media)?.let { liked = it }
        } catch (e: Exception) { }
    }

    // 2) 定时主动读播放状态 + 推进进度（1s；不依赖 callback，覆盖 session 更新/重建场景）
    LaunchedEffect(controller, playing) {
        val mc = controller ?: return@LaunchedEffect
        val speed = try { mc.playbackState?.playbackSpeed?.toFloat() ?: 1f } catch (e: Exception) { 1f }
        while (isActive) {
            val st = try { mc.playbackState } catch (e: Exception) { null }
            if (st != null) {
                playing = st.state == PlaybackState.STATE_PLAYING
                if (st.position != null) positionMs = st.position
            }
            // v8.55.2：定时同步红心状态（customActions 变化时自动切换）
            readLikedState(mc)?.let { liked = it }
            if (playing && durationMs > 0) {
                positionMs = (positionMs + (speed * 1000).toLong()).coerceAtMost(durationMs)
            }
            delay(1000)
        }
    }

    DisposableEffect(controller, callback) {
        onDispose {
            // 只做资源清理；不要在此置空 state（controller/callback 由 remember 生命周期管理，
            // 置 null 会导致重连瞬间进度条消失、弹窗闪一下）
            callback?.let { controller?.unregisterCallback(it) }
        }
    }

    val actList = actions ?: return
    Column {
        // 进度条（有 controller 且 duration>0 才显示）
        if (controller != null && durationMs > 0) {
            var dragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableStateOf(0L) }
            val shown = if (dragging) dragValue else positionMs.coerceIn(0L, durationMs)
            // v8.55.1：细轨道+小圆点进度条（原 Material Slider 大圆球上下过高）
            MediaProgressBar(
                positionMs = shown,
                durationMs = durationMs,
                onPreview = { dragging = true; dragValue = it },
                onSeek = { target ->
                    dragging = false
                    runCatching { controller?.transportControls?.seekTo(target) }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(shown), style = MaterialTheme.notixType.caption)
                Text(formatDuration(durationMs), style = MaterialTheme.notixType.caption)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actList.forEach { action ->
                val label = action.title?.toString().orEmpty()
                // 按 action.title 映射 Material 图标（跨 app 资源图标运行时无法加载）。
                // 未知动作回退文字按钮。
                val iconVector = mediaActionIcon(label)
                if (iconVector != null) {
                    // toggle：按播放状态切换 播放/暂停 图标
                    val t = label.lowercase()
                    val isToggle = t.contains("toggle") || t.contains("播放") || t.contains("暂停")
                        || t.contains("play") || t.contains("pause")
                    val showVector = if (isToggle && playing) Icons.Filled.Pause
                    else if (isToggle) Icons.Filled.PlayArrow
                    else iconVector
                    // v8.55.2：红心按钮——已标红心填充红色，未标保持主题色
                    val isLike = iconVector == Icons.Filled.Favorite
                    val iconTint = if (isLike && liked) Color(0xFFE53935) else c.contentPrimary
                    Button(
                        onClick = {
                            // v8.55.2：红心本地乐观切换（立即反馈），随后重连校准
                            if (isLike) liked = !liked
                            runCatching { action.actionIntent?.send(context, 0, null) }
                            // 点击后稍等片刻强制重连，读取最新播放状态（覆盖 session 更新/重建场景）
                            scope.launch {
                                delay(700)
                                refreshKey++
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = c.surfaceVariant,
                            contentColor = c.contentPrimary
                        )
                    ) {
                        Icon(
                            imageVector = showVector,
                            contentDescription = label,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (label.isNotBlank()) {
                    Button(
                        onClick = {
                            runCatching { action.actionIntent?.send(context, 0, null) }
                            scope.launch {
                                delay(700)
                                refreshKey++
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = c.surfaceVariant,
                            contentColor = c.contentPrimary
                        )
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.notixType.caption,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * v8.55.2：从 MediaController 判断当前歌曲是否已标红心。
 * 依据 PlaybackState.customActions：存在"取消喜欢"类动作 → 已标（true）；存在"喜欢"类动作 → 未标（false）；
 * 两者都没有 → null（无法判断，保持现状）。
 * 说明：无标准 API，此为平台惯例推断，真实匹配规则可能因音乐 app 而异，真机实测后校准。
 */
private fun readLikedState(mc: MediaController): Boolean? {
    return runCatching {
        // v8.55.3：首选标准 API——MediaMetadata USER_RATING + HeartRating（无歧义，多数音乐 app 遵循）
        val rating = mc.metadata?.getRating(MediaMetadata.METADATA_KEY_USER_RATING)
        if (rating != null && rating.ratingStyle == Rating.RATING_HEART) {
            return@runCatching rating.isRated && rating.hasHeart()
        }
        // 兜底：PlaybackState.customActions 推断（常见命名：like/unlike、favorite、heart、thumbs_up、save_to_favorites 等）
        val strs = (mc.playbackState?.customActions ?: emptyList()).map { it.action.lowercase() }
        val hasUnlike = strs.any {
            it.contains("unlike") || it.contains("unfavorite") || it.contains("unheart") ||
            it.contains("unmark") || it.contains("cancel_like") || it.contains("cancel_love") ||
            it.contains("remove_favorite") || it.contains("dislike")
        }
        if (hasUnlike) return@runCatching true
        val hasLike = strs.any {
            it.contains("like") || it.contains("favorite") || it.contains("heart") ||
            it.contains("mark") || it.contains("love") || it.contains("thumbs") ||
            it.contains("save_to_favorites") || it.contains("star")
        }
        if (hasLike) return@runCatching false
        null
    }.getOrNull()
}

/**
 * v8.55.1：细轨道（3dp）+ 小圆点（8dp）可拖动进度条。
 * 设计参考：YouTube Music / 主流播放器「细线 + 圆点」风格，比 Material Slider 默认大圆球更紧凑。
 * onPreview：拖动过程更新本地显示值（不触发 seek）；onSeek：点击/松手时提交 seekTo。
 */
@Composable
private fun MediaProgressBar(
    positionMs: Long,
    durationMs: Long,
    onPreview: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.notix
    val total = durationMs.coerceAtLeast(1L)
    val fraction = (positionMs.toFloat() / total).coerceIn(0f, 1f)
    var lastDrag by remember { mutableStateOf(positionMs) }
    Canvas(
        modifier = modifier
            .height(24.dp)
            .pointerInput(total) {
                fun seekValue(x: Float): Long {
                    val r = (x / size.width).coerceIn(0f, 1f)
                    return (r * total).toLong().coerceIn(0L, durationMs)
                }
                fun preview(x: Float) {
                    val v = seekValue(x)
                    lastDrag = v
                    onPreview(v)
                }
                detectTapGestures { offset ->
                    preview(offset.x)
                    onSeek(lastDrag)
                }
                detectHorizontalDragGestures(
                    onDragStart = { offset -> preview(offset.x) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        preview(change.position.x)
                    },
                    onDragEnd = { onSeek(lastDrag) },
                    onDragCancel = {},
                )
            }
    ) {
        val trackH = 3.dp.toPx()
        val thumbR = 4.dp.toPx()
        val trackY = size.height / 2f
        val x = size.width * fraction
        drawRoundRect(
            color = c.surfaceVariant,
            topLeft = Offset(0f, trackY - trackH / 2),
            size = Size(size.width, trackH),
            cornerRadius = CornerRadius(trackH / 2)
        )
        drawRoundRect(
            color = c.primary,
            topLeft = Offset(0f, trackY - trackH / 2),
            size = Size(x, trackH),
            cornerRadius = CornerRadius(trackH / 2)
        )
        drawCircle(color = c.primary, radius = thumbR, center = Offset(x, trackY))
    }
}

/** v8.54.1：毫秒转 mm:ss。 */
private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

/** v8.53.2：把常见的通知 action 标题映射为 Material 图标；无法识别返回 null（回退文字）。 */
private fun mediaActionIcon(label: String): ImageVector? {
    val t = label.lowercase().trim()
    return when {
        t.contains("like") || t.contains("heart") || t.contains("favorite")
        || t.contains("喜") || t.contains("赞") -> Icons.Filled.Favorite
        t.contains("pre") || t.contains("previous") || t.contains("上一") -> Icons.Filled.SkipPrevious
        t.contains("next") || t.contains("下一") -> Icons.Filled.SkipNext
        t.contains("toggle") || t.contains("play") || t.contains("pause")
        || t.contains("播放") || t.contains("暂停") -> Icons.Filled.PlayArrow
        t.contains("lyric") || t.contains("词") -> Icons.Filled.MusicNote
        else -> null
    }
}

