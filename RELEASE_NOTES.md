# Notix 8.12 Release Notes

Changes since the previous released version 8.11.

## Improved
- Rule condition "Configure condition" dialog now uses the app-wide `NotixDialog` style (replacing the old `AlertDialog`): fixed 520dp height, a three-tab layout (Keyword / Phone State / Time) with a scrollable content area so long content scrolls inside the dialog.
- Match-mode picker moved out of the inline `DropdownMenu` into a dedicated `NotixDialog`: a scrollable list with a check-circle on the selected item, divider-separated rows, and the `ADVANCED` mode shown disabled with a hint.
- Keyword input is now a popup dialog opened by tapping a trigger; the "include A and not include B" (MIXED) mode exposes two separate inputs (include A / exclude B), while other modes show a single "include keyword" input — matching the rest of the dialog family.

## Changed
- Keywords are shown directly as chips on the condition screen; tapping a chip opens the input dialog prefilled for editing, and the trailing × removes it.
- Keyword input dialog: removed the inline "+" button; the bottom "OK" button now commits the current text and closes; the input field wraps long text (min 2 / max 5 lines).

## Notes
- `STRONG_REMIND` and `POSTPONE` actions remain UI-only shells in v8.12; their real execution (high-priority heads-up + ring/vibrate, delayed re-post) lands in a future release.
