# Notix UI 改造清单（v8.3 · 一致性规范）

> 基线：v8.2（versionCode=115 / versionName="8.2"）

## 本轮修改（v8.14.1 · 2026-08-25）

> 修复 release 包「移除」动作弹窗中冻结时长 chip 无法选中的 bug。
>
> 根因：release 构建启用 R8 全量优化后，`RuleWizardScreen` DISMISS 弹窗用 `Long` 状态保存冻结时长，`FilterChip` 的 `selected = snoozeDurationMs == ms` 在优化后比较异常，导致点击 chip 无视觉反馈、没有任何 chip 保持选中（debug 包正常）。
>
> 修复：将弹窗内部选中状态改为 `SnoozeDurations.OPTIONS` 的索引 `selectedDurationIndex`（`mutableIntStateOf`），`FilterChip` 按索引比较，保存时从索引取对应时长值。同时补充 ProGuard keep 规则：`ActionSpec`、`DismissParams`、`SnoozeDurations` 保持原类名与成员，避免 R8 过度优化影响运行时行为。

`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` | DISMISS 弹窗冻结时长状态由 `Long` 改为 `OPTIONS` 索引；`FilterChip` 按索引比较与赋值。
`app/proguard-rules.pro` | 补充 keep：`ActionSpec`、`DismissParams`、`SnoozeDurations`；移除已不存在的 `ActionParams` keep 规则。
`app/build.gradle.kts` | `versionCode` 127 → **128**，`versionName` "8.14" → **"8.14.1"**。
`RELEASE_NOTES.md` | 整文件覆盖为 v8.14.1 英文发布说明。
`VERSION_HISTORY.md` / `VERSION_HISTORY.zh-CN.md` | 顶部追加 v8.14.1 英/中文历史条目。

---
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

## 本轮修改（第 12 轮 · 2026-08-24 · 规则条件弹窗 NotixDialog 化 + 匹配模式弹窗选择）

> 修改点：
> 1. 规则向导「配置条件」弹窗（原 `AlertDialog`）改为与全站一致的 `NotixDialog` 风格；窗口尺寸固定 520dp 高，内部 `TabRow`（关键字/手机状态/时间）+ `weight(1f).verticalScroll` 可滚动内容区，超出可滚动。
> 2. 匹配模式选择（原 `DropdownMenu` 锚定 `OutlinedButton`）改为独立 `NotixDialog` 弹窗：滚动列表每行 `Row.clickable` 选中即回调，`CheckCircle` 图标指示当前项，`HorizontalDivider` 分隔，`ADVANCED` 项禁用并显示 `rule_wizard_mode_advanced_hint`。
> 3. `NotixDialog` 关闭行为修正：原 `dismissOnClickOutside` 由系统处理会与弹窗内 `Switch`/Chip 事件冲突（时间 tab 开关点不动）；改为 `dismissOnClickOutside = false` + 遮罩层 `clickable { onDismiss }` 自行关闭、`dismissOnBackPress = true`、Surface 仍 `clickable { onClick = {} }` 吞内部点击。最终外部点击关闭 + 内部开关可点均正常。

`app/src/main/java/com/enlpot/notix/ui/components/NotixDialog.kt` | `DialogProperties` 由 `usePlatformDefaultWidth = false` 改为 `usePlatformDefaultWidth = false, dismissOnClickOutside = false, dismissOnBackPress = true`；遮罩层保留 `clickable { onDismiss }`、Surface 保留 `clickable { onClick = {} }` 吞点击；注释同步说明改动原因。

