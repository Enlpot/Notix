# Notix 8.10 Release Notes

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
