# Notix 8.13 Release Notes

Changes since the previous released version 8.12.

## New
- **Remove ongoing notifications (fixed / foreground-service / "drawing on top" notifications)** — the `Remove` action now has an `Include ongoing notifications` switch. When enabled, ongoing notifications that the system refuses to clear via `cancelNotification` (e.g. music players, foreground services, the "Drawing on top of other apps" alert) are suppressed via `snoozeNotification` with a very large duration, matching the behavior of apps like Notification Filter Box. Default is **off** to preserve the previous behavior.

## Improved
- DISMISS action card in `RuleWizardScreen` now distinguishes `Remove notification` vs `Remove notification (including ongoing)` in the action-flow summary, so the rule list shows whether the rule targets ongoing notifications at a glance.

## Notes
- The "include ongoing" path uses `snoozeNotification` and is only available on Android 8.0 (API 26) and above; on older systems it silently falls back to `cancelNotification`.
- Snoozed notifications are reactivated after a device reboot (system limitation); the "Phone time back 1 year" trick in Notification Filter Box works the same way to recover them earlier.
- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells in v8.13; their real execution lands in a future release.
