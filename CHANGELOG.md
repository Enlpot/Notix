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

## 本轮修改（设置页二级界面弹窗化 · 2026-08-23）

> 修改点：将设置页所有二级界面统一改为弹窗。基线：v8.6。

`app/src/main/java/com/enlpot/notix/ui/screens/SettingsScreen.kt` | **移除全屏路由**：原 `Scaffold` content 内的 `if (showPermissionScreen) PermissionScreen(...) else if (showStorageUsageScreen) StorageUsageScreen(...) else Column{...}` 改为 `Scaffold` 只渲染主设置 `Column`；权限 / 存储弹窗移到 `Scaffold` 之后、作为 `SettingsScreen` 函数体内的独立 `Dialog` 调用；保留 `permissionRefreshTick++` / `storageRefreshTick++` 的刷新语义。
`app/src/main/java/com/enlpot/notix/ui/screens/SettingsScreen.kt` | **PermissionScreen 弹窗化**：去除 `modifier` 参数，整体包在 `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))` 内；容器 `Card(Modifier.fillMaxWidth(0.95f).heightIn(max = 640.dp), RoundedCornerShape(16.dp), surfaceVariant)`，内部 `Column(verticalScroll)`；顶部由「返回箭头 + 标题」改为「标题 + 实时监控中 pill + X 关闭按钮」；内容保持 4 张 `PermissionCard` + 聚合告警横幅。
`app/src/main/java/com/enlpot/notix/ui/screens/StorageUsageScreen.kt` | **StorageUsageScreen 弹窗化**：同样包在 `Dialog(...)` 内，`Card` 容器约束同上；顶部改为「标题 + X 关闭按钮」；新增 `storage_usage_total` 文案与总占用展示「共占用 %s」；保持 3 张 `StorageItemCard` + 底部「清除全部」；4 个 `ConfirmClearDialog` 作为弹窗同级 `AlertDialog` 渲染。
`app/src/main/res/values/strings.xml` / `values-zh-rCN/strings.xml` | 新增 `storage_usage_total`：默认 "Total used: %s" / 中文 "共占用 %s"。

**行为变化**：
- 用户视角：设置页点击「权限管理」/「存储占用」后，不再全屏切换，而是居中弹窗覆盖在设置页之上，点击弹窗外区域 / 返回键 / X 按钮关闭；关闭后主设置页状态不变。
- 保留实时监控：`PermissionScreen` 内部仍用 `refreshTick + DisposableEffect(ON_RESUME)`，从系统设置返回后弹窗状态会即时刷新。
- 构建：`gradlew.bat assembleDebug --no-daemon` BUILD SUCCESSFUL（仅既有 deprecation warning）；APK 已 `adb -s emulator-5554 install -r` 安装，实测：设置页 → 存储占用 → 弹窗显示「共占用 2.3 KB」与 3 张卡片 → 关闭 → 权限管理 → 弹窗显示 4 张权限卡片（正常） → 关闭回到设置页。
- 未升版（用户未要求本轮发版）。

## 本轮修改（第 6 轮 · 2026-08-23）

> 修改点：
> 1. 统一设置页所有弹窗为崩溃日志弹窗同款设计语言（标题栏 + X 关闭、12dp 圆角按钮、垂直/水平自适应按钮布局、按钮文字强制单行）。
> 2. App 内所有涉及删除 / 清空 / 清除的不可逆操作统一增加二次确认弹窗，风格与崩溃日志弹窗一致（明确「取消」+「确认」按钮，仅确认后执行）。
> 3. 补齐按时间段 / 按应用清除历史的专用确认文案，避免文案与「全部清除」混淆；`NotixDialog` 标题支持最多两行，防止长标题截断。

