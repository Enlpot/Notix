# Notix 8.9 Release Notes

Changes since the previous released version 8.8.

### Improved

- Permission Management: every permission card is now tappable and jumps straight to the matching system settings screen (Notification Access → notification listener settings, Post Notifications → app notification settings, Battery Optimization → battery-optimization exemption, Foreground Service → app details). A chevron on each card signals the entry point.
- Permission Management screen now lists all app permissions with their Android constant name and the exact component that uses them, so every granted/required permission is fully explained in one place.

### Changed

- Removed the restricted `QUERY_ALL_PACKAGES` permission and 32 unused package-visibility entries from the manifest. App icons and app names come from captured notifications, not a full package scan, so behavior is unchanged while the Play Store restricted-permission review risk is eliminated.
- Unified dialog sizing: all `NotixDialog`-based dialogs (Settings dialogs, Permission Management, storage usage, and every delete/clear confirmation) now use a wide, compact layout (92% width, 85% height cap) consistent with the notification detail dialog, with tap-scrim-to-dismiss. The permission dialog fits all four cards on one screen.