`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` |
- 删除 `AlertDialog` / `DropdownMenu` / `DropdownMenuItem` 三个 import。
- `ConditionConfigDialog`：`AlertDialog(title + text 滚动 + OK/Cancel 两 TextButton)` → `NotixDialog(onDismiss, title)`，外层 `Column.height(520.dp)`，内部 `TabRow(selectedTabIndex=tab)` 三 tab + `Spacer(12.dp)` + `Column(weight(1f).verticalScroll)` 承载原 `when(tab)` 内容（关键字 tab 内 `MatchModePicker` + 两个 `KeywordChipInput`；手机状态 / 时间 tab 保持原 `PhoneStateSection` / `TimeSection`）。
- `MatchModePicker`：移除 `menuOpen` 与 `DropdownMenu`，改为 `showDialog` 状态；点击 `OutlinedButton` 打开新 `MatchModePickerDialog`。
- 新增 `MatchModePickerDialog(currentMode, onModeSelected, onDismiss)`：套 `NotixDialog`，内部 `Column.verticalScroll`，`MatchMode.entries.forEachIndexed` 渲染 `Row(clickable(enabled = !disabled), padding 12dp/4dp)`，左 `Column(weight(1f))` 装 label + ADVANCED 提示、右 `CheckCircle` 仅在 `selected` 显示；行间 `HorizontalDivider` 分隔（末项不加）。

**验证**：
- `gradlew.bat assembleDebug --no-daemon` BUILD SUCCESSFUL（仅既有 deprecation warning）。
- APK 已 `adb -s emulator-5554 install -r` 安装并冷重启；UIAutomator dump + 坐标 tap 验证：规则→添加新规则→点「配置条件」卡片 → NotixDialog 风格弹窗（固定高 520dp，三 tab）；点匹配模式按钮 → 独立弹窗打开，选项可点选；时间 tab 的日期/时间 `Switch` 可正常点击切换；弹窗外点击可关闭。
- 未升版（用户未要求本轮发版）。

## 本轮修改（第 13 轮 · 2026-08-24 · 关键字输入弹窗化 + MIXED 双按钮）

> 修改点：
> 1. 关键字输入由「弹窗内常驻输入框 + chip」改为「点击按钮弹出独立输入窗口」：与匹配模式选择保持一致的 `NotixDialog` 风格。
> 2. 触发按钮 `KeywordInputTrigger`（全宽 `OutlinedButton`，显示标签 + 已选关键字预览 + 下拉箭头），点击打开 `KeywordInputDialog`（标题 + 已选 chip 列表 + `OutlinedTextField` + 底部「确定」关闭）。
> 3. 模式联动按钮数量：仅 `MatchMode.MIXED`（包含A且不包含B）时显示两个输入按钮——「包含 A」（include）与「且不包含 B」（exclude）；其他模式只显示「包含关键字」一个按钮（复用 include 列表）。
> 4. 输入弹窗内 chip 行为保留：点击 chip 主体回填输入框并移除原词，尾部关闭图标直接删除；回车（Done）或「添加」按钮追加，空输入不添加。
> 5. 父级与 `ConditionConfigDialog` 移除不再需要的 `keywordInput` / `excludeKeywordInput` 提升态（输入态移入弹窗内部 `remember`），`KeywordChipInput` 旧组合被 `KeywordInputTrigger` + `KeywordInputDialog` 取代。

`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` |
- 父级 `RuleWizardScreen`：删除 `keywordInput` / `excludeKeywordInput` 两个 `rememberSaveable` 状态，以及传给 `ConditionConfigDialog` 的对应 4 个入参；保留 `includeKeywords` / `excludeKeywords` 与 add/remove 回调（add 仍做去重）。
- `ConditionConfigDialog`：删除 `keywordInput` / `excludeKeywordInput` / `onKeywordInputChange` / `onExcludeKeywordInputChange` 四个参数；关键字 tab 内原两个 `KeywordChipInput` 改为 `KeywordInputTrigger` + 条件渲染的 `KeywordInputDialog`（MIXED 显示两个触发器，否则一个）。
- 删除旧 `KeywordChipInput` 组合，新增：
  - `KeywordInputTrigger(label, keywords, onClick)`：`OutlinedButton` 全宽，左 `Column(weight(1f))` 装标签（`bodyMedium`/`Medium`）+ 非空时关键字预览（`bodySmall`/`onSurfaceVariant`，单行省略），右 `ArrowDropDown` 图标；样式与 `MatchModePicker` 按钮一致。
  - `KeywordInputDialog(title, keywords, onAdd, onRemove, onDismiss)`：套 `NotixDialog`（命名 `content` + `buttons` 两参数，避免 trailing-lambda 被绑到 `buttons`），`content` 内 `Column.verticalScroll` 渲染 chip `FlowRow` + `OutlinedTextField`（label 用 `title`、placeholder 用 `rule_wizard_keyword_placeholder`、trailing 为「添加」IconButton、Done 回车追加）；`buttons` 内一个全宽 `NotixDialogButton`（`ok` 文案）关闭。