`app/src/main/java/com/enlpot/notix/ui/components/NotixDialog.kt` | **新增通用弹窗组件**：基于 `AlertDialog` 封装 `NotixDialog(title, content, buttons)`，统一标题栏（`Row` 内 `Text(Modifier.weight(1f), maxLines=2)` + `Close IconButton`），内容区与按钮区均接收 `ColumnScope` slot；配套 `NotixDialogButton`（surfaceVariant 底 + primary 字，12dp 圆角，`Text` 强制 `maxLines=1`）、`NotixDangerButton`（error 底 + onError 字）。所有按钮文字强制单行，超长自动省略；当一行两个按钮过长时调用方可自行改为 `fillMaxWidth()` 垂直堆叠。
`app/src/main/java/com/enlpot/notix/ui/components/NotixConfirmDialog.kt` | **新增通用二次确认弹窗**：基于 `NotixDialog`，固定结构「标题 + 正文 + 垂直堆叠的取消/确认按钮」，`danger=true` 时确认按钮用 error 配色；默认文案「确认 / 取消」可被调用方覆盖。
`app/src/main/java/com/enlpot/notix/ui/components/DeleteConfirmationDialog.kt` | 重写为直接委托 `NotixConfirmDialog`（标题 `delete_item_title`，正文 `delete_item_confirm`，确认文字 `delete`）。
`app/src/main/java/com/enlpot/notix/ui/components/NotificationDetailDialog.kt` | 通知详情弹窗删除按钮改为先弹出 `NotixConfirmDialog`，确认后再调用 `onDismiss(); onDelete()`；新增 `showDeleteConfirm` 状态。
`app/src/main/java/com/enlpot/notix/ui/components/CrashLogDialog.kt` | 内部「清空日志」二次确认由旧 `AlertDialog` 替换为 `NotixConfirmDialog`；主弹窗仍保持原有结构作为视觉基准。
`app/src/main/java/com/enlpot/notix/ui/screens/StorageUsageScreen.kt` | 主弹窗由 `Dialog+Card` 替换为 `NotixDialog`；3 张分类卡片的「清除」与底部「清除全部」均沿用原危险按钮样式；4 处 `ConfirmClearDialog` 全部替换为 `NotixConfirmDialog`；移除已无人引用的私有 `ConfirmClearDialog` 死代码。
`app/src/main/java/com/enlpot/notix/ui/screens/SettingsScreen.kt` | 设置页弹窗全面统一：
- 导出/导入弹窗 → `NotixDialog` + 两个 `NotixDialogButton(Modifier.weight(1f))` 并排；
- 导出/导入结果提示弹窗 → `NotixDialog` + 单个全宽确认按钮；
- 清除历史模式弹窗 → `NotixDialog` + 全宽「全部清除」（error）/「按时间段」/「按应用」/「取消」；
- 全部清除确认 → `NotixConfirmDialog`；
- 按时间段弹窗 → `NotixDialog` + 日期选择 + 快捷选项 + 危险按钮「清除此时间段历史」+ 取消，新增嵌套 `NotixConfirmDialog` 承载真实清除逻辑；
- 按应用弹窗 → `NotixDialog` + 应用多选 + 危险按钮「清除所选应用历史」+ 取消，新增嵌套 `NotixConfirmDialog`；
- 权限管理弹窗 → `NotixDialog` 承载原 `PermissionScreen` 内容（标题 + 实时监控 pill + 4 张权限卡片），移除内部旧标题栏。
`app/src/main/java/com/enlpot/notix/ui/screens/HistoryScreen.kt` | 三个确认弹窗统一为 `NotixConfirmDialog`：停止监控确认、监听暂停/恢复确认、通知访问权限掉线引导确认；移除旧的 `Dialog+Card` / `AlertDialog` 实现及冗余 import。
`app/src/main/java/com/enlpot/notix/ui/screens/RulesScreen.kt` | 规则删除确认由旧 `AlertDialog` 替换为 `NotixConfirmDialog`。
`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` | 动作卡片长按删除前新增 `NotixConfirmDialog`（标题/正文使用既有 `rule_wizard_action_delete_*` 字符串）。
`app/src/main/res/values/strings.xml` / `values-zh-rCN/strings.xml` | 新增按时间段/按应用清除的专用确认文案：`clear_by_date_range_confirm_title/body`、`clear_by_app_confirm_title/body`。
`app/src/main/res/values-{ru,pl,ja,fr,ko,es}/strings.xml` | 同步新增上述 4 个字符串（英文占位，保证多语言文件完整）。

**验证**：
- `gradlew.bat assembleDebug --no-daemon` BUILD SUCCESSFUL（仅既有 deprecation warning）。
- APK 已 `adb -s emulator-5554 install -r` 安装并启动。
- 实测覆盖：设置页 → 存储占用弹窗 / 清除历史弹窗 / 全部清除确认 / 按时间段选择及确认 / 按应用选择及确认 / 导出导入弹窗 / 权限管理弹窗 / 崩溃日志弹窗及清空确认；历史页 → 通知详情删除确认 / 停止监控确认 / 暂停监听确认；规则页 → 长按规则卡片删除确认；规则向导 → 长按动作卡片删除确认。所有弹窗标题/按钮文字均完整可读，按钮全部单行，视觉风格统一。
- 未升版（用户未要求本轮发版）。

