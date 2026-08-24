**English** | [简体中文](README.zh-CN.md)

# Notix

> Take back control of your notifications.

Notix is a **fully offline** Android notification manager for power users. It sits on Android's notification listener, captures everything that comes in, and lets you define precise, automated rules — block the noise, silence what matters less, read important ones aloud, copy verification codes, or open and even tap buttons on incoming notifications.

No account. No cloud. No network. Everything stays on your device.

> **Fork notice.** Notix is a rebranded and repackaged fork of [DoNotNotify](https://github.com/anujja/DoNotNotify) by Anuj, under the MIT License. It carries its own release channel, package name, and signing key. Both copyright notices are preserved in [LICENSE](LICENSE).

---

## Why Notix

Most notification apps are either too simple (just a mute toggle) or too intrusive (cloud sync, accounts, tracking). Notix is built around three principles:

- **Local-first.** Your notifications never leave the device. There is no network permission in the manifest.
- **Rule-driven.** Behavior is defined by explicit user rules — there is no built-in spam filter guessing for you.
- **Survives the OS.** A foreground keep-alive service plus periodic health checks keep the listener running even when the system reclaims background processes.

## Features

**Core engine**
- Real-time capture via `NotificationListenerService`, backed by a foreground service and health checks
- Ordered action chains per rule: **Dismiss · Silent (re-post low-priority) · Open · Click Button · Copy (title / text / both) · TTS speak · Delay**
- Flexible matching: target one or more apps, with `contains any / contains all / not contains any / not contains all`, plus mixed `contains A but not B`
- Context conditions: screen on/off, charging state, Do Not Disturb, Bluetooth headset (optional device names), time window + weekday

**Workflow**
- Visual rule wizard (app → matching → conditions → actions); or start a rule directly from any notification in History
- Notification History with **By Time / By App / Filtered** tabs and a daily statistics chart
- Per-rule hit counters and a **Blocked** view showing what was acted on and by which rule
- Restore any processed notification back to the shade

**Quality of life**
- Lazy-loaded TTS with Chinese-first locale fallback and `{app}` / `{title}` / `{text}` templates
- Versioned JSON import/export (backward compatible)
- On-device crash log viewer
- First-run setup wizard covering notification access, battery-optimization exemption, and OEM auto-start (Xiaomi, Huawei, OPPO, OnePlus, vivo, Samsung, …)
- OLED-friendly pure-black Material Design 3 dark theme; compact icon-only bottom nav (long-press for labels) and a unified landscape layout

## Screenshots

> Placeholder — screenshots to be added. Drop the images under `docs/screenshots/` and update the paths below.

| Screen | File | Caption |
| --- | --- | --- |
| History & statistics | `docs/screenshots/history.png` | Notification history with the daily chart panel |
| Rules list | `docs/screenshots/rules.png` | Saved rules with hit counters |
| Rule wizard | `docs/screenshots/wizard.png` | Visual rule editor (app → matching → conditions → actions) |
| Settings | `docs/screenshots/settings.png` | Import/export, permission management, crash logs |

Recommended size: 1080×2340 (portrait) or 2400×1600 (landscape), PNG.

## Requirements

- **Android 7.0 (API 24) or higher**
- Notification listener access (granted through the in-app setup wizard)
- On Android 13+, the notification posting permission used by the block/silence flow

## Getting started

1. Install and launch — the setup wizard guides you through granting notification listener access.
2. *(Recommended)* Exempt Notix from battery optimization, and on OEM devices enable auto-start so the listener keeps running.
3. Watch incoming notifications in **History**; the chart panel gives a daily overview.
4. Tap a notification to seed a rule, or open **Rules** and use the visual wizard (app → matching → conditions → actions).
5. Processed notifications land in **Blocked** — tap any entry to inspect, edit the handling rule, or restore it.
6. In **Settings** you can import/export rules, reset hit counters, clear history, or open crash logs.

## Building from source

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK (R8 minified, signed with the Notix keystore)
./gradlew installDebug       # install debug build to a connected device
```

Signing credentials are read from `local.properties` (`KEYSTORE_NOTIX_*`) or environment variables and are never committed.

**Toolchain:** Gradle 8.13 · Android Gradle Plugin 8.13.2 · Kotlin 2.0.21 · `compileSdk 36` / `minSdk 24` / `targetSdk 36` · Java 11.

## Documentation

Developer and contributor docs live in [`docs/`](docs/):

- [Architecture & Codebase Overview](docs/ARCHITECTURE.md)
- [API Reference](docs/API_REFERENCE.md)
- [Developer Guide](docs/DEVELOPER_GUIDE.md)

Version history is tracked in [VERSION_HISTORY.md](VERSION_HISTORY.md); per-release notes in [RELEASE_NOTES.md](RELEASE_NOTES.md).

## Status & roadmap

Notix is at **v8.9** and actively developed. The v1 scope focuses on the notification capture/history pipeline, the visual drag-and-drop rule editor (AND/OR), OTP auto-copy, automatic removal of unimportant notifications, and Bluetooth / driving TTS announcements.

## License

MIT License — see [LICENSE](LICENSE).
