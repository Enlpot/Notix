**English** | [简体中文](README.zh-CN.md)

# Notix

Notix is a fully offline Android app that intercepts notifications and automatically acts on them using customizable rules — block them, silence them, read them aloud, copy them, open them, or even tap their action buttons.

> **Derived from [DoNotNotify](https://github.com/anujja/DoNotNotify)** by Anuj (MIT License). Notix is a rebranded and repackaged fork with its own release channel, package name, and signing key.

## Features

- **Real-time Notification Handling** - Built on Android's `NotificationListenerService` with a foreground keep-alive service and periodic health checks, so rules keep working even after the system tries to reclaim the background process
- **Action Flow Rules** - Each rule runs a strictly ordered chain of actions: **Dismiss**, **Silent** (re-post in a low-priority channel), **Open**, **Click Button**, **Copy** (title / text / both), **TTS speak**, and **Delay**
- **Flexible Matching** - Rules target one or more source apps with keyword matching modes: contains any, contains all, not contains any, not contains all, or a mixed "contains A but not B" mode
- **Context Conditions** - Optionally restrict rules by screen state (on/off), charging state (wired/wireless/battery), Do Not Disturb state, Bluetooth headset connection (with optional device names), and time windows with weekday selection
- **Visual Rule Wizard** - A step-by-step wizard (app → matching → conditions → actions) makes creating complex rules easy; you can also tap any notification in History to start a rule from it
- **Notification History** - Browse all received notifications with **By Time**, **By App**, and **Filtered** sub-tabs, plus a statistics chart panel with per-day filtering; retention and clearing are fully user-controlled
- **Blocked/Processed Tracking** - See which notifications were acted on and by which rule, with per-rule hit counters
- **Notification Restore** - Re-post a processed notification back to the notification shade from History
- **TTS Reading** - Read notifications aloud through a lazy-loaded TTS engine (Chinese-first locale fallback), with templates supporting `{app}` / `{title}` / `{text}` placeholders
- **Import/Export** - Back up and restore rules as versioned JSON files, backward compatible with older export formats
- **Built-in Crash Logs** - On-device crash log viewer for troubleshooting
- **Setup Wizard** - First-run flow guides you through notification access, battery optimization exemption, and OEM auto-start settings for Xiaomi, Huawei, OPPO, OnePlus, vivo, Samsung, and other vendors
- **Pure Black Dark Theme** - An OLED-friendly pure black dark theme following the Material Design 3 dark color scheme; the compact 56dp icon-only bottom navigation (long-press for labels) and a unified landscape layout (chart panel on the left, content on the right) round out the modern UI
- **Fully Offline** - No network permissions, no data collection, nothing leaves your device

## Requirements

- Android 7.0 (API 24) or higher
- Notification listener access (and, on Android 13+, notification posting permission for the blocking/silencing flow)

## Screenshots

*Screenshots will be added here.*

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install debug build on connected device
./gradlew installDebug
```

The release APK is minified with R8 and signed with the Notix release keystore. Signing credentials are read from
`local.properties` (keys `KEYSTORE_NOTIX_*`) or environment variables, and are never committed
to the repository.

The project uses Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.0.21, and targets
`compileSdk 36` / `minSdk 24` with Java 11 compatibility.

## Getting Started

1. Install the app and launch it — the setup wizard walks you through granting notification listener access
2. (Recommended) Exempt Notix from battery optimization and, on OEM devices, enable auto-start so the listener keeps running
3. View incoming notifications in the **History** tab, where the chart panel gives you an overview by day
4. Tap a notification to create a rule from it, or go to the **Rules** tab and start the visual wizard to build a custom rule (app → matching → conditions → actions)
5. Notifications acted on by rules appear in the **Blocked** tab — tap any entry to view details, edit the rule that handled it, or restore the notification
6. Use **Settings** to import/export rules, reset hit counters, clear history, or view crash logs

## Documentation

Detailed documentation for developers and contributors is available in the [`docs/`](docs/) directory:

- **[Architecture & Codebase Overview](docs/ARCHITECTURE.md)** - Project structure, data models, core services, storage layer, UI layer, data flow diagrams, navigation map, and class dependencies
- **[API Reference](docs/API_REFERENCE.md)** - Method-level reference for classes and composables
- **[Developer Guide](docs/DEVELOPER_GUIDE.md)** - Practical guide for adding features, screens, rules, storage, and running tests

See also [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines and [RELEASE_NOTES.md](RELEASE_NOTES.md) for the version history.

## License

MIT License - see [LICENSE](LICENSE) for details.

This project is a derivative of [DoNotNotify](https://github.com/anujja/DoNotNotify) by Anuj
(c) 2025, and includes Notix's own modifications (c) 2026. Both copyright notices are preserved
in the LICENSE file in accordance with the MIT License.
