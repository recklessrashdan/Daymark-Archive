<div align="center">

# Daymark

### Your Daily Life, Organised.

A calm, focused space for your tasks, plans and thoughts — beautifully simple, completely offline, and always yours.

<br>

[Download for Windows](https://github.com/recklessrashdan/Daymark/releases/download/v2.0.0/Daymark-Windows-v2.0.0.exe) &nbsp;·&nbsp; [Download for Android](https://github.com/recklessrashdan/Daymark/releases/download/v2.0.0/Daymark-Android-v2.0.0.apk)

<sub>Version 2.0.0 &nbsp;·&nbsp; Windows & Android &nbsp;·&nbsp; Offline edition</sub>

</div>

---

<div align="center">

🟦 &nbsp; **Plan clearly. Live calmly.** &nbsp; 🟩

</div>

---

## Download Daymark

Choose the build for your device. Version 2.0.0 is the current stable release. All Daymark downloads are available from the [GitHub Releases page](https://github.com/recklessrashdan/Daymark/releases).

### Windows

| | |
|---|---|
| **File** | [`Daymark-Windows-v2.0.0.exe`](https://github.com/recklessrashdan/Daymark/releases/download/v2.0.0/Daymark-Windows-v2.0.0.exe) |
| **Platform** | Windows 10 / 11 (x64) |
| **Setup** | None — self-contained single-file executable |
| **SHA-256** | `919C6E246E5A4019463EF84C24AA3183008F02807476EB0745B7C6F172B45A6B` |

Download the executable and double-click it to start Daymark. No installer, .NET runtime installation or configuration is required.

> The Windows executable is **unsigned**, so Windows SmartScreen may show a warning the first time you run it. Verify the download with `SHA256SUMS.txt` before continuing.

### Android

| | |
|---|---|
| **File** | [`Daymark-Android-v2.0.0.apk`](https://github.com/recklessrashdan/Daymark/releases/download/v2.0.0/Daymark-Android-v2.0.0.apk) |
| **Platform** | Android 7.0 (API 24) or newer |
| **Package** | `com.daymark.app` |
| **Version** | 2.0.0 (version code 2) |
| **Setup** | Install the signed APK directly |
| **SHA-256** | `5EF2BF6DC6F9E1A64F3C032E6E189F9761A8DABF2183B2C6F54C5C80678B2BA5` |

Download the APK to your device, open it, allow installation from that source if Android asks, and tap **Install**.

The APK is signed with the original Daymark release certificate, so it updates the same app identity used by V1. It requests **zero permissions**, including no Internet permission.

### Previous release

V1.0.0 remains available and unchanged in the [V1.0.0 release](https://github.com/recklessrashdan/Daymark/releases/tag/v1.0.0).

---

## What's new in V2

- A refreshed Daymark identity with Midnight Navy, Porcelain, Electric Teal and Warm Amber, including a new horizon-mark logo and launcher icons.
- A redesigned Today dashboard with daily overview, real progress, priority focus, deadlines, overdue count, note access and quick add.
- A stronger task library with categories, pinning, priority, due dates, search, filters, sorting, completed organization and undo delete.
- A polished monthly calendar on both platforms with day indicators, date selection, selected-day task lists and create-for-day. Windows also includes a functional week view.
- Daily notes with lossless autosave, previous-day library, note search and pinning.
- Light, Dark and System themes that persist across restarts.
- Verified pre-upgrade backups and a safe, additive V1-to-V2 migration.

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

## Upgrading from V1

Daymark V2 preserves V1 data. On first launch after upgrading, it creates a timestamped backup in an app-private backups folder before changing the database schema.

The migration adds fields and indexes with safe defaults. Existing tasks, notes, settings and dates are kept. No V1 table is dropped or rebuilt.

Both editions verify the backup before migration starts. If the backup cannot be created or verified, the upgrade stops before any schema change and shows a clear message. A previously verified backup is reused on retry; existing backups are not deleted.

The Android safety path was implemented and checked on disposable database copies, but physical-device and emulator runtime testing were not performed for this release.

---

## System requirements

| | Windows | Android |
|---|---|---|
| **Availability** | Available | Available |
| **Version** | 2.0.0 | 2.0.0 |
| **Minimum** | Windows 10 / 11 (x64) | Android 7.0 (API 24) |
| **Installation** | Run the self-contained executable | Install the signed APK |
| **Storage** | Space for the application and local database | Space for the APK and local database |
| **Connection** | Not required | Not required |

---

## Verification and transparency

We believe in being clear about what has and has not been verified.

- **Windows:** the release executable was launched and smoke-tested, including navigation, quick add, notes, settings and the month/week calendar switch.
- **Migration:** 16/16 offline checks passed on a copied V1 database and a representative V1 fixture, including idempotency and data preservation.
- **Backup safety:** 76/76 disposable-copy checks passed for backup failure, verification failure, migration failure, repeat launch and existing V2 installations.
- **Android:** the release build, signature, certificate identity, alignment, package metadata and zero-permission manifest were verified.
- **Android runtime:** physical-device and emulator runtime testing were **not performed** because neither was available. Android behaviour is build-verified and logic-tested, but not runtime-verified in this release.
- **Windows signing:** the executable is unsigned, so a SmartScreen warning may appear.

---

## Version information

| | |
|---|---|
| **Product** | Daymark |
| **Version** | 2.0.0 |
| **Publisher** | Recklessplyer |
| **Edition** | Offline |
| **License** | MIT — see `LICENSE`. |

## Links

- [All releases](https://github.com/recklessrashdan/Daymark/releases)
- [V2.0.0 release](https://github.com/recklessrashdan/Daymark/releases/tag/v2.0.0)
- [V1.0.0 release](https://github.com/recklessrashdan/Daymark/releases/tag/v1.0.0)

---

<div align="center">

**Daymark** &nbsp;·&nbsp; Your Daily Life, Organised.

Published by **Recklessplyer** &nbsp;·&nbsp; © 2026 Recklessplyer

</div>
