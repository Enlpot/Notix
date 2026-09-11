# Notix — Architecture Overview

> Matches **v8.57.0** (versionCode 203) `com.enlpot.notix`. Source wins over docs.  
> Older full copy (v8.15.2): [archive/v8.15.2/ARCHITECTURE.md](archive/v8.15.2/ARCHITECTURE.md).

## Overview

Single-module Android app: listen to notifications, evaluate user rules, run an **ordered action chain** (Dismiss / Open / Click Button / Copy / TTS / Delay).  
**As of v8.57.0 the word-cloud/tokenizer plugin stack is gone, including the `INTERNET` permission — the core path is fully offline.**

| Property | Value |
|---|---|
| applicationId | `com.enlpot.notix` |
| UI | Jetpack Compose + Material 3 |
| min / target / compile | 24 / 36 / 36 |
| Version | 8.57.0 (203) |
| License | MIT (DoNotNotify fork) |

## Runtime entry points

- **`NotixApp`** — debug log → crash log → health channel + `HealthCheckWorker` (6h).
- **`MainActivity`** — no Jetpack Navigation; setup wizard until done, then History / Rules / Statistics / Settings. Throttled refresh on `ACTION_HISTORY_UPDATED`. **Landscape and portrait each build their own content tree.**
- **`NotificationBlockerService`** — listener + `specialUse` foreground keep-alive; rebind on disconnect.

## Pipeline

```
onNotificationPosted
  → pause / self-package guard
  → title/text (RemoteViews fallback for empty text)
  → EnvironmentSnapshot (~10s cache)
  → RuleMatcher (first match wins)
       Pass → history (blocked=false)
       Apply → Action Flow (~3s debounce per key) + blocked history + hitCount
  → broadcast ACTION_HISTORY_UPDATED
```

## Rules & actions

`BlockerRule`: multi-app sources + keyword condition + extra conditions (screen / charge / DND / Bluetooth / time) + `actions`.

Implemented: DISMISS, CLICK_BUTTON, OPEN_NOTIFICATION, COPY, TTS, DELAY.  
**Not implemented at runtime**: STRONG_REMIND, POSTPONE (Flow records FAILED). ADVANCED match mode is UI-only (always false).

## Storage

| Store | Role |
|---|---|
| `rules.json` (AtomicFile) | Rules; id-keyed mutations under a lock |
| Room `notix.db` | Primary history (groups + changes), schema v7 (`word_frequency` dropped) |
| `app_info.db` | Package → label/icon |
| SharedPreferences | Unmonitored apps, stats, snoozed keys, settings |

## Privacy

Permissions: notification listener, `POST_NOTIFICATIONS`, battery-optimization exemption, foreground specialUse. **No `INTERNET`.** Known apps come from history/rules, not a full package scan.

## Known placeholders

- STRONG_REMIND / POSTPONE saveable but fail at execution  
- `repostNotification` / `RULE_REPOST_CHANNEL_ID` leftover from old SILENT model  
- ADVANCED matching always false  

See also [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md).
