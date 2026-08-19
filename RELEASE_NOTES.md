# v7.43

## Removed
Removed the incomplete Community Share feature: deleted CommunityShare.kt and its 80 related string resources across all 8 locales.

# v7.42

## New Features
- Pure black (OLED-friendly) dark theme following the Material Design 3 dark color scheme
- Bottom navigation bar compacted to 56dp with icon-only items; long-press an icon to reveal its label
- Unified landscape layout for all three tabs: fixed chart panel on the left, page content on the right, separated by a vertical divider

## Improvements
- Selected day state persists across rotation in landscape mode (shared chart panel)
- Adjacent history sub-tabs are pre-composed to avoid blank flashes when swiping
- Bottom tabs now switch by tap only, while the top sub-tabs remain swipeable

## Bug Fixes
- Fixed landscape screens showing the legacy gray window background instead of pure black
