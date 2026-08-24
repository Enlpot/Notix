# Notix 8.11 Release Notes

Changes since the previous released version 8.10.

## Improved
- Action flow drag-to-reorder is now animated end-to-end: the dragged card lifts and tilts while others shift aside to make room, snapping back with a spring on release. Built on `sh.calvin.reorderable` for production-grade reorder gestures.

## Changed
- Source rules with the previously removed `SILENT` action field (from old `RuleAction`) deserialize cleanly as unknown; no migration needed for users on 8.10 or earlier since SILENT was never active in shipped rules.

## Fixed
- Removed dead code paths that referenced the removed `SILENT` action: `SyncActionRunner.silent`, `ActionFlowHost.repostSilent`, and the `NotificationBlockerService.repostSilent` implementation. Test stubs and assertions were updated to match.

## Notes
- Strong-remind (`STRONG_REMIND`) and postpone (`POSTPONE`) actions remain UI-only shells for v8.11; their real execution (high-priority heads-up + ring/vibrate, delayed re-post via `Handler.postDelayed`) lands in a future release.