**验证**：
- `gradlew.bat assembleDebug --no-daemon` BUILD SUCCESSFUL（仅既有 deprecation warning）。
- APK 已 `adb -s emulator-5554 install -r` 安装并冷重启；UIAutomator dump + 坐标 tap 验证：
  - 包含任一（非 MIXED）模式：关键字 tab 仅「包含关键字」一个触发按钮；点击 → 弹出标题「包含关键字」输入窗（含输入框 + 添加 + 确定），`input text "test"` + 点「添加」后 chip 「test」出现。
  - 切换为「包含A且不包含B」（MIXED）：关键字 tab 出现「包含 A」+「且不包含 B」两个触发按钮；点「且不包含 B」→ 独立弹出标题「且不包含 B」输入窗。
  - 所有弹窗风格与匹配模式弹窗一致，点外部可关闭。
- 未升版（用户未要求本轮发版）。

## 本轮修改（第 14 轮 · 2026-08-24 · 关键字直接显示在条件界面 + 输入弹窗去加号/换行/确定添加）

> 修改点：
> 1. 关键字 chip 直接展示在「配置条件」关键字 tab 界面（而非藏在触发按钮的预览文字里），点击 chip 主体即可打开输入弹窗编辑；chip 尾部关闭图标仍可直接删除。
> 2. 关键字输入弹窗内删除输入框右侧的「+」添加按钮；底部「确定」按钮改为提交当前输入并关闭弹窗（空输入则仅关闭）。
> 3. 输入框支持长文本换行：`singleLine = false`、`minLines = 2`、`maxLines = 5`。
> 4. 编辑行为：在条件界面点击 chip 主体，先把原词从列表移除并回填到弹窗输入框；用户修改后点「确定」即添加为新词。弹窗内部 chip 列表的点击编辑行为保留。
> 5. `KeywordInputTrigger`（触发按钮）被 `KeywordChipRow` 取代：标题行左侧显示标签、右侧显示「+」添加按钮；下方 FlowRow 直接渲染关键字 chip，无关键字时显示占位提示。

`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` |
- 新增 `KeywordChipRow(label, keywords, onEditKeyword, onRemoveKeyword, onAddClick)`：`Column` 内标题行 `Row(SpaceBetween)` 左侧标签（`bodyMedium`/`Medium`）+ 右侧 32dp `IconButton`（`Add` 图标，`rule_wizard_add_keyword` 描述）；下方 4dp spacer 后，若 `keywords` 非空则 `FlowRow` 渲染 `InputChip`（主体 `onClick = { onEditKeyword(keyword) }`，尾部 `Close` 图标 `onRemoveKeyword`），否则显示 `rule_wizard_keyword_placeholder` 占位文案。
- 删除旧 `KeywordInputTrigger` 组合。
- `ConditionConfigDialog` 关键字 tab：删除两个 `KeywordInputTrigger` 调用，改为两个 `KeywordChipRow`（MIXED 模式显示包含 A / 且不包含 B 两组，其他模式只显示包含关键字一组）；新增 `includeInitial` / `excludeInitial` 两个 `rememberSaveable` 状态，用于把待编辑关键字回填进弹窗；`onEditKeyword` 内先调用 `onRemoveXxxKeyword(kw)` 移除原词、再设初始输入并打开弹窗。
- `KeywordInputDialog`：新增 `initialInput: String = ""` 参数，`var input by remember { mutableStateOf(initialInput) }`；`OutlinedTextField` 删除 `trailingIcon`（移除右侧「+」按钮），并改为 `singleLine = false, minLines = 2, maxLines = 5` 以支持换行；底部 `NotixDialogButton` 的 `onClick` 由 `onDismiss` 改为 `{ commit(); onDismiss() }`，即确定按钮负责提交输入并关闭。

