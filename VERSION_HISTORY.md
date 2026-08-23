# Notix Version History

> This file is the local cumulative version history, ordered newest-first.
> At each release, that version's notes are posted **alone** as the GitHub Release body (see `RELEASE_NOTES.md`).
> This file only retains the full history and is not used for publishing.

---

## 8.7 (2026-08-23)

Changes since the previous released version 8.6.

### Improved

- Settings: Permission Management and Storage Usage are now shown as centered dialogs instead of full-screen sub-screens, keeping the main Settings screen visible behind them. The Permission Management entry shows a live status chip (green "Normal" / red "N issues").
- Storage Usage dialog now opens with a total-usage summary header.

### Changed

- Unified dialog dismissal: tap the scrim or the close (X) button to dismiss; taps inside the dialog no longer close it.

---

## 8.6 (2026-08-23)

Changes since the previous released version 8.5.

### Improved

- Settings permission consolidation refactor: the previously scattered "Notification Listener / Battery Optimization / Background Keep-alive" guides on the main Settings screen are merged into a single "Permission Management" sub-screen. The corresponding area on the main Settings screen becomes a single tappable entry row with a shield icon, a status badge (green "Normal" / red "N issues"), and a right arrow; tapping it opens the sub-screen showing the real-time status, description, and fix entry for the 4 system permissions.
- Added real-time monitoring for "Post Notifications (POST_NOTIFICATIONS)" and "Foreground Service Keep-alive", joining the existing "Notification Listener" and "Battery Optimization" to form 4 monitored items.

### Fixed

- Permission status cards were stuck at their initial values: the original implementation used a one-shot `remember {}` evaluation, so the status never refreshed after returning from system settings. Replaced with `refreshTick` + `DisposableEffect(Lifecycle.ON_RESUME)` that re-checks automatically on returning to foreground / resuming, synced across both the main entry and the sub-screen.

### Cleanup

- Removed 6 unused `settings_keepalive_*` strings (previously deprecated along with the old inline permission guides).

---

## 8.5 (2026-08-23)

Changes since the previous released version 8.4.

### Fixed

- Notifications not entering history in real time: the in-memory cache introduced in H3 was an instance-level field, so after the listener service wrote, only its own cache refreshed while the UI instance cache stayed at the stale first-read value; broadcast refresh hit the stale cache and history stopped updating. Promoted the cache to process-level (companion object) sharing, so any single disk write is immediately visible (major regression fix).
- Crash log dialog "Open log location" button text wrapped: Material3 button default padding was too large, the 6-character title in the half-width button didn't fit. Tightened padding and forced a single line.
- Crash log dialog "Open log location" tap did nothing: the original logic used FileProvider with `text/plain` to open the log file (not the directory), and with no available viewer it errored. Changed to prefer opening the log directory with the system file manager, falling back to opening the file, then finally showing the path if all fail.

---

## 8.4 (2026-08-23)

Changes since the previous released version 8.3.

### Improved

- History storage switched to atomic writes (AtomicFile) + background-thread immediate incremental flush + throttled history-update broadcasts, eliminating main-thread parse jank / ANR risk on large history files.
- Listener service environment snapshot cached for 10s, avoiding repeated battery-broadcast registration and Bluetooth audio-device enumeration on high-frequency notification arrival, reducing listener-thread load.

### Fixed

- History collapsed-section expand state isolated by section-level id, fixing the issue where multiple collapsed sections of the same package were expanded/collapsed together (#38).
- Collapse hint count error: hidden count now correctly subtracts the always-shown first item, e.g. 4 collapsed items show "Collapse 3" (#39).
- Setup wizard forced to exit mid-progress due to rotation / configuration change: on rebuild, respect the saved wizard-visibility state first (M1).
- Action Flow host (listener service) destroyed but suspended callbacks continued pushing side effects: injected host-alive check, silently terminate after destruction (M2).
- Rule import hint inaccurate: added explicit "No new rules (all already exist)" hint to avoid misleading into "Imported 0" (L1).
- Concurrent read/write data loss: StatsStorage counter increment and UnmonitoredAppsStorage add/remove changed to locked read-modify-write, fixing lost counts on high-frequency hits / concurrent add-remove overwriting each other (L2).

---

## 8.3 (2026-08-23)

Changes since the previous released version 8.2.

### Improved

- Notification detail dialog widened to 90% of the true screen width (disabled the platform default narrow window, previously compressed below 80%).
- Dialog interaction fix: tap scrim to dismiss, semi-transparent scrim restored, taps inside dialog no longer mis-close it.
- Aggregation window (change details) synced to 90% width, tap-scrim-to-dismiss, visually consistent with the detail dialog.
- Full-app dark-mode colors standardized to Material Design 3 standard dark: base surface changed from pure black to `#1B1B1F`, primary `#92CCFF`, text/dividers follow the MD3 neutral palette, contrast reaching WCAG AAA.
- Strictly preserved the existing app-derived colors of app-group cards, rule-group cards, and rule cards — no color changes made.

### Fixed

- Notification detail dialog could not be dismissed by tapping outside (caused by native scrim failing after disabling the platform default window; manual scrim added).
- Missing `Shape.kt` (`NotixCorner` corner tokens) in the repo caused a reference error that failed the release build.
- Gradle daemon JVM CompressedOops address conflict on Windows (raised heap ceiling and migrated base address above 4GB).

---

## 8.2 (2026-08-23)

Changes since the previous released version 8.1.

### Improved

- Unified button system: the four buttons in the notification detail dialog changed to Material3 Button system (delete red-fill / open theme-color / restore gray-fill / create-rule outlined); rule page, wizard page, and edit page buttons unified to 14dp corner radius.
- Consolidated dangerous operations and corner-radius tiers: two dangerous variants (red-fill button / red-text TextButton) consistent across the project; corners unified to large-container 16dp, card 12dp, small-widget 8dp.
- Unified title hierarchy and dialogs: main page headlineMedium+Bold (Settings page adds main title), sub-page titleLarge+back; merged redundant dialog components.
- Icon normalization: week-view ◀/▶ and leftover Unicode decorative characters replaced with vector icons.

### Fixed

- Removed the inconsistent outlined-button variant between the Settings page date shortcut entry and list items.
- Merged redundant components in the notification detail dialog, deleted the unused NotificationDetailsDialog.

---

## 8.1 (2026-08-22)

Changes since the previous released version 8.0.

### Improved

- History page multi-level sticky headers: bottom tab fixed sticky, stats area and collapsed-group headers stick below the tab, no longer pushing off the label bar when scrolling.
- Rule wizard simplified: removed bottom delete key, action arrows, and add-condition button; condition area fixed to three rows, new-rule creation more focused.
- Notification detail dialog widened (side padding reduced to 12dp), fixed display of Delete / Open / Create Rule / Restore four buttons.
- Launch icon redone: replaced with adaptive vector icon (three-bar gray) with refined scaling.

### Fixed

- Notification history list layout order error.
- Collapsed-section expand/collapse scroll position error: after collapsing, auto-return to section header.
- Same-app multi-collapsed-section collapse count error.
- Notification detail dialog button display anomaly.
