# Notix 8.16.0 Release Notes

Changes since 8.15.2.

## Improved
- **Notification cards are more compact.** Internal padding reduced from 16dp to 12dp and inter-row spacing from 4dp to 2dp, bringing a typical Chinese card (app name + title + summary + timestamp) from 319px to ~280px (107dp) on a 420dpi device. Font sizes are unchanged; more cards fit per screen.
- **Consistent vertical spacing between notification cards.** Cards now use 2dp vertical padding, matching the gap between app-group headers, so the list rhythm is uniform across grouped and ungrouped sections.
- **Fold-expanded cards now display the notification title.** Previously the body cards inside an expanded fold group used compact mode which hid the title; they now show the full title while retaining the horizontal indent that distinguishes them from top-level cards.

## Fixed
- **Removed the redundant "其余 N 条" footer line on aggregated cards.** The count badge in the top-right corner already conveys the total number of grouped notifications; the footer line was duplicate information and has been removed.
- **Fixed excessive blank scroll space on empty or low-content history tabs.** The list previously included a full-viewport `scroll_room` spacer plus an extra 240dp bottom content-padding margin, so even an empty "已过滤" tab could be scrolled up into a full screen of blank space. Both have been removed; the chart header still scrolls out naturally when there is enough content, but short lists no longer over-scroll.

## Notes
- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells; their real execution lands in a future release.
