# Daymark release notes

## Daymark v2.1.0 — Major Update

**Your Daily Life, Organised.**

Status: **Stable release**

Daymark v2.1.0 improves reliability, note handling and Windows distribution while keeping the private, local-first design of earlier releases.

### What's new in 2.1.0

- **Windows installer** — per-user Inno Setup installer with a selectable directory, Start Menu shortcut, optional desktop shortcut, Installed Apps registration and standard uninstaller. A stable AppId supports future upgrades.
- **Windows portable edition** — extract-and-run ZIP built from the same self-contained publish output. Both Windows editions share `%LOCALAPPDATA%\Daymark\daymark.db`.
- **Safer Windows migration** — a verified backup is required only when a Daymark schema exists; migration-safety failures show the specific reason; backup verification no longer holds a pooled file handle; backup-directory failures are wrapped safely.
- **Note editing fix** — pending note text is flushed to the previously displayed date before switching dates.
- **Progress ring fix** — a complete circle is rendered at 100%.
- **Android fixes** — pinned notes survive edit/autosave, migration-failure messaging is more precise, and clear-all cancels stale undo state.
- Version metadata is now 2.1.0 (versionCode 3) on Android and 2.1.0 on Windows. SQLite schema remains version 2.

### Downloads

| Platform | File |
|---|---|
| Windows installer | `Daymark-Setup-v2.1.0.exe` |
| Windows portable | `Daymark-Portable-v2.1.0.zip` |
| Android | `Daymark-Android-v2.1.0.apk` |

