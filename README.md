<div align="center">

# Daymark

### Your Daily Life, Organised.

A calm, focused space for your tasks, plans and thoughts — beautifully simple, completely offline, and always yours.

<br>

[Download for Windows](https://github.com/recklessrashdan/Daymark/releases/download/v2.1.0/Daymark-Setup-v2.1.0.exe) &nbsp;·&nbsp; [Windows Portable](https://github.com/recklessrashdan/Daymark/releases/download/v2.1.0/Daymark-Portable-v2.1.0.zip) &nbsp;·&nbsp; [Download for Android](https://github.com/recklessrashdan/Daymark/releases/download/v2.1.0/Daymark-Android-v2.1.0.apk)

<sub>Version 2.1.0 &nbsp;·&nbsp; Windows & Android &nbsp;·&nbsp; Offline edition</sub>

</div>

---

<div align="center">

🟦 &nbsp; **Plan clearly. Live calmly.** &nbsp; 🟩

</div>

---

## Download Daymark

Version 2.1.0 is the current stable release. All downloads are available from the [GitHub Releases page](https://github.com/recklessrashdan/Daymark/releases).

### Windows installer

| | |
|---|---|
| **File** | [`Daymark-Setup-v2.1.0.exe`](https://github.com/recklessrashdan/Daymark/releases/download/v2.1.0/Daymark-Setup-v2.1.0.exe) |
| **Platform** | Windows 10 / 11 (x64) |
| **Setup** | Per-user installer; no administrator rights required |
| **SHA-256** | `51EF3A1F0CA14AE7A761A93EC43938B6410EAC9A203EB43D5E80D92EE9288127` |

Run the installer and choose an install directory if desired. A Start Menu shortcut is created; the desktop shortcut is optional. Daymark is registered in Installed Apps with a standard uninstaller. Uninstalling removes program files, shortcuts and registration, but intentionally leaves your database and backups in place.

### Windows portable

| | |
|---|---|
| **File** | [`Daymark-Portable-v2.1.0.zip`](https://github.com/recklessrashdan/Daymark/releases/download/v2.1.0/Daymark-Portable-v2.1.0.zip) |
| **Platform** | Windows 10 / 11 (x64) |
| **Setup** | Extract and run `Daymark.exe` |
| **SHA-256** | `E02FBD0748974EC2ACCCF20EEF4C60510336FF823D3D88F72F4A591D98A96BB3` |

The portable edition uses the same local data location as the installed edition on the same Windows account: `%LOCALAPPDATA%\Daymark\daymark.db`, with backups in `%LOCALAPPDATA%\Daymark\backups\`. It does not create a database beside the executable.

### Android

| | |
|---|---|
| **File** | [`Daymark-Android-v2.1.0.apk`](https://github.com/recklessrashdan/Daymark/releases/download/v2.1.0/Daymark-Android-v2.1.0.apk) |
| **Platform** | Android 7.0 (API 24) or newer |
| **Package** | `com.daymark.app` |
| **Version** | 2.1.0 (version code 3) |
| **Setup** | Install the signed APK directly |
| **SHA-256** | `D0AE47C6075C4DD4F8461DA837917DD26A6CFFB0878D76AF5E19CAB6307754D6` |

The APK is signed with the original Daymark release certificate, so it updates the same app identity used by V1 and V2. It requests **zero permissions**, including no Internet permission.

### Previous releases

V1.0.0 and V2.0.0 remain available and unchanged:

- [V2.0.0 release](https://github.com/recklessrashdan/Daymark/releases/tag/v2.0.0)
- [V1.0.0 release](https://github.com/recklessrashdan/Daymark/releases/tag/v1.0.0)

---

## What's new in V2.1.0

- Windows installer and portable Windows edition built from the same self-contained `win-x64` publish output.
- Safer Windows fresh-install and migration behavior: a verified pre-upgrade backup is required only when an existing Daymark schema is present, and migration-safety failures show a specific reason.
- Fixed Windows note text loss when changing the displayed note date.
- Fixed the Windows progress ring at 100%.
- Fixed Android pinned-note persistence through edit/autosave, migration-failure messaging and stale clear-all undo state.
- Version metadata updated to 2.1.0 on both platforms. SQLite schema remains version 2.

---

## Features

| | |
|---|---|
| **Today dashboard** | A daily greeting, real progress, today's tasks and a priority focus list. |
| **Task management** | Create, edit, complete and delete tasks, with search, filters, sorting and undo. |
| **Priorities & due dates** | Give tasks Low, Medium or High priority and an optional due date. |
| **Calendar** | Month view with task indicators and selectable days; week view on Windows. |
| **Daily notes** | One note for every day, saved automatically, with a searchable history. |
| **Themes** | Light, Dark and System appearance options. |
| **Offline storage** | Tasks and notes are kept in a local SQLite database on your device. |
| **No account or subscription** | Nothing to sign up for, no cloud service and no subscription. |

---

## Privacy first

Daymark is built to keep your day to itself.

- **Works offline** — every feature is available without a network connection.
- **No account required** — there is nothing to register for and nothing to remember.
- **No cloud synchronisation** — your tasks and notes are not sent anywhere.
- **No telemetry, analytics, tracking or ads**.
- **No Internet permission** on Android.
- **Local by design** — tasks and notes live in a local SQLite database on each device.

Device-level backup behaviour may depend on your operating system and settings.

---

## Upgrading

Daymark V2 preserves V1 data. On first launch after upgrading, it creates a timestamped backup in an app-private backups folder before changing the database schema.

The migration adds fields and indexes with safe defaults. Existing tasks, notes, settings and dates are kept. No V1 table is dropped or rebuilt. If a backup cannot be created or verified, the upgrade stops before any schema change and shows a clear message. A previously verified backup is reused on retry; existing backups are not deleted.

---

## System requirements

| | Windows | Android |
|---|---|---|
| **Version** | 2.1.0 | 2.1.0 |
| **Minimum** | Windows 10 / 11 (x64) | Android 7.0 (API 24) |
| **Installation** | Installer or portable ZIP | Install the signed APK |
| **Connection** | Not required | Not required |

---

## Verification and transparency

We believe in being clear about what has and has not been verified.

- **Builds:** Windows self-contained publish, Inno Setup installer, portable ZIP and signed Android APK all built successfully.
- **Windows data safety:** 16/16 disposable-database checks passed, including fresh install, V1 migration, row preservation, verified backup, repeat launch and aborted migration with unchanged source bytes.
- **Windows packaging:** silent install, reinstall, shortcut/registration creation, uninstall and cleanup checks passed under a non-admin account. Real user data was hash-verified unchanged.
- **Windows application launch:** **NOT TESTED** in the build environment because no disposable Windows user profile was available.
- **Android:** release build, signature, certificate identity, alignment, package metadata, version metadata, zero permissions and disposable SQL/logic checks passed.
- **Android device/emulator runtime:** **NOT TESTED** — no device or emulator was available.
- **Windows signing:** Windows executables are unsigned, so Windows SmartScreen may show a warning. Verify the published checksum before running.

---

## Version information

| | |
|---|---|
| **Product** | Daymark |
| **Version** | 2.1.0 |
| **Publisher** | Recklessplyer |
| **Edition** | Offline |
| **License** | MIT — see `LICENSE`. |

## Links

- [Build and packaging guide](docs/BUILD_AND_PACKAGING.md)
- [All releases](https://github.com/recklessrashdan/Daymark/releases)
- [V2.1.0 release](https://github.com/recklessrashdan/Daymark/releases/tag/v2.1.0)
- [V2.0.0 release](https://github.com/recklessrashdan/Daymark/releases/tag/v2.0.0)
- [V1.0.0 release](https://github.com/recklessrashdan/Daymark/releases/tag/v1.0.0)

---

<div align="center">

**Daymark** &nbsp;·&nbsp; Your Daily Life, Organised.

Published by **Recklessplyer** &nbsp;·&nbsp; © 2026 Recklessplyer

</div>