## 本轮修改（第 7 轮 · 2026-08-23）

> 修改点：
> 1. 按权限审计结论移除冗余且受 Google Play 审查的 `QUERY_ALL_PACKAGES` 权限，并清空 `<queries>` 里 32 个无用包名（App 仅处理已发送通知的应用，不依赖全量包查询）。
> 2. 设置页「权限管理」弹窗补全每个权限的 Android 权限标识与使用组件说明，让全部 App 权限及其用途一目了然。
> 3. 权限管理弹窗内容较长，改为 `LazyColumn` 承载，确保 4 张权限卡片均可滚动查看。

`app/src/main/AndroidManifest.xml` | 移除 `android.permission.QUERY_ALL_PACKAGES`（含 `tools:ignore`）; 删除整段 `<queries>` 包名列表，缩小包可见性面。

`app/src/main/java/com/enlpot/notix/ui/screens/SettingsScreen.kt` | 
- `PermissionCard` 新增 `permName`（权限常量，等宽字体展示）和 `usedBy`（使用组件）参数；
- 4 张权限卡分别标注：
  - 通知访问权限 → `BIND_NOTIFICATION_LISTENER_SERVICE` → `NotificationBlockerService（通知监听服务）`;
  - 发送通知 → `POST_NOTIFICATIONS` → `NotificationBlockerService（强提醒）、HealthCheckWorker（健康检查）、MainActivity`;
  - 电池优化白名单 → `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` → `保活前台服务、设置页一键设置`;
  - 前台服务保活 → `FOREGROUND_SERVICE + FOREGROUND_SERVICE_SPECIAL_USE` → `NotificationBlockerService（specialUse 保活前台服务）`。
- `PermissionScreen` 内容区改为 `LazyColumn`，避免卡片过长被截断。

`app/src/main/res/values/strings.xml` / `values-zh-rCN/strings.xml` | 新增 `settings_permission_usedby_label` 与 4 个 `settings_permission_*_usedby` 字符串；前台服务描述改为明确说明 `Foreground Service + Special Use`。

**影响评估**：
- 包可见性缩小：App 不调用 `getInstalledPackages/Applications`，图标/应用名均来自通知到达时直接抓取并存入 `AppInfoStorage` 的数据；移除后仅在极端情况下（AppInfoStorage 被清空但历史仍在）图标回退为首字母占位、名称回退为包名，正常流程无影响。模拟器实测通知历史页应用图标正常显示。
- Google Play 政策风险降低：`QUERY_ALL_PACKAGES` 为受限权限，移除后不再触发该权限的声明审查。

**验证**：
- `gradlew.bat assembleDebug --no-daemon` BUILD SUCCESSFUL（仅既有 deprecation warning）。
- 合并后的 `AndroidManifest.xml` 已确认不含 `QUERY_ALL_PACKAGES` 与任何 hardcoded 包名。
- APK 已 `adb -s emulator-5554 install -r` 安装并启动；实测：设置页 → 权限管理弹窗可滚动，4 张卡片的权限标识和使用组件均完整显示。
- 未升版（用户未要求本轮发版）。

## 本轮修改（第 8 轮 · 2026-08-23）

> 修改点：
> 1. 权限管理弹窗的 4 张权限卡片改为整卡可点击，点击即跳转对应系统设置页（通知访问 → 通知使用权设置、发送通知 → 应用通知设置、电池优化白名单 → 电池优化豁免、前台服务保活 → 应用详情页）。
> 2. 弹窗太窄/太长：重构 `NotixDialog` 容器，由 M3 `AlertDialog` 改为与「通知详情弹窗」一致的自定义 `Dialog(usePlatformDefaultWidth=false)` + 屏幕宽度 92% + 高度上限 85% + 半透明遮罩点击外部关闭，所有使用 `NotixDialog` 的弹窗统一变宽变紧凑。
> 3. 权限卡片内部压缩：内边距 16dp→14dp，卡片间距 12dp→8dp，权限标识/描述/使用组件间留白收紧；头部增加 `NavChevron()` 箭头提示「可点击进入设置」。

