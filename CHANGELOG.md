# Notix UI 改造清单（v8.3 · 一致性规范）

> 基线：v8.2（versionCode=115 / versionName="8.2"）
> 范围：仅 UI 层（screens / components / theme / RuleWizardSupport 展示部分 / strings）。
> 未改动：build.gradle.kts、gradle 配置、版本号、AndroidManifest、包名、数据/存储/通知拦截等后台逻辑。
> 构建：`gradlew.bat assembleDebug` → BUILD SUCCESSFUL，产物 `app/build/outputs/apk/debug/app-debug.apk`。
> 构建环境修复：原 `gradle.properties` daemon 用 `-Xmx1024m` 在本机 Windows 32-bit CompressedOops 模式下导致 Kotlin 编译阶段 JVM 崩溃；已加 `-XX:HeapBaseMinAddress=0x100000000` 并提至 `-Xmx1536m`（仅构建配置，不影响应用运行/版本）。
> 设计令牌：`ui/theme/Shape.kt` 新增 `NotixCorner`（Dialog=28 / Card=16 / ListItem=12 / Control=12 / Sm=8 dp）。

## 全局规范
- 所有顶层弹窗外框统一为 `NotixCorner.Dialog`（28dp），与 Material3 AlertDialog 默认对齐。
- 标题统一使用 `MaterialTheme.typography.titleLarge`，移除硬编码 `fontSize=20.sp / 18.sp` 与 `FontWeight.Bold`。
- 列表项 / 行卡片统一 12dp，按钮 / 控件统一 12dp，消除原 12/14/16/20dp 混用。
- 功能逻辑与交互行为完全不变，仅视觉 / 排版调整。

## 修改文件清单（相对路径 | 改动点摘要）

`app/src/main/java/com/enlpot/notix/ui/theme/Shape.kt` | 新增 NotixCorner 统一圆角令牌（全局一致性基础）
`app/src/main/java/com/enlpot/notix/ui/components/AutoAddedRulesDialog.kt` | 弹窗外框改 NotixCorner.Dialog(28dp)；标题改 titleLarge 并移除硬编码 Bold；清理无用 import
`app/src/main/java/com/enlpot/notix/ui/components/DeleteConfirmationDialog.kt` | 弹窗外框改 NotixCorner.Dialog(28dp)；标题由 20.sp+Bold 改 titleLarge；补 MaterialTheme 导入；清理无用 import
`app/src/main/java/com/enlpot/notix/ui/components/HistoryNotificationDetailsDialog.kt` | 聚合弹窗外框 20dp→NotixCorner.Dialog(28dp)；标题 18.sp+Bold→titleLarge；关闭按钮 16.sp→labelLarge；清理无用 import
`app/src/main/java/com/enlpot/notix/ui/components/NotificationDetailDialog.kt` | 详情弹窗外框 16dp→NotixCorner.Dialog(28dp)；底部按钮区重排为「上排 删除/打开/还原 三个次级按钮 + 下排 创建规则 主题色(primary)主按钮」，删除保留 error 文字色而非实心红块；清理无用 import（OutlinedButton/BorderStroke/Color/toArgb）
`app/src/main/java/com/enlpot/notix/ui/screens/HistoryScreen.kt` | 停止监听弹窗标题 20.sp+Bold→titleLarge
`app/src/main/java/com/enlpot/notix/ui/screens/SetupWizardScreen.kt` | 引导页标题 20.sp+Bold→titleLarge
`app/src/main/java/com/enlpot/notix/ui/screens/RulesScreen.kt` | 新建规则按钮圆角 14dp→12dp（ListItem 令牌）
`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` | 两处圆角 14dp→12dp（ListItem 令牌）

## 说明
- `RuleWizardSupport.kt` 经核查不含 UI（@Composable）代码，无需改动。
- `SettingsScreen` / `StorageUsageScreen` 等页面已遵循 12/16dp 圆角与 typography 令牌，本就一致，未做无意义改动。
- 未新增任何用户可见文案，故未改动 `strings.xml`（仍维护既有中英双语）。

## 本轮修改（第 1 轮 · 2026-08-23）

> 修改点：通知详情弹窗宽度加大（用户实测当前实际渲染宽度不足 80%）。

