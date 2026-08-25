# Notix 8.14.1 Release Notes

Changes since the previous released version 8.14.

## Fixed
- **Release-only: "Include ongoing notifications" duration chips could not be selected.** In release builds (R8 with full optimizations), the `Long` state used for the freeze duration was not comparing correctly inside `FilterChip`, so tapping a duration chip appeared to do nothing and no chip stayed selected. The UI now stores the selected duration as an index into the options list, avoiding the `Long` boxing/comparison issue under R8.

## Notes
- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells; their real execution lands in a future release.