**验证**：
- `gradlew.bat assembleDebug --no-daemon` BUILD SUCCESSFUL（仅既有 deprecation warning）。
- APK 已 `adb -s emulator-5554 install -r` 安装并冷重启；UIAutomator dump + 坐标 tap 验证：
  - 规则→添加新规则→配置条件→关键字 tab：匹配模式下方直接显示「包含关键字」标题行（右侧 + 按钮）+ 占位提示「输入关键词…」。
  - 点 + 按钮 → 弹出「包含关键字」输入窗，输入框无右侧 + 号、底部有「确定」；输入 `editme` 点确定 → 条件界面出现 chip 「editme」。
  - 点击 chip 「editme」主体 → 弹窗打开且输入框回填「editme」、弹窗内 chip 列表为空（原词已移除）；追加输入 `d` 点确定 → 条件界面 chip 变为「editmed」。
  - 切换匹配模式为「包含A且不包含B」（MIXED）：界面同时显示「包含 A」与「且不含 B」两组 chip 区；分别添加 `editmed` 与 `exclude1`，两组关键字独立展示。
  - 输入框在弹窗内高度明显高于单行，长文本可换行显示。
- 未升版（用户未要求本轮发版）。

---

## v8.13 · 移除固定/常驻通知（2026-08-24）

**核心改动**：对标通知滤盒/BuzzKill 的「包括常驻通知」开关，给 Notix 的 `DISMISS` 动作加上 `snoozeNotification` 路径，让对系统无法用 `cancelNotification` 直接消除的常驻通知（音乐、前台服务、画中画、系统上层显示）也能消除。

**核心 API 调研结论**：Android `NotificationListenerService` 对常驻通知（`StatusBarNotification.isClearable() == false`）调用 `cancelNotification(key)` 是**无效的**（系统忽略 + 第三方监听器无回调）；必须改用 `snoozeNotification(key, durationMs)`（API 26+，Android 8.0+）才能真正消除。`snooze` 副作用是「手机重启后失效」（系统限制），与通知滤盒的「时间往回调 1 年」恢复技巧是同一机制。

**改动文件清单**

`app/src/main/java/com/enlpot/notix/BlockerRule.kt` |
- 新增 `data class DismissParams(val includeOngoing: Boolean = false)` v8.13：DISMISS 动作参数；`includeOngoing=true` 触发 `snoozeNotification` 路径。

`app/src/main/java/com/enlpot/notix/ActionFlowExecutor.kt` |
- `ActionContext` 新增 `val includeOngoing: Boolean = false` 字段。
- `interface ActionFlowHost` 新增 `fun snoozeNotificationCompat(key: String)` 方法。
- `RealSyncActionRunner.dismiss(ctx)` 改写分派逻辑：`sbn != null && SDK >= 26 && !sbn.isClearable && ctx.includeOngoing` → 走 `host.snoozeNotificationCompat(...)`，否则维持 `host.cancelNotificationCompat(...)`。
- 用 `sbn.isClearable`（`StatusBarNotification` 上的方法）替代 `sbn.notification.isClearable()`，因后者在 API 34+ 已删除。

`app/src/main/java/com/enlpot/notix/NotificationBlockerService.kt` |
- `executeActionFlow` 从 `rule.actions[].params.includeOngoing` 读 flag 写入 `ActionContext`。
- `snoozeNotificationCompat(key)` 实现：API 26+ 调 `snoozeNotification(key, Long.MAX_VALUE/2)` 模拟永久冻结；<26 降级到 `cancelNotification`；异常时回退 cancel。