`app/src/main/java/com/enlpot/notix/ui/components/NotixDialog.kt` |
- 内部由 M3 `AlertDialog` 重构为自定义 `Dialog(usePlatformDefaultWidth=false)`；
- 外层 `Box(Modifier.fillMaxSize())` 补半透明遮罩 `Color.Black.copy(alpha=0.32f)`，并加 `clickable` 实现点外部关闭；
- Surface 宽度 `fillMaxWidth(0.92f)`、高度上限 `maxHeight * 0.85f`（过长时内容滚动），形状/颜色/阴影与通知详情弹窗一致（`NotixCorner.Dialog` + `surface` + `tonalElevation=6.dp`）；
- Surface 加 `clickable(onClick={})` 吞掉内部点击，避免误关；
- 标题/关闭按钮/内容/按钮区布局不变，保持 `NotixDialogButton` / `NotixDangerButton` 可用。

`app/src/main/java/com/enlpot/notix/ui/screens/SettingsScreen.kt` |
- `PermissionCard` 的 `ElevatedCard` 加 `Modifier.clickable(onClick = onFix)`，整卡可点击；
- 头部标题区右侧增加 `NavChevron()`（与设置列表项统一的可进入箭头），提示用户点击跳转；
- 卡片内边距收紧、各文本区块间距收紧，`PermissionScreen` 卡片间距由 12dp 缩至 8dp，弹窗整体更短；
- 未授予时的「前往设置」TextButton 保留，作为未授权状态的高亮入口。

**影响评估**：
- 所有调用 `NotixDialog` 的弹窗（设置页各弹窗、权限管理、存储占用、删除/清空二次确认等）同步变宽 92%、高度上限 85%、支持点外部关闭；与此前已存在的通知详情弹窗/历史详情弹窗宽屏模式一致，设计语言统一。
- 权限卡片可跳转系统设置，提升修复效率；箭头 affordance 降低用户认知成本。

**验证**：
- `gradlew.bat assembleDebug --no-daemon` BUILD SUCCESSFUL（仅既有 deprecation warning）。
- APK 已 `adb -s emulator-5554 install -r` 安装并启动；权限管理弹窗 4 张卡一屏完整显示，宽度明显加宽。
- 点击「通知访问权限」卡 → `com.android.settings.Settings$NotificationAccessSettingsActivity`；点击「前台服务保活」卡 → 应用详情页；跳转正确。
- 回归测试：通知历史长按删除 → 删除确认弹窗正常渲染，宽度一致，按钮不溢出。
- 未升版（用户未要求本轮发版）。

## 本轮修改（第 9 轮 · 2026-08-24 · v8.10）

> 修改点：
> 1. 规则向导的"动作"系统重新设计：原 7 项动作精简为 8 项与 Notix v1 目标对齐，弹窗 UI 全面统一为 `NotixDialog` 风格，配置面板改为弹窗打开而非内联展开。
> 2. 解决"点击拖手柄意外打开弹窗"的可访问性问题：把打开弹窗的 clickable 区域与拖动排序的 pointerInput 区域在视觉和命中上彻底分离。
>
> 基线：v8.9（versionCode=122 / versionName="8.9"）。
> 升级：v8.10（versionCode=123 / versionName="8.10"）。

`app/src/main/java/com/enlpot/notix/BlockerRule.kt` | **`RuleAction` 枚举重整**：删除 `SILENT`（静默重显），新增 `STRONG_REMIND`（强提醒）/ `POSTPONE`（延迟重发）。`ActionSpec.isValid` 扩到 8 个分支：POSTPONE 复用 DELAY 的 BigDecimal 范围安全解析，STRONG_REMIND 默认 true。同步新增 `StrongRemindParams(sound, vibrate)` 与 `PostponeParams(delayMs)` 两个 data class。

`app/src/main/java/com/enlpot/notix/RuleWizardSupport.kt` | `hasActionParams` 把 STRONG_REMIND/POSTPONE 加入带参类型；`defaultParamsFor` 给新加的填默认（STRONG_REMIND 默认响铃+震动均开、POSTPONE 默认 60s）；新增 `strongRemindSpec` / `postponeSpec` 工厂；`actionFlowSummary` 加新分支（"移除通知" / "打开通知对应页面" / "TTS 播报通知标题和正文" / "强提醒（heads-up + 响铃 + 震动）" / "延迟 1 分钟后重发" 等），移除 SILENT。

