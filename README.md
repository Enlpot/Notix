**English** | [简体中文](README.zh-CN.md)

# Notix

Notix is an Android app that filters and blocks unwanted notifications using customizable rules.

> **Derived from [DoNotNotify](https://github.com/anujja/DoNotNotify)** by Anuj (MIT License). Notix is a rebranded and repackaged fork with its own release channel, package name, and signing key.

## Features

- **Notification Blocking** - Block notifications in real-time using Android's NotificationListenerService
- **Flexible Rules** - Create denylist (block matching), allowlist (allow only matching), or stack rules
- **Notification Stacking** - STACK rules don't block; they collapse chatty apps into a single expandable notification group so you keep the notifications without the clutter
- **Pattern Matching** - Match notification title/text using simple contains or regex patterns
- **Time-Based Rules** - Schedule rules to activate only during specific time windows
- **Prebuilt Rules** - 40+ pre-configured rules for popular apps (e-commerce promos, social media, etc.)
- **Auto-Install Rules** - Automatically adds relevant prebuilt rules when you install supported apps
- **Notification History** - View all received notifications with configurable retention
- **Blocked History** - Track which notifications were blocked and by which rules
- **Import/Export** - Backup and restore your rules as JSON files
- **Fully Offline** - No network permissions, no data collection

## Requirements

- Android 7.0 (API 24) or higher
- Notification listener permission

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

The release APK is signed with the Notix release keystore. Signing credentials are read from
`local.properties` (keys `KEYSTORE_NOTIX_*`) or environment variables, and are never committed
to the repository.

## How It Works

1. Grant notification listener permission when prompted
2. View incoming notifications in the **History** tab
3. Press a notification to create a blocking rule, or go to **Rules** tab to manually create rules
4. Blocked notifications appear in the **Blocked** tab
5. Tap any blocked notification to view details or edit the rule that blocked it

## Documentation

Detailed documentation for developers and contributors is available in the [`docs/`](docs/) directory:

- **[Architecture & Codebase Overview](docs/ARCHITECTURE.md)** - Project structure, data models, core services, storage layer, UI layer, data flow diagrams, navigation map, and class dependencies
- **[API Reference](docs/API_REFERENCE.md)** - Method-level reference for every class and composable
- **[Developer Guide](docs/DEVELOPER_GUIDE.md)** - Practical guide for adding features, screens, rules, storage, and running tests

## License

MIT License - see [LICENSE](LICENSE) for details.

This project is a derivative of [DoNotNotify](https://github.com/anujja/DoNotNotify) by Anuj
(c) 2025, and includes Notix's own modifications (c) 2026. Both copyright notices are preserved
in the LICENSE file in accordance with the MIT License.
