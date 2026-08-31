# Notix Version History

> This file is the local cumulative version history, ordered newest-first.
> Release notes are now auto-generated from git log by the GitHub Actions workflow (see .github/workflows/release.yml).
> This file retains the full curated history and is not used directly for publishing.

---


## 8.41.0 (2026-08-31)

**Improved**
- **Ongoing notification aggregation optimization.** Normal notifications (e.g. WeChat) now skip ongoing notification groups when checking for aggregation, so they are no longer broken by frequently-updating ongoing notifications (download progress, music playback, etc.). Same-title WeChat messages still aggregate correctly even when an ongoing notification update occurs between them.

**Technical**
- NotificationGroupEntity gained was_ongoing field to mark ongoing notification groups.
- Database version bumped to v3.

---

## 8.40.0 (2026-08-31)

**New**
- **Statistics page wave 1**: Notification trend line chart (last 7 days, Canvas with gradient fill and data points), 24-hour distribution heatmap (last 7 days x 24 hours grid, tap for details).
- **Statistics page wave 2**: App notification share donut chart (top 5 apps individually, rest grouped as "Other"), rule effectiveness ranking (by hit count descending, horizontal progress bars).
- **Statistics page wave 3**: Notification health score (0-100, circular progress + level label), score dimension details (7-day average count, source diversity, rule filter efficiency), personalized improvement suggestions.

---

## 8.39.0 (2026-08-30)

**New**
- **Statistics tab**: 4th bottom navigation tab showing total notifications (today/week/month), filtered count, rule hit count, top 5 apps ranking.
- **Rule merge suggestion**: When creating a rule, if an existing rule matches all conditions except keywords (app, phone state, time, action flow, match mode), a merge prompt appears.
- **Rule card divider**: Divider between condition area (keywords/state/time) and action flow.

**Improved**
- **Smooth scroll-to-top animation**: Rules and Settings tabs now use smooth animation when tapping bottom tab, consistent with History tab.

**Fixed**
- **Statistics page blank in portrait**: The portrait (else) branch had an independent Box structure that did not call screenContent(), so the Statistics page Box was missing. Fixed in both branches.
- **Settings tab scroll-to-top not working**: After changing from 3 to 4 tabs, Settings index changed from 2 to 3, but the scroll-to-top condition still checked index == 2 (which is now Statistics). Fixed to index == 3.

---

## 8.38.0 (2026-08-30)

**New**
- **Custom compact Switch component**: Replaced system Switch with custom design; off state also draws capsule outline (no fill), more compact and consistent.

**Improved**
- **Rule card layout**: Rule name font enlarged, divider between title row and app name row, overall more compact.

---

## 8.37.0 (2026-08-30)

**Improved**
- **Rule card layout optimization**: Further compactness adjustments to rule card title row and spacing.

---

## 8.36.0 (2026-08-30)

**Improved**
- **Rule condition keyword input**: Changed from small input field with plus button to a large clickable card that opens a dialog for keyword input. Tags displayed below the card.

---

## 8.35.0 (2026-08-30)

**Improved**
- **Rule card layout optimization**: Rule description area reorganized into 3 rows (keywords, phone state, time), hidden if unlimited.
- **Storage usage dialog fix**: Fixed incorrect file listing (removed stale "history.json" entry) and updated size calculation and labels.

---

## 8.34.0 (2026-08-30)

**New**
- **Notification detail dialog persistence type**: Shows "Ongoing notification" or "Normal notification" in title area with theme color + bold.
- **Filtered tag moved to title row**: The "已过滤" tag in notification detail dialog moved to the same row as persistence type, 4pt spacing.

---

## 8.33.0 (2026-08-30)

**Fixed**
- **Filtered tab empty-state overscroll**: Fixed the "已过滤" tab allowing excessive upward scroll when empty (chart scrolled off screen).
- **Rules/Settings tab scroll-to-top**: Tapping the bottom tab while on Rules or Settings page now scrolls the list back to top.

---

## 8.32.0 (2026-08-30)

**Improved**
- **App info refresh optimization**: "Refresh app info" in Settings now immediately re-fetches app info via PackageManager instead of clearing cache.
- **Unmonitored app state sync**: Stopping monitoring an app in the app group now immediately refreshes the unmonitored app list in Settings.

---

## 8.30.0 (2026-08-30)

**Fixed**
- **Bottom tab switch scroll position loss**: Switching tabs no longer destroys and recreates the History screen, preserving scroll position and fold/expand state.

