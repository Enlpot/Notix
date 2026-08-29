# Notix — API 参考

代码库中各类的方法级参考，对应 **v8.15.2**（`com.enlpot.notix`）。签名直接取自源码；如与代码不一致，以代码为准。

## 目录

- [数据模型](#数据模型)
- [核心引擎](#核心引擎)
- [存储类](#存储类)
- [支撑子系统](#支撑子系统)
- [UI - 屏面](#ui---屏面)
- [UI - 组件](#ui---组件)
- [UI - 主题](#ui---主题)

---

## 数据模型

### `BlockerRule`（`BlockerRule.kt`）

`@Keep @Parcelize data class BlockerRule` — 通知规则。

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `id` | `String` | `""` | 稳定 id，由 `RuleIds` 规范化；更新时禁止重键 |
| `description` | `String?` | `null` | 可选规则名 |
| `isEnabled` | `Boolean` | `true` | 是否启用 |
| `hitCount` | `Int` | `0` | 命中次数 |
| `sourcePackages` | `List<SourceApp>` | `emptyList()` | 来源 App（多选） |
| `condition` | `RuleCondition` | `RuleCondition()` | 关键字匹配 |
| `extraCondition` | `ExtraCondition` | `ExtraCondition()` | 手机状态条件 |
| `actions` | `List<ActionSpec>` | `emptyList()` | 顺序动作链 |
| `createdAt` | `Long` | `0L` | 创建时间 |

属性：`isValid: Boolean` — 至少 1 个来源 App **且** `actions` 非空 **且** 每个 `ActionSpec.isValid`。

### `SourceApp`（`BlockerRule.kt`）

`data class SourceApp(val packageName: String, val appName: String? = null)`。

### `RuleCondition`（`BlockerRule.kt`）

`data class RuleCondition(mode: MatchMode = CONTAINS_ANY, includeKeywords: List<String> = emptyList(), excludeKeywords: List<String> = emptyList())` — 两关键字列表皆空时 `isEmpty()` 为 true。

### `ExtraCondition`（`BlockerRule.kt`）

`data class ExtraCondition(screenState: ScreenState = ANY, chargingState: ChargingState = ANY, dndState: DndState = ANY, bluetoothState: BluetoothState = ANY, bluetoothDeviceNames: List<String> = emptyList(), time: TimeCondition = TimeCondition())` — 全默认时 `isEmpty()` 为 true。

### `TimeCondition`（`BlockerRule.kt`）

`data class TimeCondition(enabled: Boolean = false, startHour: Int = 0, startMinute: Int = 0, endHour: Int = 23, endMinute: Int = 59, weekdays: List<Int> = emptyList())` — weekdays 1=周一…7=周日；空 = 每天。

### 枚举（`BlockerRule.kt`）

| 枚举 | 取值 |
|---|---|
| `MatchMode` | `CONTAINS_ANY, CONTAINS_ALL, NOT_CONTAINS_ANY, NOT_CONTAINS_ALL, MIXED, ADVANCED` |
| `ScreenState` | `ANY, SCREEN_ON, SCREEN_OFF` |
| `ChargingState` | `ANY, WIRED, WIRELESS, BATTERY` |
| `DndState` | `ANY, ON, OFF` |
| `BluetoothState` | `ANY, CONNECTED, DISCONNECTED` |
| `RuleAction` | `DISMISS, CLICK_BUTTON, OPEN_NOTIFICATION, COPY, TTS, STRONG_REMIND, DELAY, POSTPONE` |
| `CopyMode` | `TITLE, TEXT, TITLE_AND_TEXT` |

### 动作参数（`BlockerRule.kt`）

| 数据类 | 字段 |
|---|---|
| `ActionSpec` | `type: RuleAction`、`params: JsonObject?`（原生 Gson；`isValid` 校验 CLICK_BUTTON label、DELAY/POSTPONE 时长） |
| `TtsParams` | `template: String?` |
| `CopyParams` | `mode: CopyMode = TITLE_AND_TEXT` |
| `DelayParams` | `durationMs: Long = 1000L` |
| `DismissParams` | `includeOngoing: Boolean = false`、`snoozeDurationMs: Long = DAY_7` |
| `StrongRemindParams` | `sound: Boolean = true`、`vibrate: Boolean = true` |
| `PostponeParams` | `delayMs: Long = 60_000L` |
| `ClickButtonParams` | `buttonLabel: String = ""` |

`object SnoozeDurations`：`HOUR_1=3_600_000`、`DAY_1=86_400_000`、`DAY_7=604_800_000`、`DAY_30=2_592_000_000`、`YEAR_1=31_536_000_000`；`DEFAULT: Long = DAY_7`；`OPTIONS: List<Long>`。

Gson 互转：`fun Any.toParamsJson(): JsonObject`、`inline fun <reified T> JsonObject?.asParams(): T`。

### `SimpleNotification`（`SimpleNotification.kt`）

`@Keep @Parcelize data class SimpleNotification(appLabel: String?, packageName: String?, title: String?, text: String?, timestamp: Long, wasOngoing: Boolean = false, id: String? = UUID.randomUUID().toString(), sbnKey: String? = null, postTime: Long? = null, matchedRuleIds: List<String> = emptyList())`。

### `NotificationHistoryEntry`（`NotificationHistoryEntry.kt`）

`@Keep data class NotificationHistoryEntry(id: String = UUID.randomUUID().toString(), packageName: String? = null, appLabel: String? = null, title: String? = null, count: Int = 1, firstTimestamp: Long = 0L, lastTimestamp: Long = 0L, blocked: Boolean = false, changes: List<SimpleNotification> = emptyList())`。

属性：`latest: SimpleNotification?`（`changes` 首项）、`displayCount: String`（真实次数，无 9+ 上限）。

---

## 核心引擎

### `RuleMatcher`（`RuleMatcher.kt`）

纯 JVM 单例 `object`，无 Android 依赖。

| 成员 | 签名 | 说明 |
|---|---|---|
| `evaluate` | `(rule, packageName, title, text, env = EnvironmentSnapshot()): Boolean` | 单条规则完整判定：有效+启用 → 来源 App → 关键字 → 额外条件 |
| `matchesCondition` | `(condition, title, text): Boolean` | 按 `MatchMode` 关键字匹配（忽略大小写，标题或正文） |
| `matchesExtra` | `(extra, env): Boolean` | 手机状态 / 时间段判断（支持跨天） |
| `isTimeInRange` | `(hour, minute, startHour, startMinute, endHour, endMinute): Boolean` | 时间区间判断，支持跨天 |
| `planNotificationDecision` | `(rules, packageName, title, text, env = EnvironmentSnapshot()): RuleDecision` | 首条命中优先的决策入口 |

辅助类型：
- `data class EnvironmentSnapshot(screenOn: Boolean = true, charging: ChargingState = ANY, dndOn: Boolean = false, bluetoothDeviceNames: List<String> = emptyList(), now: Long = System.currentTimeMillis())`
- `sealed interface RuleDecision` → `data object Pass` | `data class Apply(val rule: BlockerRule)`

### `ActionFlowExecutor`（`ActionFlowExecutor.kt`）

动作链串行执行引擎。

| 成员 | 签名 | 说明 |
|---|---|---|
| 构造 | `(syncRunner: SyncActionRunner, asyncRunner: AsyncActionRunner, log: (String) -> Unit = {}, hostAlive: () -> Boolean = { true })` | 执行体 + 宿主存活检查 |
| `execute` | `(actions: List<ActionSpec>, context: ActionContext, onComplete: ((FlowResult) -> Unit)? = null): FlowExecution` | 执行动作链；每次独立 Flow 状态 |

文件内类型：
- `class ActionContext(ruleId, packageName, appName, title, text, notificationKey, postTime, includeOngoing = false, snoozeDurationMs = DAY_7, sbn: StatusBarNotification? = null, notificationActions: Array<Notification.Action>? = null, contentIntent: PendingIntent? = null)`
- `interface ActionFlowHost` — `cancelNotificationCompat(key)`、`snoozeNotificationCompat(key, ruleId?, durationMs)`、`copyToClipboard(text)`、`buildTtsText(template, app, title, text, postTime): String`、`speakTts(ctx, text, onDone: (Boolean) -> Unit)`
- `interface SyncActionRunner` — `dismiss(ctx)`、`clickButton(ctx, spec)`、`openNotification(ctx)`、`copy(ctx, spec)`
- `interface AsyncActionRunner` — `runTts(ctx, spec, onDone: (Boolean) -> Unit)`、`runDelay(delayMs: Long, onComplete: () -> Unit)`
- `class RealSyncActionRunner(host)` — 生产同步执行体
- `class RealAsyncRunner(host)` — 生产异步执行体（主线程 `Handler.postDelayed`、TTS 桥接 host）
- `class FlowExecution(actions, context)` — `isCompleted/isCancelled/result/failedActions`；`cancel()`
- `enum FlowStatus` — `SUCCESS, PARTIAL_FAILURE, EMPTY, CANCELLED`
- `data class ActionFailure(index: Int, type: RuleAction, reason: String)`
- `class FlowResult(status, failedActions = [], executedCount = 0)` — `isSuccess`、`hasFailures`

> `STRONG_REMIND` 与 `POSTPONE` 作为空操作执行（记日志 `skipped (execution TODO)`）。

### `NotificationBlockerService`（`NotificationBlockerService.kt`）

`NotificationListenerService` 子类 — 处理引擎（流水线详见 ARCHITECTURE §8）。

| 成员 | 说明 |
|---|---|
| `const val ACTION_HISTORY_UPDATED = "com.enlpot.notix.HISTORY_UPDATED"` | 处理后广播 |
| `const val RULE_REPOST_CHANNEL_ID = "rule_repost"` | 自包名守卫用渠道（重发函数为死代码） |
| `onNotificationPosted(sbn)` | 主流水线回调 |
| `onListenerConnected()` / `onListenerDisconnected()` | 前台保活 + 心跳；断线重连（`requestRebind`）除非暂停 |
| `onDestroy()` | 取消进行中的 Flow；延迟关闭线程池 |
| `onStartCommand(intent, flags, startId)` | 处理 `ACTION_APPLY_RULE` / `ACTION_RESCAN_ALL` / `ACTION_RESTORE_SNOOZED` |
| `companion object { val instance: NotificationBlockerService? }` | 存活服务引用 |

### `NotixApp`（`NotixApp.kt`）

`Application` — `onCreate()` 安装 `CrashLogManager`、创建健康渠道、排队 `HealthCheckWorker`。

### `MainActivity`（`MainActivity.kt`）

`ComponentActivity` — UI 根 / 状态协调者。关键行为：edge-to-edge、数据加载与旧历史迁移、`ACTION_HISTORY_UPDATED` 接收器（400ms 去抖刷新）、状态式导航、`triggerNotificationAction(...)`、`restoreNotificationToShade(...)`、`Color.luminance(): Float` 扩展。

---

## 存储类

### `RuleStorage`（`RuleStorage.kt`）

文件：`{filesDir}/rules.json`（`AtomicFile`）。进程级缓存 + `Any()` 锁；所有写入 id-keyed。

| 方法 | 返回 | 说明 |
|---|---|---|
| `getRules()` | `List<BlockerRule>` | 缓存/加载；规范化 id、兜底空字段、过滤无效规则、备份 `rules.json.bak` |
| `saveRules(rules)` | `Unit` | 整表覆盖（规范化 id）— 优先用 id-keyed 方法 |
| `incrementHitCounts(ruleIds)` | `Unit` | 监听器热路径；仅累加 |
| `updateRuleById(id, newRule)` | `List<BlockerRule>?` | 替换规则；id 不存在返回 `null` |
| `deleteRuleById(id)` | `List<BlockerRule>` | 删除 + 恢复该规则冻结的常驻通知 |
| `addRules(rules)` | `List<BlockerRule>` | 追加 |
| `setEnabledByIds(ids, enabled)` | `List<BlockerRule>` | 批量开关 |
| `setAllEnabled(enabled)` | `List<BlockerRule>` | 全量开关 |
| `resetHitCounts()` / `resetHitCounts(ids)` | `Unit` | 重置命中数 |
| `invalidateCache()` | `Unit` | 丢弃缓存 |

### `NotificationHistoryStorage`（`NotificationHistoryStorage.kt`）

文件：`{filesDir}/notification_history.json`（`AtomicFile`）；进程级缓存；保留期 `historyDays`（默认 5）。

| 方法 | 返回 | 说明 |
|---|---|---|
| `getEntries()` | `List<NotificationHistoryEntry>` | 聚合条目，按时间倒序 |
| `getHistory()` | `List<SimpleNotification>` | 每组取最新（兼容旧调用方） |
| `saveNotification(notification, blocked = false)` | `Boolean` | 同条去重（sbnKey+postTime）+ 头部聚合；true = 新建组 |
| `mergeBlockedNotifications(list)` | `Unit` | 合并旧版被拦历史（幂等） |
| `clearBlockedHistory()` | `Unit` | 删除被拦聚合组 |
| `deleteNotification(notification)` | `Unit` | 删除同 pkg+同标题组 |
| `deleteNotificationsFromPackage(packageName)` | `Unit` | 删除某 pkg 全部 |
| `updateAppLabelForPackage(packageName, newAppLabel)` | `Unit` | 更新组内 app 名称 |
| `clearHistory()` | `Unit` | 删文件 + 重置缓存 |
| `clearHistoryBetween(startTime, endTime)` | `Unit` | 按时间区间裁剪 |
| `clearHistoryByPackages(packages)` | `Unit` | 删除指定包集合 |

### `BlockedNotificationHistoryStorage`（`BlockedNotificationHistoryStorage.kt`）

文件：`{filesDir}/blocked_notification_history.json` — **仅迁移用**（启动合并后不再使用）。

| 方法 | 返回 | 说明 |
|---|---|---|
| `getHistory()` | `List<SimpleNotification>` | 读取并按 `historyDays` 裁剪 |
| `saveNotification(notification)` | `Boolean` | 内容去重；裁剪；true = 新条目 |
| `deleteNotification(notification)` | `Unit` | 删除条目 |
| `clearHistory()` | `Unit` | 删文件 |

### `AppInfoStorage` / `AppInfoDatabaseHelper`（`AppInfoStorage.kt`）

SQLite `app_info.db`，表 `app_info(package_name TEXT PK, app_name TEXT, app_icon BLOB)`。

| 方法 | 返回 | 说明 |
|---|---|---|
| `isAppInfoSaved(packageName)` | `String?` | 缓存名称或 null |
| `saveAppInfo(packageName, appName, icon: Drawable)` | `Unit` | 存储/替换（PNG BLOB，`CONFLICT_REPLACE`） |
| `getAppIcon(packageName)` | `Bitmap?` | 解码图标或 null |
| `getAppName(packageName)` | `String?` | 缓存名称 |
| `getAllApps()` | `List<Pair<String, String?>>` | 全部 (包名, 名称) |
| `deleteAppInfo(packageName)` | `Unit` | 删除行 |
| `clearAllAppInfo()` | `Unit` | 清空表 |

### `UnmonitoredAppsStorage`（`UnmonitoredAppsStorage.kt`）

Prefs `unmonitored_apps_prefs`，key `unmonitored_apps`（Gson `Set<String>`），带缓存与锁。

| 方法 | 返回 | 说明 |
|---|---|---|
| `getUnmonitoredApps()` | `Set<String>` | 未监控包集合 |
| `addApp(packageName)` | `Unit` | 添加 |
| `removeApp(packageName)` | `Unit` | 移除 |
| `isAppUnmonitored(packageName)` | `Boolean` | 是否包含 |

### `StatsStorage`（`StatsStorage.kt`）

Prefs `stats`。

| 方法 | 返回 | 说明 |
|---|---|---|
| `getBlockedNotificationsCount()` | `Int` | 拦截总数 |
| `incrementBlockedNotificationsCount()` | `Unit` | 累加（加锁读改写） |
| `recordNotification(timestamp)` | `Unit` | 按日计数（key `day_yyyy-MM-dd`），裁剪 >400 天 |
| `getCountForDay(date: LocalDate)` | `Int` | 某天计数 |
| `getWeekCounts(weekStart: LocalDate)` | `List<Pair<LocalDate, Int>>` | 一周 7 天计数 |
| `clearDay(date: LocalDate)` | `Unit` | 删除某天 key |

### `NotificationActionRepository`（`NotificationActionRepository.kt`）

单例 `object` + `ConcurrentHashMap<String, PendingIntent>` — `saveAction(id, action)`、`getAction(id): PendingIntent?`、`clear()`。仅内存。

### `CrashLogManager`（`CrashLogManager.kt`）

单例 `object` — 崩溃日志位于 `{getExternalFilesDir(null) ?: filesDir}/crash_logs.txt`，最多 20 条。

| 方法 | 返回 | 说明 |
|---|---|---|
| `install(context)` | `Unit` | 安装未捕获异常处理器 |
| `isEnabled(context)` / `setEnabled(context, enabled)` | `Boolean` / `Unit` | 开关（pref `crash_log_enabled`） |
| `hasCrashes(context)` | `Boolean` | 日志非空 |
| `readLogs(context)` | `String` | 全文 |
| `clearLogs(context)` | `Unit` | 清空 |
| `logFile(context)` | `File` | 当前日志文件 |
| `migrateLegacyLog(context)` | `Unit` | 从 filesDir 迁移 |

---

## 支撑子系统

### `TtsSpeaker`（`TtsSpeaker.kt`）

单例 `object` — `speak(context, text, onDone: ((Boolean) -> Unit)? = null)`、`shutdown()`。队列（最多 20 条）、并发回调表、主线程 handler、简体中文优先的引擎语言选择。

### `RemoteViewsTextExtractor`（`RemoteViewsTextExtractor.kt`）

单例 `object` — `extract(notification: Notification): String?`。反射进 `RemoteViews.mActions` 收集 `setText`/`setContentDescription` 值 + 通知按钮标题。**默认关闭**（隐私）。

### `NotificationColorEngine`（`NotificationColorEngine.kt`）

`object NotificationColorEngine` — `getNotificationColors(context, packageName?): NotificationColors`、`clearCache()`、`chooseTextColor(bg): Int`、`contrastRatio(fg, bg): Float`。`data class NotificationColors(backgroundColor, primaryColor, secondaryColor?, primaryTextColor, secondaryTextColor, accentColor, contrastRatio)`。图标采样 + hash 兜底（v8.15.2）；缓存最大 256，key=包名+lastUpdateTime。

### `RuleIds`（`RuleIds.kt`）

单例 `object` — `isValid(id): Boolean`、`normalizeIds(rules): List<BlockerRule>`、`rulesJsonHasAllIds(json): Boolean`、`needsNormalizing(rules): Boolean`、`newId(): String`。

### `RuleMutations`（`RuleMutations.kt`）

单例 `object`（纯函数）— `applyHitCounts`、`applyUpdate`、`applyDelete`、`applyAdd`、`applySetEnabled`、`applySetAllEnabled`、`applyResetHitCounts`（×2）。全部保留 `id`（不重键）。

### `RuleImport` / `RuleExport`（`RuleImport.kt`）

`const val RULE_EXPORT_VERSION = 4`。`data class RuleExport(version, locale?, rules)`；`object RuleExportSerializer.toJson(export): String`。`object RuleImport.parse(json): ImportResult` — `ImportResult` = `Success(rules, locale, droppedCount)` | `Error(reason: ImportError)`，`ImportError = TooLarge | Malformed | SchemaMismatch | Empty`。导入时净化与规范化。

### `RuleWizardSupport`（`RuleWizardSupport.kt`）

单例 `object`（纯函数）— `data class KnownApp(packageName, appName?, isQueryableInstalled)`。辅助：`mergeKnownApps(...)`、`isDuplicate(...)`、`actionFlowEquals(a, b)`、`looksLikePackageName(input)`、动作链列表操作（`actionFlowAdd/RemoveAt/MoveUp/MoveDown/Update/Move`）、`canMoveUp/canMoveDown`、`canSaveFlow(actions)`、`hasActionParams(type)`、`defaultParamsFor(type)`、各动作 `*Spec(...)` 构造器、`actionFlowSummary(spec)`、`actionFlowSummaryFlow(actions, maxShown = 3)`、`formatSnoozeDuration(ms)`。

### `health/HealthCheckWorker`（`HealthCheckWorker.kt`）

`class HealthCheckWorker`（Worker）— `companion object`：`CHANNEL_ID = "health"`、`EXTRA_OPEN_WIZARD`、`enqueue(context)`（周期 6 小时，唯一任务 `health-check`）。监听 24h 未连接且已授权时弹高优通知（24h 节流）。

### `setup/SetupState`（`SetupState.kt`）

单例 `object` — `CURRENT_SETUP_VERSION = 1`；`isNotificationListenerEnabled(ctx)`、`isPostNotificationsGranted(ctx)`、`needsPostNotificationsStep(ctx)`、`isIgnoringBatteryOptimizations(ctx)`、`hasSeenOemAutostart/markOemAutostartSeen`、`lastSeenSetupVersion/setLastSeenSetupVersion`、`shouldShowSetupWizard(ctx)`、`lastListenerConnectedMs(ctx)`、`recordListenerConnected(ctx)`。键：`last_listener_connected_ms`、`last_unhealthy_notif_ms` 等。

### `setup/OemAutostart`（`OemAutostart.kt`）

单例 `object` — `enum Vendor { XIAOMI, HUAWEI, OPPO, ONEPLUS, VIVO, SAMSUNG, ASUS, LETV, MEIZU, NOKIA }`；`currentVendor(): Vendor?`、`applies(): Boolean`、`tryLaunchAutostart(context): Boolean`。

### `ExternalLinks`（`ExternalLinks.kt`）

`object ExternalLinks.open(context, url): Boolean`。

---

## UI - 屏面

均在 `ui/screens/`，Compose `@Composable` 函数。

| 屏面 | 签名（参数） | 用途 |
|---|---|---|
| `HistoryScreen` | `(notifications, unmonitoredApps, onNotificationClick, onClearHistory, ...)`，另有 tab/筛选/搜索状态 | 历史 Tab：聚合卡片、图表面板、子 Tab（时间/应用/规则）、搜索、折叠分段 |
| `RulesScreen` | `(rules, onRuleClick, onCreateRuleClick, onToggleAllRules, onDeleteRule, onToggleRule, onResetHitCount, onRescanRule)` | 规则 Tab：规则卡片 |
| `RuleWizardScreen` | `(existingRules, pastNotifications, onClose, onCreateRule, editingRule?, onUpdateRule?, onDeleteRule?, prefillNotification?)` | 规则向导（App → 条件 → 状态 → 动作流） |
| `SettingsScreen` | `(onBack, ...)` | 设置 |
| `SetupWizardScreen` | `(onComplete, onOpenNotificationSettings, ...)` | 引导向导 |
| `StorageUsageScreen` | — | 存储占用 + 清理 |

---

## UI - 组件

`ui/components/` — 可复用组件：

- `NotixDialog` / `NotixConfirmDialog` — 统一弹窗体系
- `NotificationCard` / `RuleCard` / `SettingRow` / `SectionHeader` / `EmptyState` / `SearchField` / `Chip` / `Buttons` / `RealAppIcon`
- `HistoryNotificationDetailsDialog` — 历史详情（打开 / 创建规则 / 长按复制）
- `NotificationDetailDialog` — 通知详情
- `CrashLogDialog` — 崩溃日志查看
- `DesignSystemPreview` — 设计体系预览

---

## UI - 主题

`ui/theme/`：

- `NotixTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false, content)` — Material 3 主题；经 `CompositionLocal` 提供语义令牌：`notixColors / notixType / notixSpacing / notixLayout / notixElevation`，页面经 `MaterialTheme.notix*` 读取。
- `Color.kt` / `NotixColorScheme.kt` — 亮/暗配色 + 令牌定义。
- `Type.kt` — 字体令牌。`Spacing.kt` / `Layout.kt` / `Shape.kt` / `Elevation.kt` — 各类令牌集。
