# Notix 8.8 Release Notes

Changes since the previous released version 8.7.

### Improved

- Unified all Settings-page dialogs to the same visual style as the Crash Log dialog: consistent background, button layout, spacing, title and text typography. The whole app now shares one cohesive dialog design language.
- Added a second-confirmation dialog to every delete / clear / 清除 operation across the app (notification delete, rule delete, rule action delete, clear history by all / by date range / by app, stop monitoring, listener pause, and crash-log clear). The confirmation matches the unified dialog style and shows explicit 确认 / 取消 buttons; the destructive action runs only after 确认 is tapped.

### Fixed

- Dialog buttons could wrap to two lines when two buttons shared a row; they now always stay single-line, falling back to one button per row when a row would be too wide.
- Long dialog titles were truncated at a single line; titles now wrap safely to a second line instead of being cut off.
- The clear-by-date-range and clear-by-app confirmations reused the "clear all history" wording; they now use their own dedicated, accurate strings (added in all supported languages).