---

## 8.29.0 (2026-08-30)

**Fixed**
- **Bottom tab switch scroll position loss**: Initial fix for scroll position being lost when switching between tabs.

---
## 8.28.0 (2026-08-29)

**Fixed**
- **Rules applied to active notifications now correctly mark history as blocked.** When a newly enabled rule dismissed notifications already in the shade, the global dedup path skipped updating the locked flag on existing history rows, so the '已过滤' tab stayed empty even though notifications were removed. Existing rows with matching sbnKey + postTime now get their locked flag updated to 1.

---

## 8.27.0 (2026-08-29)

**New**
- **Rules automatically apply to notifications already in the shade.** Creating, updating, or enabling a rule now triggers a one-time pass over all currently active notifications, matching and executing rule actions (dismiss, etc.) without waiting for new posts. Previously only newly posted notifications were evaluated.

---

## 8.26.0 (2026-08-29)

**Fixed**
- **Duplicate notification groups (BUG-001).** Global dedup now keys on sbnKey + postTime instead of sbnKey alone, so updated notifications with the same key but different post time are recorded as separate history entries instead of being silently dropped.

---

## 8.25.0 (2026-08-29)

**New**
- **Sync active notifications on listener reconnect.** When the notification listener service reconnects (after process death, permission toggle, etc.), it now pulls the current active notification list and backfills any missed entries into history.

**Fixed**
- **Removed 3-second debounce on same-key notifications.** The debounce was intended to reduce duplicate writes but caused rapid chat messages (same conversation key) to be dropped from history. It has been removed; each post is now recorded.

---

## 8.24.0 (2026-08-29)

**Maintenance**
- Version bump following the Room migration and search improvements in 8.23.0.

---

## 8.23.0 (2026-08-29)

**New**
- **Full-text search across all notification history.** Search now queries the entire Room database, not just the loaded page, so notifications from months ago are findable.
- **Paginated history list loading.** The history list now loads pages of 100 records on demand, keeping scroll smooth even with tens of thousands of stored notifications.
- **Search result limit raised to 500.** Auto-cleanup of legacy JSON files and storage stats now include the Room database size.

**Performance**
- **Composite database indexes** added for the most common query patterns (by time, by package, by blocked status).

**Fixed**
- **Database version upgraded to 2** with destructive migration to apply the new schema and indexes cleanly.

---

## 8.22.0 (2026-08-29)

**New**
- **Notification history migrated to Room database.** The legacy JSON-file storage (
otification_history.json) has been replaced with a Room/SQLite database, enabling fast queries, pagination, and full-text search at scale.
- **Ongoing notification aggregation.** Ongoing (foreground) notifications from the same app are now aggregated into a single card with a change-count badge, consistent with normal notification aggregation.

---

## 8.21.0 (2026-08-29)

**New**
- **Unmonitored app management.** Apps whose notification monitoring has been stopped are now visually distinguished in the by-app group view (diagonal strike-through), with a one-tap resume button directly on the card. A dedicated settings card lets users multi-select and search apps to resume monitoring.
- **Resume monitoring confirmation dialog.** Resuming monitoring for a stopped app now shows a confirmation dialog, and the success toast displays the app name instead of the raw package name.

---

## 8.20.0 (2026-08-29)

**New**
- **Dynamic color toggle in Settings.** Users can now enable or disable per-app dynamic accent color extraction. When disabled, all cards use the default neutral background.
- **Aggregation window card colors now match the main card.** The change-count aggregation dialog previously showed uncolored cards; it now uses the same dynamic accent as the history list.

**UI**
- **Reduced chart top spacing** in the history header.
- **Replaced the bell icon** in the history header with a play/pause-style icon that better reflects its function.

---

## 8.19.0 (2026-08-29)

**Improved**
- **Low-priority optimizations (items 7-8).** Minor performance and polish improvements across the notification color engine and history rendering pipeline.

---

## 8.18.0 (2026-08-29)

**New**
- **Notification color engine optimizations (8 items, high + medium priority).** The NotificationColorEngine was refactored for faster color extraction, better caching, and more consistent fallback behavior across app icons.

**UI**
- **Settings: independent cards with 4dp gap**, removing dividers and navigation chevrons for a cleaner card-based layout.
- **Settings: About section changed from expandable to dialog style**, consistent with other dialogs in the app.
- **Removed dialog X (close) buttons** app-wide - tapping outside or pressing back already closes dialogs.
- **Permission monitoring status pill moved to the title line** in the monitoring dialog, so it stays visible while scrolling.
- **About section cards now use SettingsRow with icons** (Info + Lock), matching other settings items.