`app/src/main/java/com/enlpot/notix/ActionFlowExecutor.kt` | `when` 分支同步：删 SILENT、加 STRONG_REMIND/POSTPONE 两个 TODO 占位（`log("skipped (execution TODO)") + completeAction(null)`，保证规则触发时这两个动作不崩、流程继续推进到下一项）。**真正的执行层（高优 heads-up + 响铃 + 震动、Handler.postDelayed 重发）计划在 v8.11 接入 NotificationProcessor + 前台服务**，本轮 UI 完整、行为安全但不强提醒/不延迟重发。`syncRunner.silent()` 接口/实现保留为死代码避免编译报错，待 v8.11 一并清理。

`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` | 4 个 when 表达式（`actionAccent` / `actionIcon` / `actionLabel` / `actionDescription`）扩 8 分支并删 SILENT；图标更新（OPEN_NOTIFICATION 仍用 `OpenInNew`、TTS 用 `VolumeUp`、新增 `PriorityHigh` 与 `Schedule`）。`ActionParamEditor` 去掉 Card + Column 外壳，保留 8 分支 when 内容，作为 `NotixDialog` content 子块使用。

`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` | **新加 `ActionConfigDialog(spec, onDismiss, onCommit)`**：套 `NotixDialog`，装 description + `ActionParamEditor`，无参数项（DISMISS / OPEN_NOTIFICATION）底部自动渲染"完成"按钮。`ActionPickerDialog` 从 `AlertDialog` 升级为 `NotixDialog`（8 项统一视觉：圆形 accent 图标 + 标题 + 描述），后**第二轮反馈后又删掉底部"取消"按钮**，靠标题 X + 弹窗外点击关闭（与崩溃日志弹窗等 NotixDialog 一致）。`ActionFlowSection` 删内联 `ActionParamEditor` 调用，改为底部 `if (editingIndex in range) ActionConfigDialog(...)` 接管"打开弹窗"语义。

`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` | **`AppPickerCard` 增加折叠按钮**：搜索框右侧 trailingIcon 改为 `Row(verticalAlignment=CenterVertically)` 装"清除 X + ↑/↓ 折叠箭头"；新增 `var isAppListExpanded by rememberSaveable { mutableStateOf(true) }`（默认展开、旋转/切后台不丢）；应用列表 + 底部"完成"按钮整段用 `if (isAppListExpanded) { ... }` 包起来。收起时仅留搜索框，搜索/复选仍可用。

`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` | **`ActionCard` 重构布局（v8.10 第二轮反馈）**：去掉整张 Card 的 `combinedClickable` 顶层点击，外层改 `Row(verticalAlignment=CenterVertically)` 内部分两列——左列（`weight(1f)` + `clip` + `combinedClickable(onClick=打开弹窗, onLongClick=弹 NotixConfirmDialog 删除确认)`）承载序号/图标/标题/摘要，右列（`Box(48dp×64dp)` + `pointerInput + Icon.size(28.dp)` DragHandle）独占拖动手势响应。**点击主体打开弹窗、长按主体弹删除确认、拖手柄单独响应上下拖动排序**——三个交互区域互不重合、不会误触。拖手柄图标由 18dp 提到 28dp、触控区扩到 48dp 标准 Material 目标，高度撑到与卡片同高 64dp，视觉上明显比之前更大。

`app/src/main/res/values/strings.xml` + `values-zh-rCN/strings.xml` |
- 4 个动作 label 改名（消除通知→移除、打开通知→打开、播报→TTS 播报、等待保持）；
- 删 `rule_action_silent` / `rule_action_desc_silent` 2 条；
- 新增 `rule_action_strong_remind` / `rule_action_desc_strong_remind` / `rule_action_postpone` / `rule_action_desc_postpone` 4 条（中英同步）；
- 新增 5 条 wizard 字符串：`rule_wizard_strong_remind_desc/sound/vibrate` / `rule_wizard_postpone_duration/invalid/desc`（中英同步）；
- 新增 `rule_wizard_action_flow_pick_hint` "选择要执行的动作，触发时按顺序执行"（中英同步）。

