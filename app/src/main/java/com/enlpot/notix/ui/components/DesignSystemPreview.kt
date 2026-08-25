package com.enlpot.notix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.enlpot.notix.ui.theme.*

/**
 * Notix Design System 独立展示页（Stage 2 交付物）。
 *
 * ⚠️ 本文件仅用于预览令牌与核心组件规范，不接入正式 App 导航，
 * 不改变任何用户现有页面（见 stage2.md §18）。
 */

@Composable
private fun DsSwatch(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(NotixCorner.Control)
                .background(color)
                .border(1.dp, MaterialTheme.notix.outlineVariant, NotixCorner.Control)
        )
        Spacer(Modifier.height(4.dp))
        Text(name, style = NotixTypographyTokens.caption, color = MaterialTheme.notix.contentSecondary)
    }
}

@Composable
private fun DsSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.notixLayout.sectionSpacing / 2)
    ) {
        Text(title, style = NotixTypographyTokens.sectionTitle, color = MaterialTheme.notix.contentPrimary)
        Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
        content()
    }
}

@Composable
private fun DesignSystemContent() {
    val s = MaterialTheme.notix
    val sp = MaterialTheme.notixSpacing
    val lay = MaterialTheme.notixLayout

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(s.background)
            .padding(lay.screenHorizontal)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Notix Design System", style = NotixTypographyTokens.display, color = s.contentPrimary)

        DsSection("Colors — Semantic") {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(sp.sm),
                verticalArrangement = Arrangement.spacedBy(sp.sm),
                modifier = Modifier.height(260.dp)
            ) {
                item { DsSwatch("primary", s.primary) }
                item { DsSwatch("onPrimary", s.onPrimary) }
                item { DsSwatch("surface", s.surface) }
                item { DsSwatch("surfaceVar", s.surfaceVariant) }
                item { DsSwatch("elevated", s.surfaceElevated) }
                item { DsSwatch("outline", s.outline) }
                item { DsSwatch("success", s.success) }
                item { DsSwatch("warning", s.warning) }
                item { DsSwatch("error", s.error) }
                item { DsSwatch("info", s.info) }
                item { DsSwatch("content1", s.contentPrimary) }
                item { DsSwatch("content2", s.contentSecondary) }
            }
        }

        DsSection("Typography") {
            val samples = listOf(
                "display 28/B" to NotixTypographyTokens.display,
                "screenTitle 22/M" to NotixTypographyTokens.screenTitle,
                "sectionTitle 16/SB" to NotixTypographyTokens.sectionTitle,
                "cardTitle 14/SB" to NotixTypographyTokens.cardTitle,
                "body 16/N" to NotixTypographyTokens.body,
                "bodySecondary 14/N" to NotixTypographyTokens.bodySecondary,
                "label 12/M" to NotixTypographyTokens.label,
                "button 14/M" to NotixTypographyTokens.button,
                "caption 12/N" to NotixTypographyTokens.caption,
                "numeric 14/SB" to NotixTypographyTokens.numeric,
            )
            Column(verticalArrangement = Arrangement.spacedBy(sp.xs)) {
                samples.forEach { (label, style) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = NotixTypographyTokens.caption, color = s.contentTertiary, modifier = Modifier.size(width = 140.dp, height = 20.dp))
                        Text("通知管理 Notix 示例文字", style = style, color = s.contentPrimary)
                    }
                }
            }
        }

        DsSection("Shape") {
            Row(horizontalArrangement = Arrangement.spacedBy(sp.md)) {
                listOf("Dialog" to NotixCorner.Dialog, "Card" to NotixCorner.Card, "ListItem" to NotixCorner.ListItem, "Control" to NotixCorner.Control)
                    .forEach { (name, shape) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(56.dp).clip(shape).background(s.surfaceVariant))
                            Spacer(Modifier.height(4.dp))
                            Text(name, style = NotixTypographyTokens.caption, color = s.contentSecondary)
                        }
                    }
            }
        }

        DsSection("Buttons") {
            Row(horizontalArrangement = Arrangement.spacedBy(sp.md), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(NotixCorner.Control).background(s.primary).padding(horizontal = sp.lg, vertical = sp.sm)) {
                    Text("Primary", style = NotixTypographyTokens.button, color = s.onPrimary)
                }
                Box(
                    Modifier
                        .clip(NotixCorner.Control)
                        .border(1.dp, s.outline)
                        .padding(horizontal = sp.lg, vertical = sp.sm)
                ) { Text("Secondary", style = NotixTypographyTokens.button, color = s.primary) }
                TextButton(text = "Text", onClick = { })
            }
        }

        DsSection("Notification Card (prototype)") {
            // 动态卡示意：accent 底 + chooseTextColor 文字（此处以 primary 作 accent）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NotixCorner.Card)
                    .background(s.primary)
                    .padding(lay.cardPadding)
            ) {
                Column {
                    Text("天气预警", style = NotixTypographyTokens.cardTitle, color = s.onPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(sp.xs))
                    Text("今天傍晚有雷阵雨，请注意", style = NotixTypographyTokens.bodySecondary, color = s.onPrimary)
                }
            }
        }

        DsSection("Rule Card (prototype · 新层级)") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NotixCorner.Card)
                    .background(s.surfaceElevated)
                    .border(1.dp, s.outlineVariant, NotixCorner.Card)
                    .padding(lay.cardPadding)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(sp.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(NotixCorner.Sm).background(s.surfaceVariant))
                        Spacer(Modifier.width(sp.sm))
                        Text("Shell", style = NotixTypographyTokens.cardTitle, color = s.contentPrimary)
                        Spacer(Modifier.weight(1f))
                        Switch(checked = true, onCheckedChange = { })
                    }
                    Text("匹配：包含任一关键字 “天气预警”", style = NotixTypographyTokens.bodySecondary, color = s.contentSecondary)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(s.outlineVariant))
                    Text("动作：移除通知（含常驻冻结 7 天）", style = NotixTypographyTokens.cardTitle, color = s.primary, fontWeight = FontWeight.SemiBold)
                    Text("命中 12 次", style = NotixTypographyTokens.caption, color = s.contentTertiary)
                }
            }
        }

        DsSection("Controls") {
            Row(horizontalArrangement = Arrangement.spacedBy(sp.sm), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(NotixCorner.Full).background(s.primaryContainer).padding(horizontal = sp.md, vertical = sp.xs)) {
                    Text("Chip", style = NotixTypographyTokens.label, color = s.onPrimaryContainer)
                }
                Box(Modifier.clip(NotixCorner.Full).background(s.surfaceVariant).padding(horizontal = sp.md, vertical = sp.xs)) {
                    Text("未选", style = NotixTypographyTokens.label, color = s.contentSecondary)
                }
                Switch(checked = true, onCheckedChange = { })
            }
        }

        DsSection("Empty State") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = sp.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(64.dp).clip(NotixCorner.Full).background(s.surfaceVariant))
                Spacer(Modifier.height(sp.sm))
                Text("暂无通知", style = NotixTypographyTokens.display, color = s.contentPrimary)
                Text("新通知会出现在这里", style = NotixTypographyTokens.bodySecondary, color = s.contentSecondary)
            }
        }

        Spacer(Modifier.height(sp.xxl))
    }
}

