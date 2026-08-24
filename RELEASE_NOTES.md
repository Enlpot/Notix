# Notix 8.14 Release Notes

Changes since the previous released version 8.13.

## New
- **Customizable freeze duration for ongoing notifications** — the `Remove` action's "Include ongoing notifications" switch now offers a duration picker (1 hour / 1 day / 7 days / 30 days / 1 year, default 7 days). A frozen ongoing notification automatically returns to the notification shade once the duration expires.
- **"Restore ongoing notifications" in Settings** — a new entry under Rules & Data restores every ongoing notification frozen by your rules in one tap, with a confirmation dialog.

## Fixed
- v8.13 froze ongoing notifications with a near-infinite duration (~146 years). Because snoozed notifications persist across reboots on Android 11+ and the public API has no un-snooze call, deleting a rule left the notification effectively gone forever. v8.14 fixes this by:
  - Making the freeze duration user-selectable (default 7 days).
  - Restoring that rule's frozen notifications automatically when the rule is deleted.
  - Restoring via a short re-snooze (100 ms) on the same notification key, which overrides the original expiry and brings the notification back almost immediately.

## Improved
- The `Remove` action summary now shows the configured freeze duration, e.g. "Remove notification (including ongoing, freeze 7 days)".

## Notes
- The "include ongoing" path uses `snoozeNotification` and is only available on Android 8.0 (API 26) and above; on older systems it silently falls back to `cancelNotification`.
- On Android 11 and above, snoozed notifications persist across reboots (the earlier "reboot restores them" note in v8.13 was incorrect). Recovery happens by letting the duration expire, deleting the rule, or using the new Settings entry.
- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells; their real execution lands in a future release.
