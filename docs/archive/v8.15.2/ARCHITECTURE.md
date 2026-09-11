# Notix — Architecture & Codebase Documentation

> This document corresponds to **v8.15.2** (versionCode 131) of `com.enlpot.notix`. Every file, class and method name below maps to the actual source under `app/src/main/java/com/enlpot/notix/`. If this document conflicts with an older version of the docs, this file wins.

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Project Structure](#2-project-structure)
- [3. Build System & Configuration](#3-build-system--configuration)
- [4. Runtime Entry Points](#4-runtime-entry-points)
- [5. Data Models](#5-data-models)
- [6. Rule Decision Engine (RuleMatcher)](#6-rule-decision-engine-rulematcher)
- [7. Action Flow Executor (ActionFlowExecutor)](#7-action-flow-executor-actionflowexecutor)
- [8. Notification Processing Pipeline (NotificationBlockerService)](#8-notification-processing-pipeline-notificationblockerservice)
- [9. Storage Layer](#9-storage-layer)
- [10. Supporting Subsystems](#10-supporting-subsystems)
- [11. UI Layer](#11-ui-layer)
- [12. Concurrency & Consistency Model](#12-concurrency--consistency-model)
- [13. Privacy & Offline Design](#13-privacy--offline-design)
- [14. Testing](#14-testing)
- [15. Android Manifest & Permissions](#15-android-manifest--permissions)
- [16. Known Placeholders & Limitations](#16-known-placeholders--limitations)

---

## 1. Project Overview

Notix is a **single-module** Android app: it listens to system notifications, evaluates them against user-defined rules, and acts on each notification by running the rule's **action chain** (dismiss / click a button / open / copy / TTS speak / delay…). It runs entirely offline and declares no network permission.

| Property | Value |
|---|---|
| Package / applicationId | `com.enlpot.notix` |
| Language | Kotlin (100%) |
| UI framework | Jetpack Compose + Material 3 |
| Min SDK | 24 (Android 7.0) |
| Target / compile SDK | 36 |
| Java target | 11 |
| Current version | 8.15.2 (versionCode 131) |
| License | MIT |
| AGP / Kotlin | 8.13.2 / 2.0.21 |

### Key dependencies (`gradle/libs.versions.toml`)

| Dependency | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | 2024.09.00 | UI framework |
| Material 3 | 1.4.0 | Design system |
| Material Icons Extended | 1.7.8 | Icon library |
| Gson | 2.13.2 | JSON serialization |
| Accompanist System UI Controller | 0.36.0 | Status bar styling |
| WorkManager (`work-runtime-ktx`) | 2.9.1 | Periodic health check |
| reorderable | 2.4.3 | Drag-and-drop reorder of action chains |
| core-ktx / activity-compose / lifecycle-runtime-ktx | 1.10.1 / 1.8.0 / 2.6.1 | AndroidX basics |

---

## 2. Project Structure

```
Notix/
├── .github/workflows/release.yml     # CI: auto-build + GitHub Release (credentials via Secrets)
├── app/
│   ├── build.gradle.kts              # App build config (signing, R8, Compose)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/enlpot/notix/
│       │   │   ├── NotixApp.kt                  # Application: crash log + health channel + health check
│       │   │   ├── MainActivity.kt              # UI root: state holder, broadcast refresh, navigation
│       │   │   ├── NotificationBlockerService.kt# Listener engine: foreground keep-alive + pipeline
│       │   │   ├── BlockerRule.kt               # Rule model + enums + per-action params
│       │   │   ├── RuleMatcher.kt               # Pure-JVM decision engine + EnvironmentSnapshot
│       │   │   ├── ActionFlowExecutor.kt        # Serial action-chain executor + runner interfaces
│       │   │   ├── RuleStorage.kt               # Rule persistence (AtomicFile + locking)
│       │   │   ├── RuleIds.kt                   # Stable rule-id normalization
│       │   │   ├── RuleMutations.kt             # Pure rule mutation helpers
│       │   │   ├── RuleImport.kt                # v4 envelope import/export + sanitization
│       │   │   ├── RuleWizardSupport.kt         # Wizard summaries / known-app merge helpers
│       │   │   ├── SimpleNotification.kt        # Notification snapshot model
│       │   │   ├── NotificationHistoryEntry.kt  # Aggregated history entry model
│       │   │   ├── NotificationHistoryStorage.kt# Unified history (aggregated JSON)
│       │   │   ├── BlockedNotificationHistoryStorage.kt # Legacy blocked history (migration only)
│       │   │   ├── StatsStorage.kt              # Blocked counts + per-day notification counts
│       │   │   ├── AppInfoStorage.kt            # App icon/name cache (SQLite)
│       │   │   ├── UnmonitoredAppsStorage.kt    # Unmonitored apps (SharedPreferences)
│       │   │   ├── NotificationActionRepository.kt # In-memory PendingIntent cache
│       │   │   ├── TtsSpeaker.kt                # TTS speech + debounce
│       │   │   ├── RemoteViewsTextExtractor.kt  # Reflection text extraction (off by default)
│       │   │   ├── NotificationColorEngine.kt   # App-icon color extraction + hash fallback
│       │   │   ├── CrashLogManager.kt           # Crash log collection/view
│       │   │   ├── ExternalLinks.kt             # External links
│       │   │   ├── health/HealthCheckWorker.kt  # Periodic listener health check
│       │   │   ├── setup/OemAutostart.kt        # Per-OEM autostart settings
│       │   │   ├── setup/SetupState.kt          # Setup wizard step state
│       │   │   └── ui/
│       │   │       ├── components/              # Reusable Compose components
│       │   │       ├── screens/                 # Six main screens
│       │   │       └── theme/                   # Theme + semantic tokens
│       │   └── res/                             # Resources, localized strings
│       ├── test/                                # JVM unit tests
│       └── androidTest/                         # Instrumented tests
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml                    # Version catalog
├── gradle/wrapper/…
├── docs/                                       # ARCHITECTURE / API_REFERENCE / DEVELOPER_GUIDE
├── README.md / README.zh-CN.md
├── CHANGELOG.md / RELEASE_NOTES.md / VERSION_HISTORY*.md
└── LICENSE
```

> Note: this repository has **no** `assets/` or `fastlane/` directory, and no prebuilt-rules feature. The classes mentioned by older docs — `PrebuiltRulesRepository`, `StackedNotificationManager`, `BlockedScreen`, `Dialogs.kt`, `AboutDialog.kt` — no longer exist.

---

## 3. Build System & Configuration

### Root build (`build.gradle.kts`)

Declares plugin aliases without applying them:

- `com.android.application` (AGP 8.13.2)
- `org.jetbrains.kotlin.android` (Kotlin 2.0.21)
- `org.jetbrains.kotlin.plugin.compose`

### App build (`app/build.gradle.kts`)

**Applied plugins:** `android.application`, `kotlin.android`, `kotlin.compose`, `kotlin-parcelize`.

**Signing (release):** `signingConfigs.create("notix")` reads `KEYSTORE_NOTIX_FILE / KEYSTORE_NOTIX_PASSWORD / KEYSTORE_NOTIX_ALIAS / KEYSTORE_NOTIX_KEYPASSWORD` from `local.properties` or environment variables, and configures the keystore only when the file exists. **The keystore and its passwords are never committed** (see `.gitignore`).

**Build types:**
- `debug` — default debug configuration
- `release` — `isMinifyEnabled = true` (R8, `proguard-android-optimize.txt` + `proguard-rules.pro`), signed with the `notix` config

**Other:** `compileOptions`/`kotlinOptions` target Java 11; `buildFeatures { compose = true; buildConfig = true }`; `testOptions.unitTests.isReturnDefaultValues = true` (JVM unit tests stub `android.util.Log`).

### ProGuard (`proguard-rules.pro`)

Minimal rules. Data classes rely on `@Keep` annotations for R8 retention (required for Gson reflection-based deserialization).

---

## 4. Runtime Entry Points

### 4.1 `NotixApp` (`NotixApp.kt`)

`Application` subclass; `onCreate()` runs, in order:
1. `CrashLogManager.install(this)` — installs global crash logging (writes `crash_logs.txt`).
2. `createHealthChannel()` — creates the health-check notification channel (`IMPORTANCE_HIGH`).
3. `HealthCheckWorker.enqueue(this)` — enqueues the periodic health check.

### 4.2 `MainActivity` (`MainActivity.kt`)

`ComponentActivity` — the app's UI root and state coordinator:

- **Initialization:** `enableEdgeToEdge()`; loads local data (rules, history, unmonitored apps, settings, stats); runs the **one-time migration** of legacy blocked history on launch; registers the `ACTION_HISTORY_UPDATED` broadcast receiver.
- **State:** `setupDone` (shows the wizard until complete), `selectedTab` (History / Rules / Settings), `rules`, `notifications` (aggregated history), `unmonitoredApps`, etc. — all driven by `mutableStateOf`.
- **Navigation:** **no Jetpack Navigation**; pure state switching: setup wizard (until done) → main screen (three tabs); Settings, Rule Wizard, Storage Usage, Crash Log are full-screen overlays (boolean / nullable-state controlled).
- **Broadcast refresh:** on `ACTION_HISTORY_UPDATED`, calls `scheduleHistoryRefresh` (400 ms debounce, reads disk on an IO thread) to refresh history/rules/stats.
- Exposes `triggerNotificationAction(...)` (trigger a cached PendingIntent from the history details dialog) and `restoreNotificationToShade(...)` to communicate with the service.

### 4.3 `NotificationBlockerService` (`NotificationBlockerService.kt`)

`NotificationListenerService` subclass — the core engine (detailed in §8). Highlights:

- **Foreground keep-alive:** `onListenerConnected()` calls `startKeepAliveForeground()` (`specialUse` foreground service) plus a 1-hour heartbeat that refreshes `last_listener_connected_ms`.
- **Reconnect:** `onListenerDisconnected()` calls `requestRebind()` unless paused.
- **`onStartCommand` actions:**
  - `ACTION_APPLY_RULE` — re-apply a rule by id to currently active notifications;
  - `ACTION_RESCAN_ALL` — full rescan of active notifications;
  - `ACTION_RESTORE_SNOOZED` — restore notifications frozen by rules.
- **Self-package guard:** skips notifications from `BuildConfig.APPLICATION_ID` and the `RULE_REPOST_CHANNEL_ID` channel to avoid recursive processing (the channel is still defined, but the repost function `repostNotification()` is dead code left from the old SILENT model — see §16).
- **Teardown:** `onDestroy()` cancels in-flight Action Flows and shuts down thread pools after a 5-second delay.

---

## 5. Data Models

### 5.1 `BlockerRule` (`BlockerRule.kt`)

Since v7.37 the rule model is restructured as «multiple source apps + keyword condition + phone-state condition + **ordered action chain**»:

```kotlin
data class BlockerRule(
    val id: String = "",                  // Stable id (RuleIds-normalized; never re-keyed)
    val description: String? = null,      // Optional rule name
    val isEnabled: Boolean = true,
    val hitCount: Int = 0,                // Match count
    val sourcePackages: List<SourceApp> = emptyList(),  // Multiple source apps
    val condition: RuleCondition = RuleCondition(),     // Keyword matching
    val extraCondition: ExtraCondition = ExtraCondition(), // Phone-state condition
    val actions: List<ActionSpec> = emptyList(),  // Action chain (order = execution order)
    val createdAt: Long = 0L,
) {
    val isValid: Boolean   // At least one source app + non-empty, fully valid action chain
}
```

> The old model (single `action`/`actionParams` fields) is no longer compatible: rules missing `actions` are rejected by `isValid=false` and filtered out on load.

### 5.2 Source apps & keyword condition

```kotlin
data class SourceApp(val packageName: String, val appName: String? = null)

data class RuleCondition(
    val mode: MatchMode = MatchMode.CONTAINS_ANY,
    val includeKeywords: List<String> = emptyList(),
    val excludeKeywords: List<String> = emptyList(),  // MIXED only
)
```

`MatchMode` (six states):

| Enum | Semantics |
|---|---|
| `CONTAINS_ANY` | Contains any (any A keyword hits) |
| `CONTAINS_ALL` | Contains all (every A keyword hits) |
| `NOT_CONTAINS_ANY` | Contains none (no A keyword hits) |
| `NOT_CONTAINS_ALL` | Not all (at least one A keyword misses) |
| `MIXED` | Contains A and not B |
| `ADVANCED` | Advanced matching (UI-only; not usable yet; always false) |

### 5.3 Phone-state extra condition

```kotlin
data class ExtraCondition(
    val screenState: ScreenState = ScreenState.ANY,      // ANY / SCREEN_ON / SCREEN_OFF
    val chargingState: ChargingState = ChargingState.ANY,// ANY / WIRED / WIRELESS / BATTERY
    val dndState: DndState = DndState.ANY,               // ANY / ON / OFF
    val bluetoothState: BluetoothState = BluetoothState.ANY, // ANY / CONNECTED / DISCONNECTED
    val bluetoothDeviceNames: List<String> = emptyList(),    // Named devices; any match
    val time: TimeCondition = TimeCondition(),
)

data class TimeCondition(
    val enabled: Boolean = false,
    val startHour: Int = 0, val startMinute: Int = 0,
    val endHour: Int = 23, val endMinute: Int = 59,
    val weekdays: List<Int> = emptyList(),  // 1=Mon … 7=Sun; empty = every day
)
```

### 5.4 Action chain: `ActionSpec` + `RuleAction`

```kotlin
data class ActionSpec(
    val type: RuleAction,
    val params: JsonObject? = null,   // Native Gson object, avoids sealed-class deserialization issues
)
```

`RuleAction` — eight actions, executed **strictly serially** in list order:

| Action | Params | Execution status |
|---|---|---|
| `DISMISS` | `DismissParams(includeOngoing, snoozeDurationMs)` | ✅ Clearable: `cancel`; ongoing + `includeOngoing=true`: freeze via `snoozeNotification` |
| `CLICK_BUTTON` | `ClickButtonParams(buttonLabel)` | ✅ Exact/contains button-label match → `actionIntent.send()` |
| `OPEN_NOTIFICATION` | none | ✅ `contentIntent.send()` |
| `COPY` | `CopyParams(mode: TITLE/TEXT/TITLE_AND_TEXT)` | ✅ Writes to system clipboard |
| `TTS` | `TtsParams(template)` | ✅ Template placeholders `{app}/{title}/{text}` + speech |
| `STRONG_REMIND` | `StrongRemindParams` | ⚠️ **Execution TODO** (added v8.10; heads-up/ring/vibrate not wired) |
| `DELAY` | `DelayParams(durationMs)` | ✅ `Handler.postDelayed` then continue |
| `POSTPONE` | `PostponeParams(delayMs)` | ⚠️ **Execution TODO** (re-deliver later not implemented) |

Helper types: `CopyMode`, `TtsParams`, `CopyParams`, `DelayParams`, `DismissParams`, `StrongRemindParams`, `PostponeParams`, `ClickButtonParams`. `ActionSpec.isValid` validates CLICK_BUTTON (non-blank label), DELAY (`durationMs > 0` with safe Long-range parsing) and POSTPONE (`delayMs > 0`). `SnoozeDurations` provides freeze durations (1 hour / 1 day / 7 days / 30 days / 1 year; default 7 days).

### 5.5 `SimpleNotification` (`SimpleNotification.kt`)

Notification snapshot model: `appLabel / packageName / title / text / timestamp / wasOngoing / id` (`@Keep @Parcelize`).

### 5.6 `NotificationHistoryEntry` (`NotificationHistoryEntry.kt`)

Aggregated history entry (consecutive notifications with the same pkg + title are merged):

```kotlin
@Keep data class NotificationHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String? = null,
    val appLabel: String? = null,
    val title: String? = null,
    val count: Int = 1,                 // Real count (no 9+ cap)
    val firstTimestamp: Long = 0L,
    val lastTimestamp: Long = 0L,
    val blocked: Boolean = false,       // Blocked groups are not merged with normal ones
    val changes: List<SimpleNotification> = emptyList(),  // Reverse-chronological; [0] = newest
)
```

---

## 6. Rule Decision Engine (RuleMatcher)

`RuleMatcher` (`RuleMatcher.kt`) is a **pure-JVM singleton `object`** with no Android dependency. Decision model:

```kotlin
data class EnvironmentSnapshot(
    val screenOn: Boolean = true,
    val charging: ChargingState = ChargingState.ANY,
    val dndOn: Boolean = false,
    val bluetoothDeviceNames: List<String> = emptyList(),  // permission-free A2DP/SCO device names
    val now: Long = System.currentTimeMillis(),
)

sealed interface RuleDecision {
    data object Pass : RuleDecision          // No rule matched → let it through
    data class Apply(val rule: BlockerRule) : RuleDecision  // Matched → run actions
}
```

Core methods:

- **`evaluate(rule, packageName, title, text, env): Boolean`** — full single-rule evaluation: `isValid && isEnabled` → source-app filter → `matchesCondition` → `matchesExtra`.
- **`matchesCondition(condition, title, text): Boolean`** — case-insensitive keyword matching against title or text per `MatchMode`; no condition (A and B both empty) = always true.
- **`matchesExtra(extra, env): Boolean`** — screen / charging / DND / Bluetooth / named-device / time-window (cross-midnight) checks; uses `orEmpty()` to tolerate null fields from old JSON.
- **`isTimeInRange(hour, minute, startH, startM, endH, endM): Boolean`** — supports overnight ranges (e.g. 22:00–06:00).
- **`planNotificationDecision(rules, packageName, title, text, env): RuleDecision`** — **entry point**: scans enabled rules in order (source app → keyword → extra condition); **the first match returns `Apply`**, otherwise `Pass`.

> The decision model is «first-match wins». There is no allowlist/denylist/stack precedence matrix (that model was removed).

---

## 7. Action Flow Executor (ActionFlowExecutor)

`ActionFlowExecutor` (`ActionFlowExecutor.kt`) runs `rule.actions` **strictly serially** in list order, owning the flow lifecycle, fail-continue semantics, async completion signals, and at-most-once advancement.

### 7.1 Key design

- **Each `execute()` creates its own `FlowExecution`**: `currentIndex / failedActions / cancelled` are per-flow private state, so concurrent flows for different notifications never interfere; no global `currentIndex`.
- **Sync actions** (DISMISS/CLICK_BUTTON/OPEN_NOTIFICATION/COPY) advance immediately after running; **exceptions are caught → recorded as FAILED → the flow continues**, never aborts.
- **Async actions** (TTS/DELAY) advance only after `onDone/onError` or after `postDelayed` expires.
- **At-most-once**: duplicate callbacks (onDone+onError / onDone+onDone) are ignored.
- **Host-alive check**: an injected `!isDestroyed` callback means pending DELAY/TTS callbacks stop advancing after the host is destroyed.
- **Thread model**: the engine owns no worker pool; callers place `execute()` on their own thread (the service uses its own executor). `FlowExecution` uses `synchronized(this)` so TTS/DELAY callback threads and the synchronous execution thread are mutually exclusive.

### 7.2 Runtime context & interfaces

```kotlin
class ActionContext(
    ruleId, packageName, appName, title, text,
    notificationKey, postTime,
    includeOngoing, snoozeDurationMs,
    sbn, notificationActions, contentIntent,   // runtime objects, memory-only, never in JSON
)

interface ActionFlowHost {            // implemented by NotificationBlockerService
    fun cancelNotificationCompat(key: String)
    fun snoozeNotificationCompat(key: String, ruleId: String?, durationMs: Long)
    fun copyToClipboard(text: String)
    fun buildTtsText(template, app, title, text, postTime): String
    fun speakTts(ctx, text, onDone: (Boolean) -> Unit)
}

interface SyncActionRunner  { fun dismiss/clickButton/openNotification/copy(...) }
interface AsyncActionRunner { fun runTts(...); fun runDelay(delayMs, onComplete) }
```

Production implementations: `RealSyncActionRunner` (DISMISS dispatch between clearable/ongoing, button matching, `contentIntent.send()`, clipboard) and `RealAsyncRunner` (main-thread `Handler.postDelayed`, TTS bridged to the host). JVM unit tests inject fakes.

### 7.3 Flow final states

`FlowStatus`: `SUCCESS` / `PARTIAL_FAILURE` (failures but completed) / `EMPTY` (empty chain) / `CANCELLED` (service destroyed or externally cancelled). Results carry `failedActions: List<ActionFailure(index, type, reason)>` and `executedCount`.

---

## 8. Notification Processing Pipeline (NotificationBlockerService)

`onNotificationPosted(sbn)` main path (the whole path sits in a top-level try-catch; exceptions are written to the crash log, never crash):

```
Android posts notification (StatusBarNotification)
  │
  ├─ 1. Paused check: listener_paused == true → return
  ├─ 2. Self-package guard: packageName == BuildConfig.APPLICATION_ID
  │       or channelId == RULE_REPOST_CHANNEL_ID → skip (anti-recursion)
  ├─ 3. Extract title/text (extras: android.title / android.text)
  │       ├─ both blank AND switch extract_remoteviews_text ON (default OFF)
  │       │    → RemoteViewsTextExtractor reflection extraction
  │       └─ still blank → ignore the notification
  ├─ 4. Resolve app name + cache icon/name via AppInfoStorage
  ├─ 5. Collect EnvironmentSnapshot (screen/charging/DND/Bluetooth/time)
  ├─ 6. RuleStorage.getRules() + RuleMatcher.planNotificationDecision(...)
  │       ├─ Pass   → record history (skipped for unmonitored apps)
  │       └─ Apply  → executeActionFlow(rule)
  │
  ├─ 7. Apply branch: Action-Flow 3 s debounce (per sbn.key)
  │       → actionExecutor.execute(rule.actions, ctx)
  │       (single-threaded serial; STRONG_REMIND/POSTPONE logged and skipped)
  │
  ├─ 8. History write (dedicated single-thread historyExecutor):
  │       ├─ rule hit → hitCount+1 → saveNotification(blocked=true) + blocked count
  │       └─ no hit   → unless unmonitored → saveNotification(blocked=false)
  │
  ├─ 9. Broadcast ACTION_HISTORY_UPDATED (refresh UI)
  └─ 10. Clean up expired debounce entries
```

**Constants:** `ACTION_HISTORY_UPDATED = "com.enlpot.notix.HISTORY_UPDATED"`, `RULE_REPOST_CHANNEL_ID = "rule_repost"`, plus various debounce/heartbeat constants.

**Dedup / debounce layers (service-side):**
- `recentlyBlocked` (sbn.key, 3 s window) — suppresses repeated processing of the same key in a short window;
- Action-Flow 3 s debounce — one action-chain execution per sbn.key within 3 s;
- TTS 5 s debounce (`TtsSpeaker`) — one speech per notification within 5 s;
- storage-level same-entry dedup + head aggregation.

---

## 9. Storage Layer

| Storage class | Backing | Content / key points |
|---|---|---|
| `RuleStorage` | `rules.json` (AtomicFile) | Rule persistence; process-level cache; **all mutations id-keyed under a global lock** so the listener's hit-count bumps and the UI's edits never overwrite each other; filters invalid rules on load and backs up `rules.json.bak`; a corrupt file is preserved as `rules.json.corrupt.<ts>` |
| `NotificationHistoryStorage` | `notification_history.json` | Unified history, **aggregated model** (`NotificationHistoryEntry`); process-level cache; auto-migration from the old format; pruned by `historyDays` (default 5); `deleteNotification` / `deleteNotificationsFromPackage` / `updateAppLabelForPackage` / `clearHistory` |
| `BlockedNotificationHistoryStorage` | `blocked_notification_history.json` | **Migration-only**: merged into unified history at launch then cleared; the service no longer writes it |
| `AppInfoStorage` | SQLite `app_info.db` | package → name + PNG icon BLOB (`CONFLICT_REPLACE`) |
| `UnmonitoredAppsStorage` | SharedPreferences `unmonitored_apps_prefs` | set of unmonitored packages (Gson-serialized Set) |
| `StatsStorage` | SharedPreferences `stats` | total blocked count `blocked_count` + per-day notification counts (400-day cap) |
| `NotificationActionRepository` | in-memory `ConcurrentHashMap` | PendingIntent cache (lost on process death) |
| `CrashLogManager` | `crash_logs.txt` | crash log (enabled by default, app-private directory) |

> Consistency: JSON storages do **whole-file atomic replacement** (AtomicFile) combined with single-writer executors and locks — sufficient at this data scale (typically <100 rules, <a few thousand history entries).

---

## 10. Supporting Subsystems

- **`TtsSpeaker`** — TTS init/speak/stop with a built-in 5 s debounce; template building and cleaning live in the service's `buildTtsText`.
- **`RemoteViewsTextExtractor`** — reflection-based text extraction from `RemoteViews` for notifications with no text; **off by default** (privacy).
- **`NotificationColorEngine`** — extracts a dominant color from the app icon for UI accent colors; falls back to a package-name hash color when unresolvable (v8.15.2).
- **`HealthCheckWorker`** (WorkManager, 6-hour period) — checks whether `last_listener_connected_ms` is older than 24 h while the listener is authorized; if so, posts a high-importance notification to guide the user to fix it (24 h throttle).
- **`setup/OemAutostart`** — opens the matching autostart settings page per vendor (Xiaomi/Huawei/OPPO/OnePlus/vivo/Samsung and ~10 more).
- **`setup/SetupState`** — setup wizard step state (Welcome → Listener → PostNotif → Battery → OEM → Done).
- **`RuleIds`** — rule-id normalization (re-keys missing/duplicated ids; normal updates must never re-key, to avoid orphaning notification channels).
- **`RuleMutations`** — pure rule mutation helpers (enable/disable, rename, reset hit counts, …) that never re-key ids.
- **`RuleImport`** — v4 envelope import/export (`@Keep` fields, sanitization dropping invalid rules with `droppedCount`).
- **`RuleWizardSupport`** — wizard pure helpers: action-chain summaries, known-app merge (history + rules + AppInfo; **never reads the installed-app list**).

---

## 11. UI Layer

All Compose + Material 3, no XML layouts. Bottom three tabs: **History / Rules / Settings** (icon-only with long-press tooltips; bottom bar on portrait, left-list/right-content on landscape).

### Screens (`ui/screens/`)

| Screen | Responsibility |
|---|---|
| `HistoryScreen` | History tab: aggregated cards (By time / By app / Handled by rule sub-tabs), chart panel, search, fold segments, detail dialog, create-rule / trigger-action from a notification |
| `RulesScreen` | Rules tab: RuleCard list (action-chain summary, hit count, toggle, rescan, reset hit count, long-press delete) |
| `RuleWizardScreen` | Rule wizard: source apps (multi-select) → match mode/keywords → phone-state condition → **ordered action chain** (drag-reorder, per-action params) |
| `SettingsScreen` | Settings: retention days, import/export, permission management (listener/notifications/battery), crash log, storage usage, restore ongoing notifications, version, … |
| `SetupWizardScreen` | First-run onboarding wizard (step-by-step permissions) |
| `StorageUsageScreen` | Storage usage stats and cleanup |

### Components (`ui/components/`)

`NotixDialog` / `NotixConfirmDialog` (unified dialog system), `NotificationCard` / `RuleCard` / `SettingRow` / `SectionHeader` / `EmptyState` / `SearchField` / `Chip` / `Buttons` / `RealAppIcon` / `HistoryNotificationDetailsDialog` / `NotificationDetailDialog` / `CrashLogDialog` / `DesignSystemPreview`, …

### Theme (`ui/theme/`)

Material 3 custom Light/Dark schemes + a **semantic token system**: `NotixTheme` provides `notixColors / notixType / notixSpacing / notixLayout / notixElevation` via `CompositionLocal`; pages read them through `MaterialTheme.notix*` instead of branching on Light/Dark directly. Dynamic color (Material You) is off by default (`dynamicColor = false`).

---

## 12. Concurrency & Consistency Model

- **Single-writer executors**: history writes and action execution each use a dedicated single-threaded executor inside the service, avoiding concurrent file-write races.
- **Id-keyed rule lock**: `RuleStorage` serializes every rule mutation under a lock, so the listener's hit-count bumps and the UI's edits never overwrite each other (`RuleMutations` never re-key ids).
- **Multi-layer debounce**: 3 s (service) / 5 s (TTS) / 400 ms (UI broadcast) — prevents duplicate processing, duplicate speech, and duplicate disk reads.
- **Self-package guard**: prevents recursive handling of notifications the app re-posts itself.
- **Crash safety net**: top-level try-catch around notification handling + crash log; one bad notification never kills the process.

---

## 13. Privacy & Offline Design

- **Zero network permission**: no `INTERNET` in the manifest; no HTTP requests; all data stays on-device.
- **No app enumeration**: no `<queries>` block, no `QUERY_ALL_PACKAGES`; app info is resolved lazily by package when a notification actually arrives (`PackageManager` on that specific package).
- **RemoteViews reflection extraction is off by default**: avoids needless reads into notification internals.
- **Rule source apps are only taken from apps seen in history** (`RuleWizardSupport.mergeKnownApps` never reads the installed list).
- Crash logs and notification history live in app-private storage; export requires an explicit user action (SAF).

---

## 14. Testing

### Unit tests (`app/src/test/`, plain JVM, no Robolectric)

Rely on `testOptions.unitTests.isReturnDefaultValues = true`; Android side-effects are funneled through interfaces (`ActionFlowHost` / runners) with fakes injected in tests.

| Test class | Coverage |
|---|---|
| `ActionFlowExecutorTest` | serial execution, fail-continue, at-most-once, cancellation, host destroyed |
| `ActionFlowModelTest` | `ActionSpec` / param models / `isValid` |
| `ActionFlowEditorTest` / `ActionFlowCopyBehaviorTest` / `DismissSpecTest` | COPY modes, DISMISS dispatch, etc. |
| `RuleImportExportRoundTripTest` | v4 import/export round trip |
| `RuleWizardSupportTest` | wizard summaries / merge pure logic |
| `ExampleUnitTest` | smoke test |

### Instrumented tests (`app/src/androidTest/`)

`BaseActionFlowTest` + `TestNotificationFactory` / `TestRuleFactory` / `TestPendingIntentReceiver` back `ActionFlowBasicTest` / `ActionFlowClickFallbackTest` / `ActionFlowDebounceTest` / `ActionFlowDestroyTest` / `ActionFlowIntegrationTest` / `ActionFlowTtsConcurrencyTest`, plus Compose UI flow tests `RulesScreenFlowTest` / `RulesScreenFlow4BTest` / `RulesScreenFlowSaveValidationTest` / `RulesScreenFlowWarningTest`, etc.

---

## 15. Android Manifest & Permissions

**Permissions** (`AndroidManifest.xml`):

```xml
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

No network, storage, camera or location permissions.

**Components:**
- `MainActivity` — `MAIN`/`LAUNCHER`, exported.
- `NotificationBlockerService` — `BIND_NOTIFICATION_LISTENER_SERVICE` permission, `NotificationListenerService` intent filter, `foregroundServiceType="specialUse"` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`.
- `FileProvider` — for sharing files such as the crash log (`file_paths.xml`).

**Backup:** `fullBackupContent="@xml/backup_rules"` and `dataExtractionRules="@xml/data_extraction_rules"` control the auto-backup scope.

---

## 16. Known Placeholders & Limitations

- **`STRONG_REMIND` / `POSTPONE` are execution placeholders only**: they can be saved and will match, but `ActionFlowExecutor` logs and skips them — no runtime side effects (code notes «v8.11+ wiring»).
- **`repostNotification()` and `RULE_REPOST_CHANNEL_ID` are leftovers of the old SILENT model**: the function is defined in the service (~line 645) but has no call sites — dead code; the channel is still used for self-package/repost guards.
- **`ADVANCED` match mode is UI-only**: `matchesCondition` always returns false for it.
- **`RemoteViewsTextExtractor` is off by default**, an experimental capability.
- **`BlockedNotificationHistoryStorage` is migration-only**: merged into unified history at launch and cleared; the service no longer writes it.
- The repository has no prebuilt rules and no fastlane metadata; `.github` contains only the single CI workflow `release.yml` (all credentials via GitHub Secrets).