@Preview(name = "Notix DS — Light", showBackground = true)
@Composable
fun DesignSystemPreview_Light() {
    NotixTheme(darkTheme = false) { DesignSystemContent() }
}

@Preview(name = "Notix DS — Dark", showBackground = true)
@Composable
fun DesignSystemPreview_Dark() {
    NotixTheme(darkTheme = true) { DesignSystemContent() }
}

/**
 * Stage 3 组件库展示（纯展示组件，仅用语义 Token）。
 * 非 private，供临时 Preview 入口（MainActivity）在运行时实渲染验证后移除。
 */
@Composable
fun ComponentShowcaseContent() {
    val c = MaterialTheme.notix
    val t = MaterialTheme.notixType
    val sp = MaterialTheme.notixSpacing
    val lay = MaterialTheme.notixLayout

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.background)
            .padding(lay.screenHorizontal)
    ) {
        Text("Components", style = t.display, color = c.contentPrimary)

        DsSection("Notification Card — Normal") {
            NotificationCard(
                data = NotificationCardData(
                    appName = "天气",
                    title = "雷阵雨预警",
                    summary = "今天傍晚有雷阵雨，请注意携带雨具",
                    timestamp = "08-25 10:30",
                    count = 1,
                ),
                accent = c.primary,
                onAccent = c.onPrimary,
                packageName = null,
            )
        }

        DsSection("Notification Card — Multiple") {
            NotificationCard(
                data = NotificationCardData(
                    appName = "微信",
                    title = "群消息",
                    summary = "你收到了多条新消息",
                    timestamp = "08-25 10:28",
                    count = 5,
                ),
                accent = c.success,
                onAccent = c.onSuccess,
                packageName = null,
                variant = NotificationCardVariant.Multiple,
            )
        }

        DsSection("Rule Card — 视觉层级") {
            RuleCard(
                appName = "Shell",
                packageName = null,
                conditionText = "匹配：包含任一关键字 “天气预警”",
                actionText = "动作：移除通知（含常驻冻结 7 天）",
                hitCount = 12,
                enabled = true,
            )
            Spacer(Modifier.height(sp.md))
            RuleCard(
                appName = "微博",
                packageName = null,
                conditionText = "匹配：来自指定频道",
                actionText = "动作：静音 30 分钟",
                hitCount = 3,
                enabled = false,
            )
        }

        DsSection("Setting Row") {
            SettingRow(icon = Icons.Default.Settings, title = "通知监听", subtitle = "已开启", onClick = {})
            SettingRow(
                icon = Icons.Default.Delete,
                title = "清除历史",
                subtitle = "不可恢复",
                destructive = true,
                onClick = {},
            )
            SettingRow(
                icon = Icons.Default.Notifications,
                title = "普通项",
                trailing = { Switch(checked = true, onCheckedChange = {}) },
            )
        }

        DsSection("Section Header") {
            SectionHeader(title = "历史记录", subtitle = "最近 30 天")
        }

        DsSection("Buttons") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(sp.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryButton("Primary", onClick = {})
                SecondaryButton("Secondary", onClick = {})
                TextButton("Text", onClick = {})
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = c.contentPrimary)
                }
            }
        }

        DsSection("Chips") {
            Row(horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                Chip("已选", selected = true, onClick = {})
                Chip("未选", selected = false, onClick = {})
            }
        }

        DsSection("Search") {
            SearchField(value = "", onValueChange = {}, placeholder = "搜索通知")
        }

        DsSection("Empty State") {
            EmptyState(
                icon = Icons.Default.Notifications,
                title = "暂无通知",
                description = "新通知会出现在这里",
            )
        }

        Spacer(Modifier.height(sp.xxl))
    }
}

@Preview(name = "Notix Components — Light", showBackground = true)
@Composable
fun ComponentPreview_Light() {
    NotixTheme(darkTheme = false) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            ComponentShowcaseContent()
        }
    }
}

@Preview(name = "Notix Components — Dark", showBackground = true)
@Composable
fun ComponentPreview_Dark() {
    NotixTheme(darkTheme = true) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            ComponentShowcaseContent()
        }
    }
}