`app/src/main/java/com/enlpot/notix/RuleWizardSupport.kt` |
- `defaultParamsFor(DISMISS) = null`（保持向后兼容——既有的 ActionFlowEditorTest 断言 `params == null` 不被破坏；`includeOngoing=false` 的行为与 `params=null` 等价）。
- `hasActionParams(DISMISS) = true`（保证用户从动作选择器点 DISMISS 后能进入「包括常驻通知」参数弹窗）。
- 新增 `fun dismissSpec(includeOngoing: Boolean): ActionSpec`：includeOngoing=true 时写入 `DismissParams` JSON；false 时返回 `params=null` 的 spec（与默认一致，JSON 体积最小）。
- `actionFlowSummary(DISMISS)` 差异化：includeOngoing=true → "移除通知（含常驻）"，false → "移除通知"，null → "移除通知"（向后兼容）。

`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` |
- `ActionParamEditor` 新增 `RuleAction.DISMISS` 分支（移出原 `else ->` 共享块）：标题行 + 「包括常驻通知」Switch + 副标题 + 取消/保存按钮；Switch 状态从 `spec.params.includeOngoing` 初始化；保存调 `RuleWizardSupport.dismissSpec(includeOngoing)`。
- `OPEN_NOTIFICATION` 留在 `else ->`（仍无参数）。

`app/src/main/res/values/strings.xml` |
- 新增 `rule_wizard_dismiss_include_ongoing` = "包括常驻通知"。
- 新增 `rule_wizard_dismiss_include_ongoing_desc` = "对通知栏无法滑动消除的通知（如音乐、前台服务、画中画）也生效。手机重启后失效。"。

`app/src/test/java/com/enlpot/notix/ActionFlowCopyBehaviorTest.kt` |
- `FakeHost` 新增 `val snoozedKeys` 列表与 `override fun snoozeNotificationCompat(key)` 实现（满足新接口契约）。

`app/src/test/java/com/enlpot/notix/DismissSpecTest.kt`（新文件） |
- v8.13 新增单测 4 例：① `defaultParamsFor(DISMISS) == null`；② `dismissSpec(false).params == null`；③ `dismissSpec(true).params.includeOngoing == true`；④ `hasActionParams(DISMISS) == true`。

`app/build.gradle.kts` |
- `versionCode 125 → 126`，`versionName "8.12" → "8.13"`。

`RELEASE_NOTES.md` / `VERSION_HISTORY.md` / `VERSION_HISTORY.zh-CN.md` |
- 整文件覆盖为 v8.13 英文发布说明 / 顶部插入 8.13 历史条目 / 中文版同步新增 8.13。

**验证**：
- `gradlew.bat assembleDebug --no-daemon` BUILD SUCCESSFUL。
- `gradlew.bat :app:testDebugUnitTest --no-daemon` BUILD SUCCESSFUL（87 + 4 = 91 例全过，包括 2 个原本因新参数会失败的 ActionFlowEditorTest 与 4 个新增 DismissSpecTest）。
- APK 已 `adb -s emulator-5554 install -r` 安装并冷启动；UIAutomator dump + 坐标 tap 验证：
  - 规则 → 添加新规则 → + 添加动作 → 选「移除」→ 弹出「移除」弹窗，**显示「包括常驻通知」开关 + 副标题**，默认关闭；保存后动作卡显示「**移除通知（含常驻）**」（证明 `actionFlowSummary` 正确读取 `includeOngoing=true`）。
  - 开关保持关闭时保存，spec.params 应为 null（向后兼容路径），不写 JSON。
- 待 v8.13 发版后真机验证：真机（一加 Ace 5 Pro / ColorOS 15）的「上层显示」「哔哩哔哩前台媒体」类通知，开启 includeOngoing 后应被一次性冻结消除（与通知滤盒行为一致）。

**已知限制**：
- 模拟器（Android 16 / API 36 / x86_64）上系统 uid=1000 通知（AlertWindowNotification）第三方监听器仍无法消除（ColorOS 厂商 ROM 限制）；真机验证是关键。
- v8.13 用 `Long.MAX_VALUE/2`（≈146 年）做冻结，属过度冻结：snooze 在 Android 11+ 持久化、**重启不失效**，且公开 API 无 unSnooze，导致被冻通知几乎无法恢复。此问题在 v8.14 修正（可自定义时长 + 短时长 re-snooze 恢复）。