`app/build.gradle.kts` | `versionCode 122 → 123`，`versionName "8.9" → "8.10"`。
`RELEASE_NOTES.md` | 整文件覆盖为 v8.10（仅本版 + 英文，发版给 GitHub Release 用）。
`VERSION_HISTORY.md` | 顶部新增 `## 8.10 (2026-08-24)` 段（英文，结构与本段同步但精简为 GitHub Release 友好的 bullet）。
`VERSION_HISTORY.zh-CN.md` | 顶部新增 `## 8.10 (2026-08-24)` 段（中文，与英文版结构同步，弥补此前 v8.6/8.7 中文断档）。

**验证**：
- `gradlew.bat assembleDebug --no-daemon` BUILD SUCCESSFUL（约 36~50s，依缓存而定）。
- APK 装到 `emulator-5554` 并启动；UIAutomator dump + 截图全部通过：
  - 弹窗 8 项顺序、label、描述与设计要求一致（移除 / 点击按钮 / 打开 / 复制内容 / TTS 播报 / 强提醒 / 等待 / 延迟）；
  - 「强提醒」/「延迟」参数面板可用，参数可保存到动作流；
  - 添加动作弹窗、点击已添加动作卡片均弹 NotixDialog 配置面板，**不再内联向下展开**；
  - 添加动作弹窗底部已删"取消"按钮，靠 X + 弹窗外点击关闭；
  - 拖手柄 48×64dp 大触控区，点拖手柄不打开弹窗，向上/向下拖可正常排序（实测 1 等待/2 移除 互换成功）；
  - 来源应用搜索框右侧出现 `content-desc="收起"/"展开"` 的 IconButton，bounds [912,649][975,712]，点击收起后应用列表 + 完成按钮同步消失，再点展开回归。
- 截图存证：`D:\AndroidDevelop\Notix\.workbuddy\screenshots\v8.10_*.png`（action_picker_8items / action_picker_postpone_panel / action_picker_notixdialog / action_config_dialog_postpone / action_card_big_draghandle / action_picker_no_cancel / rule_wizard_source_expanded）。
- 行为兼容：现有 v8.9 已发版规则因含 `SILENT` 字段会反序列化为 null，但 8.9→8.10 期间无用户线上规则带 SILENT（v8.9 中 SILENT 即可正常加载执行），故无迁移风险；v8.11 接入强提醒/延迟执行层后，STRONG_REMIND/POSTPONE 字段已能被新代码正常加载。
- 已 bump versionName/versionCode，commit 后 push main 触发 GitHub Actions 自动发版 v8.10（workflow `check-version` 命中 v8.10 → `build-and-release` 走 NOTIX_KEYSTORE_* 签名并通过 `RELEASE_NOTES.md` 贴正文）。

## 本轮修改（第 10 轮 · v8.11）— 动作拖动动画指示 + 死代码清理

- **死代码清理（v8.10 待办收尾）**：
  - `ActionFlowExecutor.kt` 删除 `SyncActionRunner.silent(ctx)` 接口方法（line 83）及其 `RealSyncActionRunner` 实现；
  - `ActionFlowExecutor.kt` 删除 `ActionFlowHost.repostSilent(ctx)` 接口方法（line 67-68）；
  - `NotificationBlockerService.kt` 删除 `repostSilent(ctx)` 实现（line 627-637，含 SILENT 注释块）；
  - 测试桩同步清理：`ActionFlowExecutorTest.kt` / `ActionFlowCopyBehaviorTest.kt` 中 `FakeSyncRunner`/`FakeHost` 移除 `silent`/`repostSilent` override；`RuleWizardSupportTest.kt` 删除 `silent summary is 静默重显` 用例；`ActionFlowModelTest.kt` 中 `RuleAction.SILENT` 引用移除；
  - `ActionFlowExecutorTest.kt:85` 修正：构造调用 `ActionFlowExecutor(sync, async, log = {}, hostAlive = { true })`（v8.0 注入的 `hostAlive` lambda 没显式给，导致它被推断到第一个 lambda 槽位而类型不匹配；之前 assembleDebug 不编测试所以漏检）；
  - `RuleWizardSupportTest.kt` 三处断言随 v8.10 label 改动同步更新（DISMISS 摘要 "消除通知" → "移除通知"；TTS 摘要前缀加 "TTS "）。
