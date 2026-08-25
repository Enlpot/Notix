# Notix 8.15.0 Release Notes

Changes since 8.14.1. This release completes the app-wide UI refactor (Stages 2–12): a new design system now drives every screen, with a unified component library, consistent dialogs, and verified light/dark themes.

## New
- **Design system foundation.** A single Notix theme token set (colors, spacing, typography, corner radii, layout) now drives all screens, replacing scattered hardcoded values. A built-in design-system preview was added for development and QA.
- **Reusable component library.** Shared building blocks (`SectionHeader`, `NotificationCard`, `FilterChip`, dialog components, etc.) are centralized and reused across History, Rules, Settings, and the rule wizard.

## Improved
- **History screen** fully tokenized and rebuilt on reusable card components; spacing and dark/light themes verified on device.
- **Rules screen** migrated to the shared component set.
- **Settings screen** tokenized and switched to shared components.
- **Rule wizard** tokenized end-to-end.
- **Dialog system unified** into one `NotixDialog` style app-wide (condition config, match-mode picker, confirmations, about) with consistent sizing, corner radius, and scrim/outside-tap behavior.

## Changed
- Light and dark themes now share the same token-driven palette; the full app passed visual regression across 5 end-to-end flows on a real device.

## Notes
- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells; their real execution lands in a future release.