---

## 8.17.0 (2026-08-29)

**UI**
- **Unified page title styling** across Notification History, Rules, and Settings screens - same font, size, and subtitle layout.
- **Settings sticky header.** The '设置' title now scrolls naturally and pins to the top when scrolling up, unpinning on scroll down.
- **Rules page title position** aligned with the Settings page for visual consistency.

**Fixed**
- **Settings section headers now align with card left edge** (16dp horizontal padding), instead of being flush against the screen edge.

---

## 8.16.0 (2026-08-29)

**Improved**
- **Notification cards are more compact.** Internal padding reduced from 16dp to 12dp and inter-row spacing from 4dp to 2dp, bringing a typical Chinese card (app name + title + summary + timestamp) from 319px to ~280px (107dp) on a 420dpi device. Font sizes are unchanged; more cards fit per screen.
- **Consistent vertical spacing between notification cards.** Cards now use 2dp vertical padding, matching the gap between app-group headers.
- **Fold-expanded cards now display the notification title.** Previously the body cards inside an expanded fold group used compact mode which hid the title; they now show the full title while retaining the horizontal indent.

**Fixed**
- **Removed the redundant "其余 N 条" footer line on aggregated cards.** The count badge in the top-right corner already conveys the total.
- **Fixed excessive blank scroll space on empty or low-content history tabs.** Removed the full-viewport `scroll_room` spacer and the extra 240dp bottom content-padding margin; short lists no longer over-scroll into blank space.

**Notes**
- `STRONG_REMIND` and `POSTPONE` remain UI-only shells.

---

## 8.15.2 (2026-08-25)

**Fixed**
- **Dynamic accent color no longer collapses to a single neutral gray for unresolvable apps.** Notifications from apps whose icon cannot be resolved on the current device (uninstalled, monochrome/adaptive-only, or a missing package name) previously fell back to one shared neutral gray, making stored history cards and group headers hard to tell apart. They now get a deterministic, per-package hash-derived accent color, so each app keeps a distinguishable color.

**Improved**
- **Group headers now use the same background-derived accent as notification cards**, unifying the color source across the History screen (previously headers used a different engine field than cards).
- **Added diagnostic logging to `NotificationColorEngine`.** Each color-resolution fallback (null package name / icon not found / monochrome icon) now emits a `Log.w` with the package name, making missing-color issues far easier to diagnose on device.

**Notes**
- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells.

---

## 8.15.1 (2026-08-25)

**Fixed**
- **History screen: tapping the search button no longer switches the active sub-tab.** The search and tab header was nested inside each `HorizontalPager` page; expanding search perturbed the pager layout and settled on the wrong page, which then locked the selected tab. The header is now rendered once above the pager, so search expand/collapse no longer affects tab selection.

**Notes**
- `STRONG_REMIND` and `POSTPONE` remain UI-only shells.

---

## 8.15.0 (2026-08-25)

**New**
- **Design system foundation.** A single Notix theme token set (colors, spacing, typography, corner radii, layout) now drives all screens, replacing scattered hardcoded values. A built-in design-system preview was added for development and QA.
- **Reusable component library.** Shared building blocks (`SectionHeader`, `NotificationCard`, `FilterChip`, dialog components, etc.) are centralized and reused across History, Rules, Settings, and the rule wizard.

**Improved**
- **History screen** fully tokenized and rebuilt on reusable card components; spacing and dark/light themes verified on device.
- **Rules screen** migrated to the shared component set.
- **Settings screen** tokenized and switched to shared components.
- **Rule wizard** tokenized end-to-end.
- **Dialog system unified** into one `NotixDialog` style app-wide (condition config, match-mode picker, confirmations, about) with consistent sizing, corner radius, and scrim/outside-tap behavior.

**Changed**
- Light and dark themes now share the same token-driven palette; the full app passed visual regression across 5 end-to-end flows on a real device.

**Notes**
- `STRONG_REMIND` and `POSTPONE` remain UI-only shells.

---

## 8.14.1 (2026-08-25)

**Fixed**
- **Release-only: "Include ongoing notifications" duration chips could not be selected.** In release builds (R8 with full optimizations), the `Long` state used for the freeze duration was not comparing correctly inside `FilterChip`, so tapping a duration chip appeared to do nothing and no chip stayed selected. The UI now stores the selected duration as an index into the options list, avoiding the `Long` boxing/comparison issue under R8.