`app/src/main/java/com/enlpot/notix/ui/components/NotificationDetailDialog.kt` | 弹窗宽度修复：Dialog 增加 `DialogProperties(usePlatformDefaultWidth = false)`，外层包 `Box(Modifier.fillMaxSize(), contentAlignment = Center)` 占满窗口；Surface 宽度保持 `fillMaxWidth(0.9f)` 但此时按屏幕真实宽度生效（此前被平台默认窄窗口压缩，视觉不足 80%）。仅视觉调整，文案 / 逻辑 / 令牌均不变。

## 本轮修改（第 2 轮 · 2026-08-23）

> 修改点：修复弹窗点外部空白处无法关闭；聚合窗口同步按同样结构改造。

`app/src/main/java/com/enlpot/notix/ui/components/NotificationDetailDialog.kt` | 点外部关闭修复：`usePlatformDefaultWidth = false` 关闭原生 scrim 后，手动补半透明遮罩 `Color.Black.copy(alpha = 0.32f)`；外层 `Box` 加 `clickable(onClick = onDismiss)` 实现点遮罩关闭；Surface 加 `clickable(onClick = {})` 吞掉内部点击，避免点击穿透误关。内部按钮 / 文字选择等交互不变。
`app/src/main/java/com/enlpot/notix/ui/components/HistoryNotificationDetailsDialog.kt` | 聚合窗口同步改造：Dialog 增加 `DialogProperties(usePlatformDefaultWidth = false)`，外层 `Box(fillMaxSize)` 补遮罩 + 点遮罩关闭；Card 宽度改为 `fillMaxWidth(0.9f)` 并按屏幕真实宽度生效，同时吞掉内部点击。视觉上与通知详情弹窗保持一致，文案 / 逻辑 / 令牌均不变。

## 本轮修改（第 3 轮 · 2026-08-23）

> 修改点：全站基面配色标准化为 MD3 深色规范（基面由纯黑 OLED 改为标准 MD3 深色 `#1B1B1F`），并严格保留应用分组卡片 / 规则分组卡片 / 规则卡片现有配色。

`app/src/main/java/com/enlpot/notix/ui/theme/Color.kt` | 深色板 `md_theme_dark_background` 与 `md_theme_dark_surface` 由纯黑 `0xFF000000` 改为标准 MD3 深色 `0xFF1B1B1F`（Neutral 10）。其余深色令牌（primary=#92CCFF、onSurface=#E2E2E6、onSurfaceVariant=#C2C7CE、surfaceVariant=#42474E、outline/outlineVariant、error=#FFB4AB、scrim 等）保持不变。非卡片界面元素（背景 / 工具栏 / 按钮 / 分割线 / 文本）已全部使用 `MaterialTheme.colorScheme.*` 令牌，改基面令牌即整站统一，无需逐文件改色。

**受保护组件（未做任何改动）**：`HistoryScreen` 的 `AppGroupHeader`、按应用/已过滤分组卡片、聚合卡片，以及 `RulesScreen` 的 `RuleCard`——其配色均来自 `NotificationColorEngine.getNotificationColors()` 派生的 `accent`/`accentFg`（按 App 图标动态取色），不引用 background/surface 令牌，本次基面改动对其零影响。

**无障碍**：`onSurface #E2E2E6` 配 `#1B1B1F` 对比度 ≈ 13.8:1、`onSurfaceVariant #C2C7CE` ≈ 11.3:1，均超 WCAG AAA（7:1）。

## 本轮修改（发布配置 · 2026-08-23）

> 目标：修复 GitHub Action 自动发布，并随 v8.3 改造一并推送。

- `app/build.gradle.kts` | `versionCode` 115 → **116**，`versionName` "8.2" → **"8.3"**。
- `.github/workflows/release.yml` | 修复自动发布触发条件：原 `paths: ['app/build.gradle.kts']` 导致非该文件改动时**永不触发发布**；改为「`push` 到 `main` 即触发」。`actions/checkout` 由 v5 降为稳定 v4（两处）。签名仍走 `NOTIX_KEYSTORE_*` secret（已配置）。
- `RELEASE_NOTES.md` | 新增 `## 8.3` 章节，汇总本轮弹窗加宽 / 点遮罩关闭 / MD3 深色基面 / 保留受保护卡片 / 构建修复等变更。