## v8.14（进行中）· 常驻通知可自定义冻结时长 + 真实恢复（2026-08-24）

**背景纠正（实测坐实，推翻此前两轮草稿的错误结论）**：
- Android 公开 API `NotificationListenerService` **只有 `snoozeNotification(key, durationMs)`（API 26+），没有 `unSnoozeNotification`**。
- **snooze 是持久化的，Android 11+ 重启不失效**（写入 `/data/system/notification_policy.xml`）。之前「重启即失效」是错的——v8.13 用 `Long.MAX_VALUE/2`（≈146 年）冻的通知，重启后仍压在通知栏。
- **恢复手段（实测有效）**：对同一 key 再调 `snoozeNotification(key, 极小值)`（如 100ms），短值到期后通知自动回栏。这就是第三方 App（通知滤盒/BuzzKill）「恢复」的原理，无需 unSnooze、无需 root。

**本轮改动（可自定义时长 + 真恢复，未构建 APK）**：

`app/src/main/java/com/enlpot/notix/BlockerRule.kt` |
- 新增 `object SnoozeDurations`：冻结时长档位（1 小时 / 1 天 / 7 天 / 30 天 / 1 年），默认 7 天。
- `DismissParams` 新增 `snoozeDurationMs: Long = SnoozeDurations.DAY_7` 字段。

`app/src/main/java/com/enlpot/notix/ActionFlowExecutor.kt` |
- `ActionContext` 新增 `snoozeDurationMs` 字段。
- `ActionFlowHost.snoozeNotificationCompat(key, ruleId, durationMs)` 新增 `durationMs` 形参。
- `RealSyncActionRunner.dismiss` 冻结时传 `ctx.snoozeDurationMs`。

`app/src/main/java/com/enlpot/notix/NotificationBlockerService.kt` |
- 删除固定 `SNOOZE_DURATION_MS` 常量，冻结时长由规则参数传入。
- 新增 `RESTORE_RESNOOZE_MS = 100L`。
- `snoozeNotificationCompat(key, ruleId, durationMs)` 用传入时长冻结。
- 恢复改为**真实现**：`restoreSnoozedByRule(ruleId)` / `restoreAllSnoozedNotifications()` 对每个 key 用 100ms 短时长 re-snooze 覆盖原到期时间，通知到期自动回栏（替换掉上一轮只清本地登记的假恢复）。
- `ACTION_RESTORE_SNOOZED` 分支改用 `restoreAllSnoozedNotifications()`。
- 分组登记表 `snoozedByRule` 保留（JSON 落盘 + 旧格式迁移）。

`app/src/main/java/com/enlpot/notix/RuleStorage.kt` |
- `deleteRuleById(id)` 删除提交后调用 `restoreSnoozedByRule(id)`——**删规则即真正恢复**该规则冻结的常驻通知（100ms 后自动回栏）。

`app/src/main/java/com/enlpot/notix/RuleWizardSupport.kt` |
- `dismissSpec(includeOngoing, snoozeDurationMs)` 新增时长参数（默认 7 天）。
- `actionFlowSummary(DISMISS)` 显示「冻结 X」时长。
- 新增 `formatSnoozeDuration(ms)` 人类可读文案。

`app/src/main/java/com/enlpot/notix/ui/screens/RuleWizardScreen.kt` |
- DISMISS 弹窗新增「冻结时长」FilterChip 档位选择（勾选「包括常驻通知」后展示）。

`app/src/main/res/values/strings.xml` |
- 修正 `rule_wizard_dismiss_include_ongoing_desc`（删除错误的「手机重启后失效」说明）。
- 新增 `rule_wizard_dismiss_snooze_duration` / `rule_wizard_dismiss_snooze_duration_desc`。

`app/src/test/java/com/enlpot/notix/DismissSpecTest.kt` |
- 新增 2 例：默认时长 7 天、自定义时长写入。
- `ActionFlowCopyBehaviorTest.FakeHost` 适配 `snoozeNotificationCompat(key, ruleId, durationMs)` 新签名。

