# Notix 8.15.1 Release Notes

Changes since 8.15.0.

## Fixed
- **History screen: tapping the search button no longer switches the active sub-tab.** The search and tab header was previously nested inside each `HorizontalPager` page; expanding search perturbed the pager layout and settled on the wrong page, which then locked the selected tab. The header is now rendered once above the pager, so search expand/collapse no longer affects tab selection.

## Notes
- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells; their real execution lands in a future release.