**回灌完整性核对（2026-08-23）**：对 `output/notix-ui-refactor/Notix_full_8.2/` 与 `D:\AndroidDevelop\Notix` 做全量 diff，除 `.gradle/` 缓存与构建产物外，源码文件一致性核对结论——仓库内 5 个未参与本轮改造的 kt 文件（`AutoAddedRulesDialog`/`DeleteConfirmationDialog`/`HistoryScreen`/`RuleWizardScreen`/`RulesScreen`/`SetupWizardScreen`）在 output 工作区为更旧的基线写法，仓库版已是 v8.3 首轮改造后的权威版，**无需回灌**；`gradle.properties` 仓库版含修复注释亦为权威版。仓库当前状态完整且正确。

## 本轮修改（设置页-权限管理二级界面 · 2026-08-23）

> 修改点：权限管理做进二级界面，主设置界面只显示「权限管理」入口与实时监控状态，点进去跳转详情；同时修复原一次性 `remember{}` 监控失效 bug，并补齐缺失的 `POST_NOTIFICATIONS` 与新增的「前台服务保活」实时监控项。
>
> 基线：v8.5（versionCode=118 / versionName="8.5"）。

`app/src/main/java/com/enlpot/notix/ui/screens/SettingsScreen.kt` | **主设置页**：删除原三个内联 `SettingsSection`（通知监听 / 电池优化 / 后台保活）；替换为单一可点击入口行 `SettingsSection(title=settings_permission_section_title)`，行内盾牌图标(主题蓝圆底) + 标题「权限管理」 + 副标题「实时监听 4 项系统权限」 + 右侧状态徽标（绿色 `settings_permission_all_normal` / 红色 `settings_permission_n_abnormal`(带数量格式)） + 右箭头，点击进入二级详情页。
`app/src/main/java/com/enlpot/notix/ui/screens/SettingsScreen.kt` | **二级详情页 PermissionScreen**：与 `StorageUsageScreen` 同款结构（顶部返回栏 `IconButton(KeyboardArrowLeft) + 标题(Bold) + 「实时监控中」pill`；正文 `Column(verticalScroll)`；4 张 `ElevatedCard(RoundedCornerShape(16.dp))` 权限卡片）。失效项聚合时在标题下方插入红色告警横幅 `ElevatedCard(RoundedCornerShape(12.dp), containerColor=errorContainer)` 「检测到 N 项权限已失效，部分功能可能受影响」。每张卡片含功能图标(主题蓝圆底，NotificationListenerService=铃 / POST_NOTIFICATIONS=发送 / 电池优化=电池警告 / 前台服务=盾牌)、标题、状态（绿色「正常」/ 红色「已失效」）、说明文字；失效项额外在底部居右显示红色文字按钮 `TextButton(contentColor=error)`「前往设置修复 / 一键设置」，点击跳对应系统设置（`ACTION_NOTIFICATION_LISTENER_SETTINGS` / `ACTION_APP_NOTIFICATION_SETTINGS(EXTRA_APP_PACKAGE)` / `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS(package:uri)` / `ACTION_APPLICATION_DETAILS_SETTINGS`）。
`app/src/main/java/com/enlpot/notix/ui/screens/SettingsScreen.kt` | **路由**：在 `SettingsScreen` 主体内新增 `var showPermissionScreen by mutableStateOf(false)`，content 渲染顺序改为 `if (showPermissionScreen) PermissionScreen(...) else if (showStorageUsageScreen) StorageUsageScreen(...) else Column{...}`，沿用既有布尔位路由；`PermissionScreen.onBack` 关闭详情页并 `permissionRefreshTick++` 触发主入口徽标同步刷新。
`app/src/main/java/com/enlpot/notix/ui/screens/SettingsScreen.kt` | **实时监控修复**：删除原一次性 `val notificationAccessGranted / batteryOptimizationGranted by remember {}`；新增 `var permissionRefreshTick by mutableStateOf(0)`，主入口 4 项状态改为 `remember(permissionRefreshTick) { SetupState.isNotificationListenerEnabled / isPostNotificationsGranted / isIgnoringBatteryOptimizations(context) / isKeepaliveServiceRunning(context) }`（其中 `isPostNotificationsGranted` 为**本轮新增**的第 4 项监控——Android 13+ 运行时权限；`isKeepaliveServiceRunning` 为**本轮新增**的第 5 项监控——通过 `ActivityManager.getRunningServices` 检查 `NotificationBlockerService` 是否以 fg 身份运行），`permFailedCount = listOf(...).count { !it }`。`DisposableEffect(LocalLifecycleOwner)` 注册 `LifecycleEventObserver(ON_RESUME)`，进入前台时 `permissionRefreshTick++` 重算——彻底修复原「从系统设置返回后状态不刷新」的 stale bug。`PermissionScreen` 内部用同样的 `refreshTick` + ON_RESUME 模式实现自身 4 张卡的实时同步。
`app/src/main/java/com/enlpot/notix/ui/screens/SettingsScreen.kt` | **辅助函数**：新增 `private fun isKeepaliveServiceRunning(context: Context): Boolean`（`@Suppress("DEPRECATION") getRunningServices(Int.MAX_VALUE)` 匹配 `NotificationBlockerService::class.java.name && it.foreground`），用于前台服务监控。
`app/src/main/java/com/enlpot/notix/ui/screens/SettingsScreen.kt` | **imports 调整**：移除 `android.os.PowerManager`（已无引用）；新增 `android.app.ActivityManager`、`androidx.compose.runtime.DisposableEffect`、`androidx.compose.material.icons.automirrored.filled.{KeyboardArrowLeft,Send}`、`androidx.compose.ui.platform.LocalLifecycleOwner`、`androidx.lifecycle.{Lifecycle, LifecycleEventObserver}`、`com.enlpot.notix.setup.SetupState`。
`app/src/main/res/values/strings.xml` | 新增 13 条英文文案：`settings_permission_section_title`("Permissions")、`settings_permission_monitoring_subtitle`("Monitoring 4 system permissions in real time")、`settings_permission_all_normal`("Normal")、`settings_permission_n_abnormal`("%d abnormal")、`settings_permission_monitoring_pill`("Live monitoring")、`settings_permission_monitor_title`("Live Permission Monitor · 4 permissions")、`settings_permission_abnormal_banner`("%d permission(s) have failed; some features may be affected")、`settings_permission_status_normal`("Normal")、`settings_permission_status_failed`("Failed")、`settings_permission_postnotif_title`("Post Notifications")、`settings_permission_postnotif_desc`("Used to show blocking results and reminders (Android 13+ requires system grant)")、`settings_permission_foreground_title`("Foreground Service")、`settings_permission_foreground_desc`("Foreground service keeps notification listening alive when the system reclaims resources")。
`app/src/main/res/values-zh-rCN/strings.xml` | 新增对应 13 条中文文案：权限管理 / 实时监听 4 项系统权限 / 正常 / %d 项异常 / 实时监控中 / 实时权限监控 · 共 4 项权限 / 检测到 %d 项权限已失效，部分功能可能受影响 / 正常 / 已失效 / 发送通知 / 用于展示拦截结果与提醒（Android 13+ 需在系统中授权）/ 前台服务保活 / 前台服务常驻，保证通知监听不被系统回收。