- **动作拖动动画指示**：用户实测 v8.10 步进式拖动"没动画效果"，调研后引入社区主流方案 `sh.calvin.reorderable:reorderable:2.4.3`：
  - `ActionFlowSection` 从 `Column.forEachIndexed` 改 `LazyColumn + ReorderableLazyListState + ReorderableItem`，由库接管拖动手势；
  - `ActionCard` 接收 `isDragging: Boolean` + `dragHandleModifier: Modifier`（来自 `ReorderableItem.draggableHandle`），删除手写 pointerInput/detectDragGestures/accum 步进逻辑；
  - 给拖手柄加 `ViewCompat.performHapticFeedback` 长按/释放触觉反馈（DRAG_START / GESTURE_END）；
  - `ActionSpec.toStableKey()` 提供 LazyColumn item 稳定 key；
  - LazyColumn 必须设 `heightIn(max = 480.dp)`，否则嵌入外层 verticalScroll Column 时报 `IllegalStateException: ... infinity maximum height constraints`；
  - `gradle/libs.versions.toml` 新增 `reorderable = "2.4.3"`；`app/build.gradle.kts` 加 `implementation(libs.reorderable)`。
- **验证**：
  - `gradlew testDebugUnitTest` **87 tests passed**（含 v8.10 待办触发的几处测试桩清理）；
  - `gradlew assembleDebug` BUILD SUCCESSFUL；
  - emulator-5554 实测：1 等待/2 移除 → swipe 250px / 600ms → 1 移除/2 等待 互换成功；拖动中截图 `v8.11_reorder_mid.png` 可见被拖卡片蓝色 primary 描边 + 抬升 6dp 阴影 + 其他卡片让出 gap（中间出现空隙，"移除通知"文字被部分覆盖）；释放后 `v8.11_reorder_done.png` 卡片平整、shadow 回落。
- **未做的（v8.12+ 待办）**：强提醒（STRONG_REMIND）的真实执行（heads-up + 响铃 + 震动）需接入 NotificationProcessor 的 high-importance NotificationChannel；延迟（POSTPONE）的真实执行需 Handler.postDelayed 重新投递原通知。
- 已 bump versionName/versionCode → v8.11，commit + push main 触发 GitHub Actions 自动发版。

## 本轮修改（第 11 轮 · 2026-08-24 · TTS 变量选择插入）

> 修改点：TTS 播报动作配置弹窗支持可视化变量选择，点击后自动插入到当前输入框光标位置；同时执行层扩展支持 {time}/{date} 两个新占位符。

`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` | **TTS 编辑器改用 TextFieldValue**：由 `String` 改为 `TextFieldValue` 以同时保存文本与光标/选区位置；`OutlinedTextField` 的 `value`/`onValueChange` 同步切换。

`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` | **新增变量选择 chip 区**：在模板输入框与提示文字之间插入「点击插入变量」标题 + `FlowRow` 包裹 5 个 `AssistChip`，标签分别为 app名称 / 标题 / 内容 / 发送时间 / 发送日期；点击 chip 时根据当前 `selection.start/end` 在光标处插入对应占位符 `{app}`/`{title}`/`{text}`/`{time}`/`{date}`，并将光标移到插入内容之后。支持重复点击、自由组合、在已有文本任意位置插入。

`app/src/main/java/com/enlpot/notix/ActionFlowExecutor.kt` | **扩展 `ActionFlowHost.buildTtsText` 签名**：增加 `postTime: Long` 参数，供模板渲染发送时间/日期。

`app/src/main/java/com/enlpot/notix/NotificationBlockerService.kt` | **TTS 占位符扩展**：`buildTtsText` 新增 `{time}`/`{date}` 替换；新增 `formatTtsTime(postTime)` → "HH点mm分"、`formatTtsDate(postTime)` → "M月d日"（中文口语化，便于播报）；`postTime <= 0` 时返回空字符串，与现有 `{app}`/`{title}`/`{text}` 缺失即跳过逻辑一致。

`app/src/main/res/values/strings.xml` | 更新 TTS hint 为 "支持 {app}、{title}、{text}、{time}、{date} 占位符"；新增 `rule_wizard_action_tts_variables_title` 及 5 个变量标签字符串。

`app/src/test/java/com/enlpot/notix/ActionFlowCopyBehaviorTest.kt` | 同步更新 `FakeHost.buildTtsText` override 签名（增加 `postTime`）。

**验证**：
- `gradlew testDebugUnitTest` BUILD SUCCESSFUL（全部单测通过）。
- `gradlew assembleDebug --no-daemon` BUILD SUCCESSFUL。
- 未升版（用户未要求本轮发版）。
