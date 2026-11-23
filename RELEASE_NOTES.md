# Daymark v1.0.0

**Your Daily Life, Organised.**

Release date: 2026-11-23

Daymark v1.0.0 is the first public release of a premium, minimal daily planner for Windows and Android. It is built to stay out of your way: everything is stored locally, and there is no account, cloud service or subscription.

## Highlights

- **Today dashboard** — daily progress, quick task entry and a high-priority focus list.
- **Task management** — full create/edit/delete with priorities, due dates, search, filters and undo delete.
- **Calendar** — month view with per-day task dots and a selected-day task list.
- **Daily notes** — one note per day with automatic saving and a history of previous days.
- **Themes** — Light, Dark and System appearance options.
- **Private by design** — local-only storage. Nothing is uploaded.

## Downloads

| Platform | File |
|---|---|
| Windows | `Daymark-Windows-v1.0.0.exe` |
| Android | `Daymark-Android-v1.0.0.apk` |

Checksums for both files are published in `SHA256SUMS.txt`.

## Checksums (SHA-256)

| File | SHA-256 |
|---|---|
| `Daymark-Windows-v1.0.0.exe` | `EE281E303D99A395BC95B2B9607DB8C3DB574F2994145CC9D953F5D49C55CC21` |
| `Daymark-Android-v1.0.0.apk` | `7CC589FA6A228861444835D27637C996B3D52BF605BDFF627C3B3BE5A44AD919` |

## Verification status

- **Windows:** the release build passed **16/16 automated functional tests**, covering task creation and completion, search and filters, undo delete, notes autosave, calendar navigation, theme persistence, settings and data survival across a restart. Launch of the standalone executable was verified.
- **Android:** the release build passed compilation, release lint, APK signature verification, manifest inspection and checksum verification. It has **not yet been run on a physical device or emulator**; physical-device runtime testing is still required before broad distribution.

## Known limitations

- Android has not yet received on-device runtime testing (see above).
- Android is distributed as a directly installed APK, not through an app store.
- The Windows executable is not Authenticode-signed, so operating-system SmartScreen may show a first-run warning; verify the published checksum before running.
- Data is local to each device. There is no synchronisation between Windows and Android installs.
- Removing an app removes its locally stored data.

## Attribution

Daymark is designed and published by **Recklessplyer**. Released under the MIT License.