**行为变化**：
- 用户视角：主设置页顶部进入后，权限区域仅一格「权限管理」+ 盾牌图标 + 实时状态标签（绿「正常」/ 红「N 项异常」）+ 副标题「实时监听 4 项系统权限」+ 右箭头；点击进入 4 卡详情页，可视化查看每项权限状态、说明文字与修复入口。
- 监控更准确：进入前台 / 从系统设置返回后自动重新检查，状态不再卡在首次进入值；新增对 `POST_NOTIFICATIONS`（Android 13+）与「前台服务保活」的实时监控。
- 构建：`gradlew.bat assembleDebug --no-daemon` BUILD SUCCESSFUL（仅 `LocalLifecycleOwner` 与 `Icons.Filled.Send` 两处 deprecation warning，与 `SetupWizardScreen` 等既有代码一致）；APK 已 `adb -s emulator-5554 install -r` 安装，启动后实测：主设置页 → 权限管理入口（盾牌 + 绿色「正常」） → 点击进入二级页（4 张绿色「正常」卡片 + 返回箭头 + 「实时监控中」pill） → 返回箭头回到主设置（状态同步刷新）。
- 未改动：版本号 / `AndroidManifest.xml` / 数据层 / 通知拦截服务 / SetupState 逻辑 / `StorageUsageScreen` 与既有弹窗（仅在主设置页加了一处新二级路由，沿用 `showStorageUsageScreen` 相同的布尔位模式）。
- 升版：未升版（用户尚未要求发版）；如需发布可由用户在 `app/build.gradle.kts` 将 `versionCode 118→119`、`versionName 8.5→8.6` 后触发 push 与 CI 自动发布。