All three files are attached to the [v2.1.0 release](https://github.com/recklessrashdan/Daymark/releases/tag/v2.1.0), together with `SHA256SUMS.txt`.

### Checksums (SHA-256)

| File | SHA-256 |
|---|---|
| `Daymark-Setup-v2.1.0.exe` | `51EF3A1F0CA14AE7A761A93EC43938B6410EAC9A203EB43D5E80D92EE9288127` |
| `Daymark-Portable-v2.1.0.zip` | `E02FBD0748974EC2ACCCF20EEF4C60510336FF823D3D88F72F4A591D98A96BB3` |
| `Daymark-Android-v2.1.0.apk` | `D0AE47C6075C4DD4F8461DA837917DD26A6CFFB0878D76AF5E19CAB6307754D6` |

### Verification status

| Check | Status |
|---|---|
| Windows build and self-contained publish | PASS |
| Inno Setup compile and portable ZIP build | PASS |
| Windows installer file/shortcut/registration/uninstall checks | PASS |
| Windows data-safety regression harness | PASS — 16/16 |
| Windows application launch from installed/portable builds | **NOT TESTED** |
| Android release build, signature, certificate identity, alignment and metadata | PASS |
| Android zero-permission manifest and schema version 2 | PASS |
| Android device/emulator runtime | **NOT TESTED** |
| Windows code signing | **NOT APPLIED** — unsigned; SmartScreen may warn |

### Privacy

Daymark remains offline by design: no account, cloud sync, telemetry, analytics, tracking, ads or Internet permission on Android. Tasks and notes are stored locally.

### Known limitations

- Windows app-launch scenarios and Android device/emulator runtime scenarios were not tested in the build environment.
- Windows executables are unsigned, so a SmartScreen warning may appear.
- Data is local to each device; there is no synchronisation between Windows and Android installs.

---

## Daymark v2.0.0 — Major Update

**Your Daily Life, Organised.**

Release date: 2026-11-24  
Status: **Stable release**

Daymark v2.0.0 is the second public release of the offline daily planner for Windows and Android. It keeps the private, local-first promise of V1 while adding a refreshed identity, a richer dashboard, stronger task tools, better notes and safer V1-to-V2 data migration.

### What's new in 2.0.0

- **New Daymark identity** — Midnight Navy, Porcelain, Electric Teal and Warm Amber across Windows and Android, with a new horizon-mark logo and launcher icons.
- **Redesigned Today dashboard** — date and greeting, daily overview, circular progress, priority focus, deadlines and overdue counts, notes access, quick add and a real completion-activity strip. V1 completions are not given invented timestamps.
- **Stronger task library** — create, edit, complete and delete tasks with Low/Medium/High priority, optional due dates, free-text categories and pinning. Search, smart filters, sorting, overdue indicators, completed organization, details and undo delete are included.
- **Polished calendar** — month view on both platforms with day indicators, date selection, a selected-day task list and create-for-day. Windows also includes a functional week view.
- **Better daily notes** — lossless autosave, a previous-day library, note search and pinning. Notes are flushed before leaving the notes screen so the last keystrokes are not lost.
- **Appearance settings** — Light, Dark and System themes persist across restarts.
- **Safer data controls** — timestamped pre-upgrade backup, local backup copy, storage and about information, and a guarded clear-all action.
- **Safe V1 upgrade** — additive, transactional and idempotent migration. Existing tasks, notes, settings and dates are preserved; no V1 table is dropped or rebuilt.

### Downloads

| Platform | File |
|---|---|
| Windows | `Daymark-Windows-v2.0.0.exe` |
| Android | `Daymark-Android-v2.0.0.apk` |

Both files are attached to the [v2.0.0 release](https://github.com/recklessrashdan/Daymark/releases/tag/v2.0.0), together with `SHA256SUMS.txt`.

#### Windows

The EXE is a self-contained, single-file WPF application for Windows 10/11 x64. No .NET runtime installation is needed. Double-click the file to run it.

The Windows executable is **unsigned**, so Windows SmartScreen may warn on first run. Verify the checksum before continuing.

#### Android

The APK is signed with the original Daymark release certificate, so it updates the same app identity as V1. Android 7.0 (API 24) or newer is required. Install the APK directly and allow installation from your chosen source if Android asks.

The APK requests **no permissions**, including no Internet permission.

### Checksums (SHA-256)

| File | SHA-256 |
|---|---|
| `Daymark-Windows-v2.0.0.exe` | `919C6E246E5A4019463EF84C24AA3183008F02807476EB0745B7C6F172B45A6B` |
| `Daymark-Android-v2.0.0.apk` | `5EF2BF6DC6F9E1A64F3C032E6E189F9761A8DABF2183B2C6F54C5C80678B2BA5` |

### Upgrading from V1

On first V2 launch, Daymark backs up the local database before applying the schema upgrade. The backup is written to an app-private backups folder on each platform.

The migration adds columns and indexes with safe defaults, records schema version 2 and preserves V1 data. Both editions now verify the backup before changing the schema. Verification includes integrity, required tables, row counts and full row-content comparison.

If the backup cannot be created or verified, migration is stopped before any schema change and a clear message is shown. A previously verified backup is reused on retry; existing backups are not deleted. Do not force-stop or uninstall during the first V2 launch.

Android's safety path is implemented and logic-tested on disposable copies, but was not runtime-tested on a physical device or emulator.

### Verification status

| Check | Status |
|---|---|
| Windows build | PASS — 0 warnings, 0 errors |
| Windows release EXE launch and smoke test | PASS |
| V1-to-V2 migration checks | PASS — 16/16 offline checks |
| Verified-backup safety suite | PASS — 76/76 disposable-copy checks |
| Android release build | PASS |
| Android signature and certificate identity | PASS |
| Android alignment, package metadata and permissions | PASS |
| Android on-device install and runtime test | **NOT TESTED** — no device/emulator available |
| Windows code signing | **NOT APPLIED** — executable is unsigned |

### Privacy

Daymark remains offline by design:

- no account or sign-in
- no cloud sync
- no telemetry, analytics, tracking or ads
- no network/HTTP client code in the app
- no Internet permission on Android
- all tasks and notes stored locally on the device

### Known limitations

- Android physical-device and emulator runtime testing have not been completed.
- Android is distributed as a directly installed APK, not through an app store.
- The Windows executable is not Authenticode-signed, so operating-system SmartScreen may show a first-run warning.
- Data is local to each device. There is no synchronisation between Windows and Android installs.
- Removing an app removes its locally stored data.

### License

MIT License. See `LICENSE`.

Published by **Recklessplyer** · © 2026 Recklessplyer

---

## Daymark v1.0.0

**Your Daily Life, Organised.**

Release date: 2026-11-23

Daymark v1.0.0 is the first public release of a premium, minimal daily planner for Windows and Android. It is built to stay out of your way: everything is stored locally, and there is no account, cloud service or subscription.

The V1.0.0 release remains available and unchanged.

### Highlights

- **Today dashboard** — daily progress, quick task entry and a high-priority focus list.
- **Task management** — full create/edit/delete with priorities, due dates, search, filters and undo delete.
- **Calendar** — month view with per-day task dots and a selected-day task list.
- **Daily notes** — one note per day with automatic saving and a history of previous days.
- **Themes** — Light, Dark and System appearance options.
- **Private by design** — local-only storage. Nothing is uploaded.

### Downloads

| Platform | File |
|---|---|
| Windows | `Daymark-Windows-v1.0.0.exe` |
| Android | `Daymark-Android-v1.0.0.apk` |

Checksums for both files are published in `SHA256SUMS.txt`.

### Checksums (SHA-256)

| File | SHA-256 |
|---|---|
| `Daymark-Windows-v1.0.0.exe` | `EE281E303D99A395BC95B2B9607DB8C3DB574F2994145CC9D953F5D49C55CC21` |
| `Daymark-Android-v1.0.0.apk` | `7CC589FA6A228861444835D27637C996B3D52BF605BDFF627C3B3BE5A44AD919` |

### Verification status

- **Windows:** the release build passed **16/16 automated functional tests**, covering task creation and completion, search and filters, undo delete, notes autosave, calendar navigation, theme persistence, settings and data survival across a restart. Launch of the standalone executable was verified.
- **Android:** the release build passed compilation, release lint, APK signature verification, manifest inspection and checksum verification. It has **not yet been run on a physical device or emulator**; physical-device runtime testing is still required before broad distribution.

### Known limitations

- Android has not yet received on-device runtime testing (see above).
- Android is distributed as a directly installed APK, not through an app store.
- The Windows executable is not Authenticode-signed, so operating-system SmartScreen may show a first-run warning; verify the published checksum before running.
- Data is local to each device. There is no synchronisation between Windows and Android installs.
- Removing an app removes its locally stored data.

### Attribution

Daymark is designed and published by **Recklessplyer**. Released under the MIT License.