**Notes**
- `STRONG_REMIND` and `POSTPONE` remain UI-only shells.

---

## 8.14 (2026-08-24)

**New**
- **Customizable freeze duration for ongoing notifications** — the `Remove` action's "Include ongoing notifications" switch now offers a duration picker (1 hour / 1 day / 7 days / 30 days / 1 year, default 7 days). A frozen ongoing notification automatically returns once the duration expires.
- **"Restore ongoing notifications" in Settings** — a new entry under Rules & Data restores every ongoing notification frozen by rules in one tap, with a confirmation dialog.

**Fixed**
- v8.13 froze ongoing notifications with a near-infinite duration (~146 years). Because snoozed notifications persist across reboots on Android 11+ and the public API has no un-snooze call, deleting a rule left the notification effectively gone forever. v8.14 fixes this by making the duration user-selectable (default 7 days), restoring a rule's frozen notifications automatically on rule deletion, and restoring via a 100 ms re-snooze on the same key (overrides the original expiry).

**Improved**
- The `Remove` action summary now shows the configured freeze duration (e.g. "freeze 7 days").

**Notes**
- The "include ongoing" path uses `snoozeNotification` (API 26+); older systems fall back to `cancelNotification`.
- On Android 11+ snoozed notifications persist across reboots; recovery is via duration expiry, rule deletion, or the new Settings entry.
- `STRONG_REMIND` and `POSTPONE` remain UI-only shells.

---

## 8.13 (2026-08-24)

**New**
- **Remove ongoing notifications (fixed / foreground-service / "drawing on top" notifications)** — the `Remove` action now has an `Include ongoing notifications` switch. When enabled, ongoing notifications that the system refuses to clear via `cancelNotification` (e.g. music players, foreground services, the "Drawing on top of other apps" alert) are suppressed via `snoozeNotification` with a very large duration, matching the behavior of apps like Notification Filter Box. Default is **off** to preserve the previous behavior.

**Improved**
- DISMISS action card in `RuleWizardScreen` now distinguishes `Remove notification` vs `Remove notification (including ongoing)` in the action-flow summary.

**Notes**
- The "include ongoing" path uses `snoozeNotification` and is only available on Android 8.0 (API 26) and above; on older systems it silently falls back to `cancelNotification`.
- Snoozed notifications are reactivated after a device reboot (system limitation); the "Phone time back 1 year" trick in Notification Filter Box works the same way to recover them earlier.
- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells in v8.13; their real execution lands in a future release.

---

## 8.12 (2026-08-24)

**Improved**
- Rule condition "Configure condition" dialog upgraded to the app-wide `NotixDialog` style (was `AlertDialog`): fixed 520dp height with a three-tab (Keyword / Phone State / Time) scrollable content area.
- Match-mode picker is now a dedicated `NotixDialog` (was inline `DropdownMenu`): scrollable list, check-circle on the selected item, divider-separated rows, `ADVANCED` shown disabled.
- Keyword input moved into a popup `NotixDialog` opened from a trigger; MIXED ("include A and not include B") shows two inputs (include A / exclude B), other modes show one.

**Changed**
- Keywords rendered as chips directly on the condition screen; tap a chip to open the dialog prefilled for editing, trailing × to remove.
- Keyword input dialog: dropped the inline "+" button; the bottom "OK" button commits the current text and closes; input field wraps long text (min 2 / max 5 lines).

**Notes**
- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells in v8.12; their real execution lands in a future release.

---

## 8.11 (2026-08-24)

**Improved**
- Action flow drag-to-reorder is now animated end-to-end: the dragged card lifts while others shift aside to make room, snapping back with a spring on release. Built on `sh.calvin.reorderable`.

**Fixed**
- Removed dead code paths for the removed `SILENT` action: `SyncActionRunner.silent`, `ActionFlowHost.repostSilent`, and `NotificationBlockerService.repostSilent`. Updated unit-test stubs (`FakeSyncRunner` / `FakeHost`) and assertions to match.

**Notes**
- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells in v8.11; their real execution lands in a future release.

---

## 8.10 (2026-08-24)

Changes since the previous released version 8.9.

### Improved

