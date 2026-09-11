# Notix - Developer Guide

A practical guide for developers working on the Notix codebase, matching **v8.15.2**.

## Table of Contents

- [Getting Started](#getting-started)
- [Build & Run](#build--run)
- [Project Layout](#project-layout)
- [How to Add a New Feature](#how-to-add-a-new-feature)
- [Understanding the Rule System](#understanding-the-rule-system)
- [Understanding the Action Flow](#understanding-the-action-flow)
- [How to Add a New Storage Mechanism](#how-to-add-a-new-storage-mechanism)
- [How to Add a New Screen](#how-to-add-a-new-screen)
- [How to Add a New Dialog](#how-to-add-a-new-dialog)
- [Key Design Decisions](#key-design-decisions)
- [Common Patterns](#common-patterns)
- [Testing](#testing)
- [Release Process](#release-process)

---

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- Android SDK with API level 36
- An Android device or emulator running API 24+

### Project Setup

1. Clone the repository.
2. Open in Android Studio (Gradle syncs automatically).
3. Build: `./gradlew assembleDebug`
4. Run on device: `./gradlew installDebug`

### Signing (release only)

The release signing config reads `KEYSTORE_NOTIX_FILE / KEYSTORE_NOTIX_PASSWORD / KEYSTORE_NOTIX_ALIAS / KEYSTORE_NOTIX_KEYPASSWORD` from `local.properties` or environment variables. Debug builds need no keystore. **Never commit the keystore or its passwords** (`.gitignore` already excludes `*.jks`, `*.keystore`, `signing.properties`, `keystore.properties`, `local.properties`).

### Important: Notification Listener Permission

Notix is a `NotificationListenerService`; this permission can only be granted via system settings. The first-run setup wizard guides the user through it. For development: **Settings > Apps > Special app access > Notification access > Notix**.

---

## Build & Run

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (R8 minification; needs signing config)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Clean build artifacts
./gradlew clean
```

Unit tests run on plain JVM (no Robolectric); `testOptions.unitTests.isReturnDefaultValues = true` stubs `android.util.Log`.

---

## Project Layout

```
app/src/main/java/com/enlpot/notix/
├── NotixApp.kt                  # Application entry
├── MainActivity.kt              # UI root & state
├── NotificationBlockerService.kt# Listener engine
├── BlockerRule.kt               # Rule + action models
├── RuleMatcher.kt               # Pure decision engine
├── ActionFlowExecutor.kt        # Action-chain executor + runners
├── RuleStorage.kt / RuleIds.kt / RuleMutations.kt / RuleImport.kt / RuleWizardSupport.kt
├── NotificationHistoryStorage.kt / BlockedNotificationHistoryStorage.kt / NotificationHistoryEntry.kt / SimpleNotification.kt
├── StatsStorage.kt / AppInfoStorage.kt / UnmonitoredAppsStorage.kt / NotificationActionRepository.kt
├── TtsSpeaker.kt / RemoteViewsTextExtractor.kt / NotificationColorEngine.kt / CrashLogManager.kt / ExternalLinks.kt
├── health/HealthCheckWorker.kt
├── setup/OemAutostart.kt / SetupState.kt
└── ui/
    ├── components/              # Reusable composables
    ├── screens/                 # History / Rules / RuleWizard / Settings / SetupWizard / StorageUsage
    └── theme/                   # Theme + semantic tokens
```

---

## How to Add a New Feature

### Example: add a new action to the action chain

The action chain is the core extensibility point. Adding a new action touches these files:

1. **`BlockerRule.kt`** — add the value to `enum RuleAction`; add a params data class (e.g. `MyActionParams`) and any validation in `ActionSpec.isValid`.
2. **`ActionFlowExecutor.kt`** — add a `when` branch in `advance()` that calls the runner and then `completeAction(...)`; if it is async, wait for its completion callback first (see TTS/DELAY). Wire any Android side-effect through `ActionFlowHost` and `SyncActionRunner` / `AsyncActionRunner`.
3. **`RuleWizardSupport.kt`** — add `hasActionParams(type)`, `defaultParamsFor(type)`, a `*Spec(...)` builder, and an `actionFlowSummary` case for the new action.
4. **`ui/screens/RuleWizardScreen.kt`** — add label/icon/description + `ActionParamEditor` UI for the new action.
5. **Tests** — engine logic is covered in `ActionFlowExecutorTest`; behavior in a new test class.

### File modification order (general)

1. Data models (`BlockerRule.kt`, `SimpleNotification.kt`)
2. Pure logic (`RuleMatcher.kt`, `RuleMutations.kt`, `RuleWizardSupport.kt`)
3. Storage (if needed)
4. Engine (`NotificationBlockerService.kt`, `ActionFlowExecutor.kt`)
5. UI components (`ui/components/`)
6. UI screens (`ui/screens/`)
7. Activity wiring (`MainActivity.kt`)
8. Tests

> Keep Android side-effects behind the interfaces (`ActionFlowHost`, `*ActionRunner`) so the logic stays JVM-testable.

---

## Understanding the Rule System

### Rule anatomy

```
sourcePackages (≥1 app)
  → condition:  keyword matching (MatchMode + include/exclude keywords)
  → extraCondition: phone-state (screen / charging / DND / Bluetooth / time window)
  → actions: ordered action chain
```

### Decision flow (`RuleMatcher`)

1. Rule must be `isValid && isEnabled`.
2. Source-app filter: the notification's package must be in `sourcePackages`.
3. Keyword match (`matchesCondition`): per `MatchMode`, case-insensitive against title or text. **Empty condition = always matches.**
4. Extra conditions (`matchesExtra`): all configured phone-state checks must pass; the time window supports overnight ranges; empty weekdays = every day.
5. **First rule that passes all checks wins** (`planNotificationDecision` → `RuleDecision.Apply`); otherwise `Pass`.

### Hit counting

Each matched notification bumps `hitCount` on the matched rule via `RuleStorage.incrementHitCounts`. Counts are visible on rule cards; reset from the Rules screen.

### Rule identity & ids

- Every rule has a stable `id` (`RuleIds`); ids are never re-keyed on update (`RuleMutations`), because a rule's id owns a notification channel.
- Imported/legacy rules are normalized and sanitized on load; invalid rules (no source app or empty action chain) are filtered out.

---

## Understanding the Action Flow

- `rule.actions` is an ordered `List<ActionSpec>`; `actions[0]` runs first.
- Execution is **strictly serial**: each action completes (success or failure) before the next starts; failures are recorded (`ActionFailure`) and the flow continues.
- Async actions (TTS, DELAY) advance only after their completion callback; duplicate callbacks are ignored (at-most-once).
- The flow is cancelled if the service is destroyed (`hostAlive`).
- **Placeholders**: `STRONG_REMIND` and `POSTPONE` are validated/saved but currently execute as no-ops (logged `skipped (execution TODO)`).

Adding an action: see [How to Add a New Feature](#how-to-add-a-new-feature).

---

## How to Add a New Storage Mechanism

The app uses several storage patterns; choose by need:

### JSON file (structured lists) — `RuleStorage` / `NotificationHistoryStorage`

Use `AtomicFile` + a process-level cache + a lock for read-modify-write safety:

```kotlin
class MyStorage(context: Context) {
    private val gson = Gson()
    private val file = File(context.filesDir, "my_data.json")
    private val atomicFile = AtomicFile(file)

    fun getData(): List<MyData> {
        if (!file.exists()) return emptyList()
        val type = object : TypeToken<List<MyData>>() {}.type
        return gson.fromJson(atomicFile.readFully().toString(Charsets.UTF_8), type) ?: emptyList()
    }

    fun saveData(data: List<MyData>) {
        val stream = atomicFile.startWrite()
        try {
            stream.write(gson.toJson(data).toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            atomicFile.failWrite(stream)
            throw e
        }
    }
}
```

> If the data can be written from multiple threads (e.g. the listener executor and the UI), add a shared lock and a cache, like `RuleStorage` does.

### SharedPreferences (key-value) — `StatsStorage` / `UnmonitoredAppsStorage`

Follow `StatsStorage` for primitives (with a lock around read-modify-write), or `UnmonitoredAppsStorage` for Gson-serialized collections (with a cache).

### SQLite (queryable / larger data) — `AppInfoStorage`

Follow `AppInfoStorage` / `AppInfoDatabaseHelper` (`SQLiteOpenHelper`).

### In-memory (transient) — `NotificationActionRepository`

Singleton `object` with a `ConcurrentHashMap`.

### Integration

After creating the storage class, instantiate it where needed (the service owns the write path; the activity/screens own the read path). Keep the **single-writer** principle: concurrent writers must share one lock.

---

## How to Add a New Screen

1. **Create the screen composable** in `ui/screens/`.
2. **Add navigation state** in `MainActivity`:
   ```kotlin
   private var showMyScreen by mutableStateOf(false)
   ```
3. **Wire it in the root composable** with a state check + `BackHandler`. The app uses boolean/state-based navigation (no Jetpack Navigation):
   ```kotlin
   if (showMyScreen) {
       BackHandler { showMyScreen = false }
       MyScreen(onClose = { showMyScreen = false }, ...)
   }
   ```

Read UI text from `strings.xml` (there are `values`, `values-zh-rCN`, `values-es`, `values-fr`, `values-ja`, `values-ko`, `values-pl`, `values-ru`); use the semantic tokens (`MaterialTheme.notix*`) instead of hardcoded colors/spacing.

---

## How to Add a New Dialog

1. **Create the dialog composable** in `ui/components/`, extending the unified system `NotixDialog` / `NotixConfirmDialog` where possible.
2. **Add trigger state** in the owning composable:
   ```kotlin
   var itemToShow by remember { mutableStateOf<MyData?>(null) }
   ```
3. **Show conditionally** (nullable state: non-null shows, null dismisses):
   ```kotlin
   itemToShow?.let { item -> MyDialog(data = item, onDismiss = { itemToShow = null }, ...) }
   ```

---

## Key Design Decisions

### No Architecture Framework
Direct state management with Compose `mutableStateOf`, state owned by `MainActivity` and passed down as parameters (no ViewModel/LiveData/StateFlow). Appropriate at this scale; refactor if complexity grows.

### Single Module
Everything lives in `:app`. Avoids build complexity for a project of this size.

### No Network
Zero network permissions; all data stays on-device. Don't add networking without a strong reason and a privacy review.

### Gson over Kotlin Serialization
Data classes use `@Keep` to survive R8 (Gson uses reflection). Keep `@Keep` on anything Gson touches.

### Full List Replacement + AtomicFile
JSON storages replace the whole file atomically; with single-writer executors and locks this is correct for the data sizes here.

### Boolean Navigation
State-based navigation instead of Jetpack Navigation — minimal dependencies, no deep-linking.

### Engine / Android side-effect seams
The decision engine (`RuleMatcher`) and the action engine (`ActionFlowExecutor`) depend only on interfaces (`ActionFlowHost`, `*ActionRunner`); the service provides the real implementations. Keep it that way so logic stays JVM-testable.

---

## Common Patterns

### Storage access in composables
Instantiate via `remember { Storage(context) }`; load async with `produceState` + `Dispatchers.IO` (e.g. app icons from `AppInfoStorage`).

### App icon colors
Use `NotificationColorEngine.getNotificationColors(context, packageName)` for accent colors (falls back to a hash color when the icon is unresolvable).

### Debouncing
- Service: 3 s Action-Flow debounce per `sbn.key`, 3 s `recentlyBlocked` window.
- TTS: 5 s debounce in `TtsSpeaker`.
- UI broadcast refresh: 400 ms debounce in `MainActivity`.

### Rule mutation safety
Never mutate the rule list outside `RuleStorage`'s id-keyed methods (`incrementHitCounts`, `updateRuleById`, `deleteRuleById`, …). They re-read under the lock and can't resurrect a deleted rule.

### Snackbar over Toast
Use the in-app `SnackbarHostState` (see `RuleWizardScreen`) instead of system toasts.

---

## Testing

### Unit tests (`app/src/test/`)

Plain JVM, no Robolectric. Run: `./gradlew testDebugUnitTest`.

- `ActionFlowExecutorTest` / `ActionFlowModelTest` / `ActionFlowCopyBehaviorTest` / `DismissSpecTest` / `ActionFlowEditorTest` — engine behavior with fakes.
- `RuleImportExportRoundTripTest` — v4 import/export round trip.
- `RuleWizardSupportTest` — wizard pure helpers.

Inject fakes for `ActionFlowHost` / `SyncActionRunner` / `AsyncActionRunner` and assert on `FlowResult` / `FlowExecution`. Example:

```kotlin
@Test
fun `dismiss failure continues the flow`() {
    val host = FakeHost(failDismiss = true)
    val exec = ActionFlowExecutor(RealSyncActionRunner(host), RealAsyncRunner(host))
    val flow = exec.execute(listOf(dismissSpec(), copySpec(CopyMode.TITLE)), ctx)
    assertEquals(FlowStatus.PARTIAL_FAILURE, flow.result?.status)
}
```

### Instrumented tests (`app/src/androidTest/`)

Run: `./gradlew connectedAndroidTest`. Use `TestNotificationFactory` / `TestRuleFactory` / `TestPendingIntentReceiver` and `BaseActionFlowTest` to drive real notifications; `RulesScreenFlow*` classes cover the Compose UI flows.

---

## Release Process

### Version bumping

In `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 132          // increment per release
    versionName = "8.16.0"     // human-readable
}
```

Also update `RELEASE_NOTES.md`, `VERSION_HISTORY.md` / `VERSION_HISTORY.zh-CN.md`, and `CHANGELOG.md` as appropriate.

### CI auto-release

`.github/workflows/release.yml` runs on push to `main` (and manual dispatch). It:
1. Reads `versionName`, skips if a `v<version>` GitHub release already exists.
2. Decodes the keystore from `secrets.NOTIX_KEYSTORE_BASE64` (plus `NOTIX_KEYSTORE_PASSWORD`, `KEYSTORE_NOTIX_KEYPASSWORD`, `KEYSTORE_NOTIX_ALIAS`) and builds `assembleRelease`.
3. Publishes the APK as a GitHub Release with `RELEASE_NOTES.md`.

> **Important:** keep the GitHub Secrets in sync with any local keystore. The release keystore is the signing identity of every published APK — store the keystore and passwords carefully, and never commit them.
