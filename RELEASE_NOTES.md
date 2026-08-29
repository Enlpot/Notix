# Notix 8.28.0 Release Notes

Changes since 8.27.0.

## Fixed

- **Rules applied to active notifications now correctly mark history as blocked.** When a newly enabled rule dismissed notifications already in the shade, the global dedup path skipped updating the `blocked` flag on existing history rows, so the "已过滤" tab stayed empty even though notifications were removed. Existing rows with matching `sbnKey + postTime` now get their `blocked` flag updated to 1.

## Notes

- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells; their real execution lands in a future release.
