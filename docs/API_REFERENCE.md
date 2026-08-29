# Notix - API Reference

Method-level reference for the classes in the codebase, matching **v8.15.2** (`com.enlpot.notix`). Signatures are taken directly from source; any drift between this file and the code should be resolved in favor of the code.

## Table of Contents

- [Data Models](#data-models)
- [Core Engine](#core-engine)
- [Storage Classes](#storage-classes)
- [Supporting Subsystems](#supporting-subsystems)
- [UI - Screens](#ui---screens)
- [UI - Components](#ui---components)
- [UI - Theme](#ui---theme)

---

## Data Models

### `BlockerRule` (`BlockerRule.kt`)

`@Keep @Parcelize data class BlockerRule` — a notification rule.

| Field | Type | Default | Description |
|---|---|---|---|
| `id` | `String` | `""` | Stable id; normalized by `RuleIds`. Never re-keyed on update |
| `description` | `String?` | `null` | Optional rule name |
| `isEnabled` | `Boolean` | `true` | Whether the rule is active |
| `hitCount` | `Int` | `0` | Match count |
| `sourcePackages` | `List<SourceApp>` | `emptyList()` | Source apps (multi-select) |
| `condition` | `RuleCondition` | `RuleCondition()` | Keyword matching |
| `extraCondition` | `ExtraCondition` | `ExtraCondition()` | Phone-state conditions |
| `actions` | `List<ActionSpec>` | `emptyList()` | Ordered action chain |
| `createdAt` | `Long` | `0L` | Creation time |

Property: `isValid: Boolean` — true iff ≥1 source app **and** non-empty `actions` **and** every `ActionSpec.isValid`.

### `SourceApp` (`BlockerRule.kt`)

`data class SourceApp(val packageName: String, val appName: String? = null)`.

### `RuleCondition` (`BlockerRule.kt`)

`data class RuleCondition(mode: MatchMode = CONTAINS_ANY, includeKeywords: List<String> = emptyList(), excludeKeywords: List<String> = emptyList())` — `isEmpty()` true when both keyword lists are empty.

### `ExtraCondition` (`BlockerRule.kt`)

`data class ExtraCondition(screenState: ScreenState = ANY, chargingState: ChargingState = ANY, dndState: DndState = ANY, bluetoothState: BluetoothState = ANY, bluetoothDeviceNames: List<String> = emptyList(), time: TimeCondition = TimeCondition())` — `isEmpty()` true when all fields are default.

### `TimeCondition` (`BlockerRule.kt`)

`data class TimeCondition(enabled: Boolean = false, startHour: Int = 0, startMinute: Int = 0, endHour: Int = 23, endMinute: Int = 59, weekdays: List<Int> = emptyList())` — weekdays 1=Mon…7=Sun; empty = every day.

### Enums (`BlockerRule.kt`)

| Enum | Values |
|---|---|
| `MatchMode` | `CONTAINS_ANY, CONTAINS_ALL, NOT_CONTAINS_ANY, NOT_CONTAINS_ALL, MIXED, ADVANCED` |
| `ScreenState` | `ANY, SCREEN_ON, SCREEN_OFF` |
| `ChargingState` | `ANY, WIRED, WIRELESS, BATTERY` |
| `DndState` | `ANY, ON, OFF` |
| `BluetoothState` | `ANY, CONNECTED, DISCONNECTED` |
| `RuleAction` | `DISMISS, CLICK_BUTTON, OPEN_NOTIFICATION, COPY, TTS, STRONG_REMIND, DELAY, POSTPONE` |
| `CopyMode` | `TITLE, TEXT, TITLE_AND_TEXT` |

### Action params (`BlockerRule.kt`)

| Data class | Fields |
|---|---|
| `ActionSpec` | `type: RuleAction`, `params: JsonObject?` (native Gson; `isValid` checks CLICK_BUTTON label, DELAY/POSTPONE duration) |
| `TtsParams` | `template: String?` |
| `CopyParams` | `mode: CopyMode = TITLE_AND_TEXT` |
| `DelayParams` | `durationMs: Long = 1000L` |
| `DismissParams` | `includeOngoing: Boolean = false`, `snoozeDurationMs: Long = DAY_7` |
| `StrongRemindParams` | `sound: Boolean = true`, `vibrate: Boolean = true` |
| `PostponeParams` | `delayMs: Long = 60_000L` |
| `ClickButtonParams` | `buttonLabel: String = ""` |

`object SnoozeDurations`: `HOUR_1=3_600_000`, `DAY_1=86_400_000`, `DAY_7=604_800_000`, `DAY_30=2_592_000_000`, `YEAR_1=31_536_000_000`; `DEFAULT: Long = DAY_7`; `OPTIONS: List<Long>`.

Gson helpers: `fun Any.toParamsJson(): JsonObject` and `inline fun <reified T> JsonObject?.asParams(): T`.

### `SimpleNotification` (`SimpleNotification.kt`)

`@Keep @Parcelize data class SimpleNotification(appLabel: String?, packageName: String?, title: String?, text: String?, timestamp: Long, wasOngoing: Boolean = false, id: String? = UUID.randomUUID().toString(), sbnKey: String? = null, postTime: Long? = null, matchedRuleIds: List<String> = emptyList())`.

### `NotificationHistoryEntry` (`NotificationHistoryEntry.kt`)

`@Keep data class NotificationHistoryEntry(id: String = UUID.randomUUID().toString(), packageName: String? = null, appLabel: String? = null, title: String? = null, count: Int = 1, firstTimestamp: Long = 0L, lastTimestamp: Long = 0L, blocked: Boolean = false, changes: List<SimpleNotification> = emptyList())`.

Properties: `latest: SimpleNotification?` (first of `changes`), `displayCount: String` (real count, no 9+ cap).

---

## Core Engine

### `RuleMatcher` (`RuleMatcher.kt`)

Pure-JVM singleton `object`. No Android dependency.

| Member | Signature | Description |
|---|---|---|
| `evaluate` | `(rule, packageName, title, text, env = EnvironmentSnapshot()): Boolean` | Full single-rule evaluation: valid+enabled → source-app → keyword → extra condition |
| `matchesCondition` | `(condition, title, text): Boolean` | Keyword matching per `MatchMode` (case-insensitive, title-or-text) |
| `matchesExtra` | `(extra, env): Boolean` | Phone-state / time-window checks (cross-midnight aware) |
| `isTimeInRange` | `(hour, minute, startHour, startMinute, endHour, endMinute): Boolean` | Time-range check supporting overnight spans |
| `planNotificationDecision` | `(rules, packageName, title, text, env = EnvironmentSnapshot()): RuleDecision` | First-match-wins decision entry point |

Support types:
- `data class EnvironmentSnapshot(screenOn: Boolean = true, charging: ChargingState = ANY, dndOn: Boolean = false, bluetoothDeviceNames: List<String> = emptyList(), now: Long = System.currentTimeMillis())`
- `sealed interface RuleDecision` → `data object Pass` | `data class Apply(val rule: BlockerRule)`

### `ActionFlowExecutor` (`ActionFlowExecutor.kt`)

Serial action-chain executor.

| Member | Signature | Description |
|---|---|---|
| ctor | `(syncRunner: SyncActionRunner, asyncRunner: AsyncActionRunner, log: (String) -> Unit = {}, hostAlive: () -> Boolean = { true })` | Runners + host-alive check |
| `execute` | `(actions: List<ActionSpec>, context: ActionContext, onComplete: ((FlowResult) -> Unit)? = null): FlowExecution` | Run the chain; per-flow state |

Types in the file:
- `class ActionContext(ruleId, packageName, appName, title, text, notificationKey, postTime, includeOngoing = false, snoozeDurationMs = DAY_7, sbn: StatusBarNotification? = null, notificationActions: Array<Notification.Action>? = null, contentIntent: PendingIntent? = null)`
- `interface ActionFlowHost` — `cancelNotificationCompat(key)`, `snoozeNotificationCompat(key, ruleId?, durationMs)`, `copyToClipboard(text)`, `buildTtsText(template, app, title, text, postTime): String`, `speakTts(ctx, text, onDone: (Boolean) -> Unit)`
- `interface SyncActionRunner` — `dismiss(ctx)`, `clickButton(ctx, spec)`, `openNotification(ctx)`, `copy(ctx, spec)`
- `interface AsyncActionRunner` — `runTts(ctx, spec, onDone: (Boolean) -> Unit)`, `runDelay(delayMs: Long, onComplete: () -> Unit)`
- `class RealSyncActionRunner(host)` — production sync impl
- `class RealAsyncRunner(host)` — production async impl (main-thread `Handler.postDelayed`, TTS via host)
- `class FlowExecution(actions, context)` — `isCompleted/isCancelled/result/failedActions`; `cancel()`
- `enum FlowStatus` — `SUCCESS, PARTIAL_FAILURE, EMPTY, CANCELLED`
- `data class ActionFailure(index: Int, type: RuleAction, reason: String)`
- `class FlowResult(status, failedActions = [], executedCount = 0)` — `isSuccess`, `hasFailures`

> `STRONG_REMIND` and `POSTPONE` are executed as no-ops (logged `skipped (execution TODO)`).

### `NotificationBlockerService` (`NotificationBlockerService.kt`)

`NotificationListenerService` subclass — the processing engine (see ARCHITECTURE §8 for the pipeline).

| Member | Description |
|---|---|
| `const val ACTION_HISTORY_UPDATED = "com.enlpot.notix.HISTORY_UPDATED"` | Broadcast after processing |
| `const val RULE_REPOST_CHANNEL_ID = "rule_repost"` | Channel used in self-package guard (repost fn is dead code) |
| `onNotificationPosted(sbn)` | Main pipeline callback |
| `onListenerConnected()` / `onListenerDisconnected()` | Foreground keep-alive + heartbeat; reconnect (`requestRebind`) unless paused |
| `onDestroy()` | Cancels active flows; delayed executor shutdown |
| `onStartCommand(intent, flags, startId)` | Handles `ACTION_APPLY_RULE` / `ACTION_RESCAN_ALL` / `ACTION_RESTORE_SNOOZED` |
| `companion object { val instance: NotificationBlockerService? }` | Live service reference |

### `NotixApp` (`NotixApp.kt`)

`Application` — `onCreate()` installs `CrashLogManager`, creates the health channel, enqueues `HealthCheckWorker`.

### `MainActivity` (`MainActivity.kt`)

`ComponentActivity` — UI root / state coordinator. Key behaviors: edge-to-edge, data loading + legacy migration, `ACTION_HISTORY_UPDATED` receiver with 400 ms debounced refresh, boolean/state-based navigation, `triggerNotificationAction(...)`, `restoreNotificationToShade(...)`, `Color.luminance(): Float` extension.

---

## Storage Classes

### `RuleStorage` (`RuleStorage.kt`)

File: `{filesDir}/rules.json` via `AtomicFile`. Process-level cache + `Any()` lock; all writes id-keyed.

| Method | Returns | Description |
|---|---|---|
| `getRules()` | `List<BlockerRule>` | Cached/load; normalizes ids, sanitizes null fields, filters invalid, backs up `rules.json.bak` |
| `saveRules(rules)` | `Unit` | Whole-list overwrite (normalizes ids) — prefer id-keyed methods |
| `incrementHitCounts(ruleIds)` | `Unit` | Listener hot path; bump-only mutation |
| `updateRuleById(id, newRule)` | `List<BlockerRule>?` | Replace rule; `null` if id absent |
| `deleteRuleById(id)` | `List<BlockerRule>` | Delete + restore that rule's snoozed ongoing notifications |
| `addRules(rules)` | `List<BlockerRule>` | Append |
| `setEnabledByIds(ids, enabled)` | `List<BlockerRule>` | Toggle a set |
| `setAllEnabled(enabled)` | `List<BlockerRule>` | Toggle all |
| `resetHitCounts()` / `resetHitCounts(ids)` | `Unit` | Reset hit counts |
| `invalidateCache()` | `Unit` | Drop cached rules |

### `NotificationHistoryStorage` (`NotificationHistoryStorage.kt`)

File: `{filesDir}/notification_history.json` via `AtomicFile`; process-level cache; retention `historyDays` (default 5).

| Method | Returns | Description |
|---|---|---|
| `getEntries()` | `List<NotificationHistoryEntry>` | Aggregated entries, reverse-chronological |
| `getHistory()` | `List<SimpleNotification>` | Latest per group (legacy-compatible) |
| `saveNotification(notification, blocked = false)` | `Boolean` | Same-entry dedup (sbnKey+postTime) + head aggregation; true = new group |
| `mergeBlockedNotifications(list)` | `Unit` | Merge legacy blocked history (idempotent) |
| `clearBlockedHistory()` | `Unit` | Remove blocked groups |
| `deleteNotification(notification)` | `Unit` | Remove group with same pkg+title |
| `deleteNotificationsFromPackage(packageName)` | `Unit` | Remove all for a package |
| `updateAppLabelForPackage(packageName, newAppLabel)` | `Unit` | Rename group + changes |
| `clearHistory()` | `Unit` | Delete file + reset cache |
| `clearHistoryBetween(startTime, endTime)` | `Unit` | Prune time range |
| `clearHistoryByPackages(packages)` | `Unit` | Remove listed packages |

### `BlockedNotificationHistoryStorage` (`BlockedNotificationHistoryStorage.kt`)

File: `{filesDir}/blocked_notification_history.json` — **migration-only** (merged at launch, then unused).

| Method | Returns | Description |
|---|---|---|
| `getHistory()` | `List<SimpleNotification>` | Read + prune by `historyDays` |
| `saveNotification(notification)` | `Boolean` | Dedup by content; trim; true if new |
| `deleteNotification(notification)` | `Unit` | Remove entry |
| `clearHistory()` | `Unit` | Delete file |

### `AppInfoStorage` / `AppInfoDatabaseHelper` (`AppInfoStorage.kt`)

SQLite `app_info.db`, table `app_info(package_name TEXT PK, app_name TEXT, app_icon BLOB)`.

| Method | Returns | Description |
|---|---|---|
| `isAppInfoSaved(packageName)` | `String?` | Cached app name or null |
| `saveAppInfo(packageName, appName, icon: Drawable)` | `Unit` | Store/replace (PNG BLOB, `CONFLICT_REPLACE`) |
| `getAppIcon(packageName)` | `Bitmap?` | Decoded icon or null |
| `getAppName(packageName)` | `String?` | Cached name |
| `getAllApps()` | `List<Pair<String, String?>>` | All (package, name) rows |
| `deleteAppInfo(packageName)` | `Unit` | Remove row |
| `clearAllAppInfo()` | `Unit` | Truncate |

### `UnmonitoredAppsStorage` (`UnmonitoredAppsStorage.kt`)

Prefs `unmonitored_apps_prefs`, key `unmonitored_apps` (Gson `Set<String>`), with cache + lock.

| Method | Returns | Description |
|---|---|---|
| `getUnmonitoredApps()` | `Set<String>` | Excluded packages |
| `addApp(packageName)` | `Unit` | Add |
| `removeApp(packageName)` | `Unit` | Remove |
| `isAppUnmonitored(packageName)` | `Boolean` | Contains check |

### `StatsStorage` (`StatsStorage.kt`)

Prefs `stats`.

| Method | Returns | Description |
|---|---|---|
| `getBlockedNotificationsCount()` | `Int` | Total blocked |
| `incrementBlockedNotificationsCount()` | `Unit` | Bump (locked read-modify-write) |
| `recordNotification(timestamp)` | `Unit` | Per-day count (key `day_yyyy-MM-dd`), trims >400 days |
| `getCountForDay(date: LocalDate)` | `Int` | Day count |
| `getWeekCounts(weekStart: LocalDate)` | `List<Pair<LocalDate, Int>>` | 7 daily counts for a week |
| `clearDay(date: LocalDate)` | `Unit` | Remove a day key |

### `NotificationActionRepository` (`NotificationActionRepository.kt`)

Singleton `object` with `ConcurrentHashMap<String, PendingIntent>` — `saveAction(id, action)`, `getAction(id): PendingIntent?`, `clear()`. In-memory only.

### `CrashLogManager` (`CrashLogManager.kt`)

Singleton `object` — crash log at `{getExternalFilesDir(null) ?: filesDir}/crash_logs.txt`, max 20 entries.

| Method | Returns | Description |
|---|---|---|
| `install(context)` | `Unit` | Install uncaught-exception handler |
| `isEnabled(context)` / `setEnabled(context, enabled)` | `Boolean` / `Unit` | Toggle (pref `crash_log_enabled`) |
| `hasCrashes(context)` | `Boolean` | Log non-empty |
| `readLogs(context)` | `String` | Full log text |
| `clearLogs(context)` | `Unit` | Clear |
| `logFile(context)` | `File` | Current log file |
| `migrateLegacyLog(context)` | `Unit` | Migrate from filesDir |

---

## Supporting Subsystems

### `TtsSpeaker` (`TtsSpeaker.kt`)

Singleton `object` — `speak(context, text, onDone: ((Boolean) -> Unit)? = null)`, `shutdown()`. Queue (max 20 pending), concurrent callback registry, main-thread handler, simplified-Chinese-preferred locale selection.

### `RemoteViewsTextExtractor` (`RemoteViewsTextExtractor.kt`)

Singleton `object` — `extract(notification: Notification): String?`. Reflection into `RemoteViews.mActions` collecting `setText`/`setContentDescription` values + notification action titles. **Off by default** (privacy).

### `NotificationColorEngine` (`NotificationColorEngine.kt`)

`object NotificationColorEngine` — `getNotificationColors(context, packageName?): NotificationColors`, `clearCache()`, `chooseTextColor(bg): Int`, `contrastRatio(fg, bg): Float`. `data class NotificationColors(backgroundColor, primaryColor, secondaryColor?, primaryTextColor, secondaryTextColor, accentColor, contrastRatio)`. Uses icon sampling with hash fallback (v8.15.2); cache max 256 keyed by package+lastUpdateTime.

### `RuleIds` (`RuleIds.kt`)

Singleton `object` — `isValid(id): Boolean`, `normalizeIds(rules): List<BlockerRule>`, `rulesJsonHasAllIds(json): Boolean`, `needsNormalizing(rules): Boolean`, `newId(): String`.

### `RuleMutations` (`RuleMutations.kt`)

Singleton `object` (pure) — `applyHitCounts`, `applyUpdate`, `applyDelete`, `applyAdd`, `applySetEnabled`, `applySetAllEnabled`, `applyResetHitCounts` (×2). All preserve `id` (never re-key).

### `RuleImport` / `RuleExport` (`RuleImport.kt`)

`const val RULE_EXPORT_VERSION = 4`. `data class RuleExport(version, locale?, rules)`; `object RuleExportSerializer.toJson(export): String`. `object RuleImport.parse(json): ImportResult` — `ImportResult` = `Success(rules, locale, droppedCount)` | `Error(reason: ImportError)` where `ImportError = TooLarge | Malformed | SchemaMismatch | Empty`. Sanitizes and normalizes on import.

### `RuleWizardSupport` (`RuleWizardSupport.kt`)

Singleton `object` (pure) — `data class KnownApp(packageName, appName?, isQueryableInstalled)`. Helpers: `mergeKnownApps(...)`, `isDuplicate(...)`, `actionFlowEquals(a, b)`, `looksLikePackageName(input)`, action-flow list ops (`actionFlowAdd/RemoveAt/MoveUp/MoveDown/Update/Move`), `canMoveUp/canMoveDown`, `canSaveFlow(actions)`, `hasActionParams(type)`, `defaultParamsFor(type)`, per-action `*Spec(...)` builders, `actionFlowSummary(spec)`, `actionFlowSummaryFlow(actions, maxShown = 3)`, `formatSnoozeDuration(ms)`.

### `health/HealthCheckWorker` (`HealthCheckWorker.kt`)

`class HealthCheckWorker` (Worker) — `companion object`: `CHANNEL_ID = "health"`, `EXTRA_OPEN_WIZARD`, `enqueue(context)` (periodic 6 h, unique work `health-check`). Flags a high-importance notification when the listener hasn't connected in 24 h (throttle 24 h).

### `setup/SetupState` (`SetupState.kt`)

Singleton `object` — `CURRENT_SETUP_VERSION = 1`; `isNotificationListenerEnabled(ctx)`, `isPostNotificationsGranted(ctx)`, `needsPostNotificationsStep(ctx)`, `isIgnoringBatteryOptimizations(ctx)`, `hasSeenOemAutostart/markOemAutostartSeen`, `lastSeenSetupVersion/setLastSeenSetupVersion`, `shouldShowSetupWizard(ctx)`, `lastListenerConnectedMs(ctx)`, `recordListenerConnected(ctx)`. Keys: `last_listener_connected_ms`, `last_unhealthy_notif_ms`, etc.

### `setup/OemAutostart` (`OemAutostart.kt`)

Singleton `object` — `enum Vendor { XIAOMI, HUAWEI, OPPO, ONEPLUS, VIVO, SAMSUNG, ASUS, LETV, MEIZU, NOKIA }`; `currentVendor(): Vendor?`, `applies(): Boolean`, `tryLaunchAutostart(context): Boolean`.

### `ExternalLinks` (`ExternalLinks.kt`)

`object ExternalLinks.open(context, url): Boolean`.

---

## UI - Screens

All in `ui/screens/`, Compose `@Composable` functions.

| Screen | Signature (params) | Purpose |
|---|---|---|
| `HistoryScreen` | `(notifications, unmonitoredApps, onNotificationClick, onClearHistory, ...)`, plus tab/selection/search state | History tab: aggregated cards, chart panel, sub-tabs (time/app/rule), search, fold segments |
| `RulesScreen` | `(rules, onRuleClick, onCreateRuleClick, onToggleAllRules, onDeleteRule, onToggleRule, onResetHitCount, onRescanRule)` | Rules tab: rule cards |
| `RuleWizardScreen` | `(existingRules, pastNotifications, onClose, onCreateRule, editingRule?, onUpdateRule?, onDeleteRule?, prefillNotification?)` | Rule wizard (app → condition → state → action flow) |
| `SettingsScreen` | `(onBack, ...)` | Settings |
| `SetupWizardScreen` | `(onComplete, onOpenNotificationSettings, ...)` | Onboarding wizard |
| `StorageUsageScreen` | — | Storage usage + cleanup |

---

## UI - Components

`ui/components/` — reusable composables:

- `NotixDialog` / `NotixConfirmDialog` — unified dialog system
- `NotificationCard` / `RuleCard` / `SettingRow` / `SectionHeader` / `EmptyState` / `SearchField` / `Chip` / `Buttons` / `RealAppIcon`
- `HistoryNotificationDetailsDialog` — history details (Open / Create Rule / copy on long-press)
- `NotificationDetailDialog` — notification details
- `CrashLogDialog` — crash log viewer
- `DesignSystemPreview` — design-system preview

---

## UI - Theme

`ui/theme/`:

- `NotixTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false, content)` — Material 3 theme; provides semantic tokens via `CompositionLocal`: `notixColors / notixType / notixSpacing / notixLayout / notixElevation`, read through `MaterialTheme.notix*`.
- `Color.kt` / `NotixColorScheme.kt` — Light/Dark color schemes + token definitions.
- `Type.kt` — typography tokens. `Spacing.kt` / `Layout.kt` / `Shape.kt` / `Elevation.kt` — token sets.