- Rule Wizard: tapping an action card now opens a configuration dialog consistent with the rest of the app's `NotixDialog` style (title bar with X-close, 92% width, scrim-tap-to-dismiss), replacing the previous inline-expansion beneath the card.
- "Add action" picker is now a proper `NotixDialog` matching the other in-app dialogs. The bottom "Cancel" button is removed; tap the X, the scrim, or use the system back gesture to close.
- Each action card's drag handle is now a dedicated 48 × 64 dp touch column with a 28 dp icon, visually separated from the click-to-open area, so tapping or long-pressing the handle no longer accidentally opens the configuration dialog.
- Source-app picker on the Rule Wizard: the search field now has an up/down arrow on its right side to collapse/expand the app list. The expanded/collapsed state is preserved across configuration changes.

### Changed

- Action catalog re-aligned with the Notix v1 plan: the 7 previous actions are reshaped to a curated set of 8 — Remove, Click Button, Open, Copy Content, TTS Speak, Strong Remind, Wait, Postpone. Labels renamed to the agreed user-facing names (e.g. "消除通知" → "移除", "打开通知" → "打开", "播报" → "TTS 播报"). The "Silent Redisplay" action is removed.
- New actions in this release are UI-complete and safe to save: their parameters (Strong Remind sound/vibrate switches, Postpone delay-ms) can be set and persisted. Actual runtime execution of Strong Remind (heads-up + sound + vibrate) and Postpone (delayed re-delivery) is scheduled for v8.11; today they are logged as skipped and the action flow continues without crashing.

### Fixed

- Action cards no longer trigger the configuration dialog when the drag handle is tapped, because the open-dialog click target and the drag gesture target are now disjoint.
- Inconsistent dialog styling in the Rule Wizard: both the "Add action" picker and the per-action configuration are now in the same `NotixDialog` family as Settings, Permission Management, and Storage Usage dialogs.

---

## 8.9 (2026-08-23)

Changes since the previous released version 8.8.

### Improved

- Permission Management: every permission card is now tappable and jumps straight to the matching system settings screen (Notification Access → notification listener settings, Post Notifications → app notification settings, Battery Optimization → battery-optimization exemption, Foreground Service → app details). A chevron on each card signals the entry point.
- Permission Management screen now lists all app permissions with their Android constant name and the exact component that uses them, so every granted/required permission is fully explained in one place.

### Changed

- Removed the restricted `QUERY_ALL_PACKAGES` permission and 32 unused package-visibility entries from the manifest. App icons and app names come from captured notifications, not a full package scan, so behavior is unchanged while the Play Store restricted-permission review risk is eliminated.
- Unified dialog sizing: all `NotixDialog`-based dialogs now use a wide, compact layout (92% width, 85% height cap) consistent with the notification detail dialog, with tap-scrim-to-dismiss. The permission dialog fits all four cards on one screen.

---

## 8.8 (2026-08-23)

Changes since the previous released version 8.7.

### Improved

- Unified all Settings-page dialogs to the same visual style as the Crash Log dialog: consistent background, button layout, spacing, title and text typography across the app.
- Added a second-confirmation dialog to every delete / clear / 清除 operation (notification, rule, rule action, clear history by all / date range / app, stop monitoring, listener pause, crash-log clear). Confirmation matches the unified style with explicit 确认 / 取消 buttons; the action runs only after 确认.

### Fixed

- Dialog buttons now always stay single-line (one per row when a row is too wide) instead of wrapping.
- Long dialog titles no longer truncate; they wrap safely to a second line.
- Clear-by-date-range and clear-by-app confirmations now use dedicated strings instead of reusing the "clear all history" wording (added in all supported languages).

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

---

## 8.0 (2026-08-22)

Changes since the previous released version 7.47.

### New Features

- Storage Usage: Settings > General adds a "Storage Usage" entry and a secondary detail screen, showing the size of notification history / rules / other files by category, with per-item clear and one-tap clear-all (both with confirmation).
- Clear history by app: dialog restyled as a dark card; list items now show app icons, support row tap to multi-select, and one-tap select-all / clear-selection after search filtering.
- Clear history by time range: dialog restyled as a dark card; supports start/end date picking plus quick presets (7 / 30 / 90 days / all).
- Rule creation split into three columns: the rule screen is divided into "Source / Condition / Workflow"; a rule name can be filled at the top (auto-named "Unnamed Rule N" when empty); conditions are configured via a dialog (Keyword / Phone State / Time tabs); action cards support drag-to-reorder, tap-to-configure, and long-press-to-delete.
- Bottom three-tab (History / Rules / Settings) transition animation: fade + slight horizontal shift (~250ms), with each page's state preserved.

