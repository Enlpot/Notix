# Notix 8.15.2 Release Notes

Changes since 8.15.1.

## Fixed
- **Dynamic accent color no longer collapses to a single neutral gray for unresolvable apps.** Notifications from apps whose icon cannot be resolved on the current device (uninstalled, monochrome/adaptive-only, or a missing package name) previously fell back to one shared neutral gray, making stored history cards and group headers hard to tell apart. They now get a deterministic, per-package hash-derived accent color, so each app keeps a distinguishable color.

## Improved
- **Group headers now use the same background-derived accent as notification cards**, unifying the color source across the History screen (previously headers used a different engine field than cards).
- **Added diagnostic logging to `NotificationColorEngine`.** Each color-resolution fallback (null package name / icon not found / monochrome icon) now emits a `Log.w` with the package name, making missing-color issues far easier to diagnose on device.

## Notes
- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells; their real execution lands in a future release.
