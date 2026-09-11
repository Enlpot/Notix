# Notix Developer Guide

> Matches **v8.57.0**. Older guide (v8.15.2): [archive/v8.15.2/DEVELOPER_GUIDE.md](archive/v8.15.2/DEVELOPER_GUIDE.md).

## Environment

- JDK 17, Android SDK `compileSdk 36`
- `./gradlew assembleDebug` / `:app:testDebugUnitTest` / `installDebug`
- Signing: `KEYSTORE_NOTIX_*` in `local.properties` or env — never commit secrets

## Before you edit

1. Read [AGENTS.md](../AGENTS.md).
2. **MainActivity**: landscape uses `screenContent()`; portrait `else` duplicates tab content — keep both in sync.
3. Grep callers for shared components.
4. Version bumps: `app/build.gradle.kts`, `RELEASE_NOTES.md`; push to `main` triggers `release.yml`.

## Code map

| Concern | File |
|---|---|
| Notification pipeline | `NotificationBlockerService.kt` |
| Matching | `RuleMatcher.kt` |
| Action chain | `ActionFlowExecutor.kt` |
| Rule persistence | `RuleStorage.kt` / `RuleMutations.kt` / `RuleIds.kt` |
| History (Room) | `data/repository/NotificationHistoryRepository.kt` |
| Card colors | `NotificationColorEngine.kt` |
| History fold UI | `ui/screens/HistoryScreen.kt` |

## Tests

Prefer JVM tests for pure logic (Action Flow has Fake host/runners). Use `androidTest` for Service/Compose. Word-cloud/plugin tests were removed in 8.57.

## Docs

Keep [ARCHITECTURE.md](ARCHITECTURE.md) aligned with source; park stale full copies under `docs/archive/`. User-facing changes go in [RELEASE_NOTES.md](../RELEASE_NOTES.md). Root `CHANGELOG.md` is no longer maintained (see `docs/archive/CHANGELOG.md`). Keep `VERSION_HISTORY*.md` in sync when cutting a release.