**设计要点**：
- 冻结时长用户可选：到期后通知自动恢复；越短越「可逆」，越长越「像永久移除」。
- 恢复 = 短时长 re-snooze（实测有效、无需 root、无需 unSnooze），删规则自动触发 + 设置页按钮兜底。
- 向后兼容：`dismissSpec(includeOngoing=false)` 仍返回 params=null；旧规则无 `snoozeDurationMs` 时默认 7 天。

### 设置页「恢复常驻通知」按钮（本轮追加，2026-08-24）

`app/src/main/java/com/enlpot/notix/ui/screens/SettingsScreen.kt` |
- 「规则与数据」分区新增「恢复常驻通知」入口（`Icons.Filled.Notifications` + 副标题 + NavChevron）。
- 点击弹 `NotixConfirmDialog`（`danger=false`，确认按钮用主题色）；确认后调 `NotificationBlockerService.instance?.restoreAllSnoozedNotifications()`，用 Snackbar 提示「已恢复 N 条」或「无待恢复」。

`app/src/main/res/values/strings.xml` + `values-zh-rCN/strings.xml` |
- 新增 `settings_restore_snoozed` / `settings_restore_snoozed_desc` / `settings_restore_snoozed_confirm_title` / `settings_restore_snoozed_confirm_body` / `settings_restore_snoozed_done` / `settings_restore_snoozed_none`（英文默认 + 中文翻译同步）。

**验证**：
- `testDebugUnitTest` + `assembleDebug` 均 BUILD SUCCESSFUL；APK 已装 emulator-5554。
- UIAutomator 实测：设置页「规则与数据」下显示「恢复常驻通知 / 把被规则冻结（移除）的常驻通知恢复到通知栏」；点击弹出确认弹窗（标题/正文/取消/确定）齐全；取消可正常关闭。
- 恢复的实际效果（通知回栏）依赖 `restoreAllSnoozedNotifications()` 的 100ms 短时长 re-snooze，等用户在模拟器上手动触发冻结→点按钮验证闭环。

### Stage 4：RulesScreen 迁移到组件 + RuleCard 能力补全（2026-08-25）

**本轮修改**：
- `app/src/main/java/com/enlpot/notix/ui/components/RuleCard.kt`（重写）：补全动态底色（accent/onAccent 注入）、重新扫描、长按删除、命中计数重置、禁用态整体 alpha=0.5。视觉层级：匹配条件弱色（bodySecondary / 动态底 onAccent·0.85）、执行动作强色（cardTitle SemiBold / 动态底 onAccent）；分隔线拉开。纯展示，数据由参数注入，组件内不调 `NotificationColorEngine`、不判 Light/Dark。
- `app/src/main/java/com/enlpot/notix/ui/screens/RulesScreen.kt`（迁移）：删除页内私有 `RuleCard`（原 181–379 行），改用组件实例；页面 Token 化（标题/副标题/新建按钮/间距/圆角/字体均用语义 Token `notix`/`notixType`/`notixSpacing`/`notixLayout`/`NotixCorner`）。LazyColumn item 经 `produceState` 调 `NotificationColorEngine.getNotificationColors` 取色注入；动作摘要用 `RuleWizardSupport.actionFlowSummaryFlow` + 超 3 个补 `rule_flow_total_actions`；多来源用 `rule_sources_count`。
- `app/src/main/java/com/enlpot/notix/ui/components/DesignSystemPreview.kt`（追加示例）：新增「Rule Card — 动态底色（accent 注入）」展示段。

**设计要点**：
- 探查结论：原页内 `accent` 变量为死代码，真实文本色用 `primaryTextColor`；组件参数映射 `accent=backgroundColor`、`onAccent=primaryTextColor`。
- 边界：仅迁移 RulesScreen；未动 History/RuleWizard/Settings/SetupWizard、业务逻辑、数据模型、`NotificationColorEngine`、Dialog、死代码、导航框架、依赖。

