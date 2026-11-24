# Daymark build and packaging guide

This repository contains the Windows WPF source, Android source, shared schema/assets,
and the Windows installer script. Release binaries, keystores, databases, backups and
credentials are intentionally not tracked.

## Prerequisites

- Windows 10/11 x64 with .NET 8 SDK for Windows builds.
- Inno Setup 7.1.1 (`ISCC.exe`) for the installer.
- Android SDK, JDK 17 and Gradle 8.x for Android builds.
- The existing Daymark release keystore for signed Android builds. It must be kept
  outside the repository.

## Windows

Build and verify:

```powershell
dotnet build src\Windows\Daymark\Daymark.csproj -c Release
```

Publish a self-contained `win-x64` build:

```powershell
dotnet publish src\Windows\Daymark\Daymark.csproj `
  -c Release -r win-x64 --self-contained true `
  -p:PublishSingleFile=false `
  -o Releases\V2_1\Windows\publish
```

Create the sanitized staging folder used by both the installer and portable ZIP. Exclude
PDB files and never copy databases, backups, keys, logs or local configuration:

```powershell
# Copy files from publish to Releases\V2_1\Windows\stage, excluding *.pdb.
```

Build the portable ZIP from `stage\`, with a `PORTABLE_README.md` describing launch and
the shared data location.

Compile the installer after staging:

```powershell
# With Inno Setup 7.1.1 available as ISCC.exe:
ISCC packaging\windows\Daymark-v2.1.0.iss
```

The installer is per-user (`PrivilegesRequired=lowest`), allows a custom directory,
creates a Start Menu shortcut and an optional desktop shortcut, registers with Installed
Apps and uses a stable AppId. It intentionally has no uninstall rule for user data.

## Android

The Gradle build reads signing settings from the environment. Do not place passwords or
keystore paths in tracked files.

```powershell
$env:DAYMARK_KEYSTORE = '<path to existing release keystore>'
$env:DAYMARK_STORE_PASSWORD = '<store password>'
$env:DAYMARK_KEY_PASSWORD = '<key password>'
$env:DAYMARK_KEY_ALIAS = 'daymark'   # optional; this is the default

./gradlew assembleRelease
```

`DAYMARK_BUILD_DIR` may be set to redirect Gradle output when the project directory is
read-only or synced by other software; it is optional.

Verify the APK with Android build tools:

```powershell
apksigner verify --print-certs path\to\Daymark.apk
zipalign -c 4 path\to\Daymark.apk
```

Expected release metadata: package `com.daymark.app`, versionName `2.1.0`, versionCode
`3`, minSdk 24, targetSdk 35, zero permissions, schema version 2, and the existing
Daymark signer certificate.

## Data safety and tests

Release candidates are checked with disposable SQLite databases and disposable install
folders. Real user databases and backups must never be opened, moved, deleted or modified
by packaging tests.

Windows checks cover fresh database initialization, V1-to-V2 migration, row preservation,
verified backup creation, repeat launch idempotency, backup-failure abort behavior and
unchanged source bytes after an aborted migration.

## Repository exclusions

Never commit:

- `*.jks`, `*.keystore`, `*.pfx`, `*.p12`, `*.key`
- `local.properties`
- credentials, tokens or passwords
- `*.db`, `*.db-wal`, `*.db-shm`, `*.sqlite`, backups
- build outputs, logs, PDB files, temporary test files
- compiled release binaries or release packages

See `.gitignore`.
