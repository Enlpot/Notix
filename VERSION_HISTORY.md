# Notix Version History

> This file is the local cumulative version history, ordered newest-first.
> At each release, that version's notes are posted **alone** as the GitHub Release body (see `RELEASE_NOTES.md`).
> This file only retains the full history and is not used for publishing.

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