### Improved

- Rules screen: removed the floating new-button; replaced with a "Rules" main title + "N rules total" subtitle + a top banner new-button. Rule cards support long-press delete confirmation; the card-top delete button is removed while re-scan and the on/off switch are kept; empty state keeps the title and banner button.
- Notification detail dialog widened (side padding reduced to 12dp), fixed display of Delete / Open / Create Rule / Restore four buttons.
- Aggregation change window: top-right label changed from "Change n" to "n changes".
- Notification history bar chart extracted into a standalone rounded dark-gray card, with ◀/▶ swipe hint arrows added (synced in landscape left pane).
- Notification history search box auto-shows the keyboard when expanded and dismisses it on close.
- Bottom tab selected state changed to a theme-color rounded pill (≈52×32dp, 16dp radius), with icons switching to a contrasting color.

### Fixed

- Notification collapsed-card scroll position error after expand/collapse: auto-returns to the section header after collapsing.
- Same-app multi-collapsed-section key conflict caused an IllegalArgumentException crash in the list.
- Per-app collapsed sections now ordered by notification send time, avoiding tab-switch jank.

---

## 7.47 (2026-08-21)

### Fixed

- Fixed a crash (IllegalArgumentException) in LazyColumn caused by duplicate fold-toggle keys when the same app appeared in multiple non-consecutive fold segments; fold segment keys now include the segment's newest entry id.

### Improved

- Fixed history tab switching jank: switching from the Filtered tab to By Time / By App no longer freezes for ~1 second before showing the target list. Data preparation is now decoupled from the selected tab, so all three tabs share one cached dataset and tab switches trigger no recomputation.
- By App folding now segments entries by send time: within an app group, entries are only folded together when they are adjacent in the global timeline. If another app's notification falls between two entries of the same app, they form separate fold segments instead of one large collapsed group.

---

## 7.46 (2026-08-21)

### New Features

- Added notification card folding on the History screen: when an app has 4+ consecutive aggregated entries, they are collapsed into a single card with a "Show N more" toggle; works on all three tabs (By Time / By App / Filtered). The expand/collapse toggle stays sticky under the newest entry for one-tap folding, and expanded cards are slightly indented to distinguish from normal ones.

### Improved

- Added translations for the new folding strings in Spanish, French, Japanese, Korean, Polish and Russian.

---

## 7.45 (2026-08-20)

### New Features

- Added an optional "Extract remote views text" switch (default off): when enabled, notifications without visible text will have their action button labels and content descriptions extracted and used for rule matching and history records.

### Fixed

- Fixed lag when swiping between "Filtered" and "By App" tabs on the History screen: group-by/sort computation is now cached and no longer fully recomputed on every recomposition.
- Fixed app icons appearing with delay after tab switches or list recycling: added an in-process memory cache for app icons so PackageManager is not hit repeatedly.

---

## 7.43 (2026-08-19)

### Removed

- Removed the incomplete Community Share feature: deleted CommunityShare.kt and its 80 related string resources across all 8 locales.

---

## 7.42 (2026-08-18)

### New Features

- Pure black (OLED-friendly) dark theme following the Material Design 3 dark color scheme.
- Bottom navigation bar compacted to 56dp with icon-only items; long-press an icon to reveal its label.
- Unified landscape layout for all three tabs: fixed chart panel on the left, page content on the right, separated by a vertical divider.

### Improved

- Selected day state persists across rotation in landscape mode (shared chart panel).
- Adjacent history sub-tabs are pre-composed to avoid blank flashes when swiping.
- Bottom tabs now switch by tap only, while the top sub-tabs remain swipeable.

### Fixed

- Fixed landscape screens showing the legacy gray window background instead of pure black.

---

## 7.39 (2026-08-18)

### New Features

- History tab now supports swipeable navigation between the three sub-tabs (HorizontalPager).
- "Filtered" tab groups notification records by the matched rule.

### Improved

- History lists remain scrollable even with few notifications (fixed the issue where scrolling was impossible with short content).
- Double-tapping the "History" tab in the bottom navigation quickly returns to the current week (clears date filter).

### Fixed

- Fixed rule wizard state being lost when the screen rotates.

---

## 7.38 (2026-08-18)

- Initial release notes entry. Full changelog: https://github.com/Enlpot/Notix/commits/v7.38
- Notification detail dialog button display anomaly.