**验证**：
- `./gradlew.bat assembleDebug --no-daemon` → BUILD SUCCESSFUL（约 59s）；仅既有弃用警告，无新增错误。
- emulator-5554 实机 UIAutomator 行为回归（测试规则：日历 / 包含任一「测试通知」「模拟消息」/ 移除通知）：卡片层级正确、动态底色注入（结构）、长按→删除确认、点击→规则详情、Switch 切换 checked true⇄false（禁用 alpha 代码生效）、重新扫描无崩溃、命中>0 显示「重置命中计数」且可点（重置后回「无命中」）。深浅色 `cmd uimode` 切换正常。
- 截图：`ui-ref/screenshots/stage4_rules_dark.png` / `stage4_rules_light.png`；基线对比 `screen_rules_filled.png` / `screen_rules_filled_light.png`。

### Stage 5：History 页面 Token 化 + NotificationCard 组件补全（2026-08-25）

**本轮修改**：
- `app/src/main/java/com/enlpot/notix/ui/screens/HistoryScreen.kt`（骨架 Token 化 +152/−84）：标题/统计/子 Tab/搜索/暂停监听/分组头/折叠/空态 全 Token 化。圆角 `RoundedCornerShape(16/12/8.dp)` → `NotixCorner.Card/ListItem/Sm`；颜色 `MaterialTheme.colorScheme.{surface,surfaceVariant,onSurface,onSurfaceVariant,outlineVariant,primary}` → `MaterialTheme.notix.{surface,surfaceVariant,contentPrimary,contentSecondary,outlineVariant,primary}`；字体 `headlineMedium+Bold` / `bodyMedium+Medium/SemiBold` → `notixType.{display,button,cardTitle}`（完全等价替换）。**私有 NotificationCard (1633–1807) 保持原样不动**（Stage 6 整体替换）；分组头动态色逻辑保留。增 `import com.enlpot.notix.ui.theme.*`。
- `app/src/main/java/com/enlpot/notix/ui/components/NotificationCard.kt`（组件补全 +47/−）：新增 `blocked: Boolean = false`（右下角 error 底「已过滤」徽标）/ `compact: Boolean = false` + `indent: Dp = 0.dp`（折叠展开态缩宽显示）/ `onHistoryClick: () -> Unit`（计数徽标独立点击，与 `onClick` 分离）。`CountBadge` 改为 `clickable` 接 `onHistoryClick`。纯展示原则保持：数据/颜色/回调全部由参数注入。
- `app/src/main/java/com/enlpot/notix/ui/components/DesignSystemPreview.kt`（追加展示 +50）：新增「Notification Card — 已过滤 (blocked badge)」、「Notification Card — 折叠展开 (compact + indent)」、「Notification Card — 计数徽标独立点击 (onHistoryClick)」三段展示。

**设计要点**：
- 批量替换 `RoundedCornerShape(16/12/8.dp)` → `NotixCorner.*` 时私有 NC 4 处被改回原值，逐一验证恢复；`git diff` 确认 `MainActivity.kt` 无残留、净改动 3 文件 +165/−84。
- dp 间距 → `notixSpacing` 替换延后至 Stage 6：私有 NC 共用相同 dp 值，`replace_all` 会破坏不动的私有 NC；逐处替换对视觉无变化（值与令牌等价）。

**验证**：
- `./gradlew.bat assembleDebug --no-daemon` → BUILD SUCCESSFUL（50s / 46s 两次）；仅既有弃用警告，无新增错误。
- emulator-5554 实机 UIAutomator 行为回归：标题/统计/柱状图正常；子 Tab 切换（按时间/按应用/已过滤）正常；搜索展开正常；暂停监听弹「暂停通知监听？」确认弹窗正常；点击卡片弹详情弹窗（删除/打开/还原/创建规则）正常；已过滤徽标显示正常；深浅色 `cmd uimode` 切换正常。
- 截图：`ui-ref/screenshots/stage5_history_dark.png` / `stage5_history_light.png`；基线对比 `screen_history_filled.png` / `screen_history_filled_light.png`。Token 化前后视觉无破坏（值等价）。
