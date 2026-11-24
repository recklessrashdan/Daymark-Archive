; Daymark 2.1.0 — per-user Windows installer (Inno Setup 7.1.1)
; Compile with Inno Setup 7.1.1 (ISCC.exe).
; This script intentionally does NOT delete %LOCALAPPDATA%\Daymark on uninstall.

#define MyAppName "Daymark"
#define MyAppVersion "2.1.0"
#define MyAppPublisher "Recklessplyer"
#define MyAppExeName "Daymark.exe"
#define MyAppId "{{A9E3E4F2-7B18-4C6D-9A2F-5D8E1C0B7F31}"

[Setup]
AppId={#MyAppId}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL=https://github.com/recklessrashdan/Daymark
AppSupportURL=https://github.com/recklessrashdan/Daymark/issues
AppUpdatesURL=https://github.com/recklessrashdan/Daymark/releases
VersionInfoVersion=2.1.0.0
VersionInfoCompany={#MyAppPublisher}
VersionInfoDescription=Daymark — your daily life, organised (offline edition)
VersionInfoProductName={#MyAppName}
DefaultDirName={userpf}\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
DisableDirPage=no
UsePreviousAppDir=yes
PrivilegesRequired=lowest
UninstallDisplayIcon={app}\{#MyAppExeName}
UninstallDisplayName={#MyAppName} {#MyAppVersion}
SetupIconFile=..\..\src\Windows\Daymark\Assets\daymark.ico
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
OutputDir=..\..\Releases\V2_1\Windows
OutputBaseFilename=Daymark-Setup-v2.1.0
; No [UninstallDelete] entry: user data under %LOCALAPPDATA%\Daymark is preserved.

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional shortcuts:"; Flags: unchecked

[Files]
; Package the self-contained publish folder (PDB and development files excluded).
Source: "..\..\Releases\V2_1\Windows\stage\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs; Excludes: "*.pdb"

[Icons]
Name: "{autoprograms}\{#MyAppName}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Comment: "Launch Daymark"
Name: "{autoprograms}\{#MyAppName}\Uninstall {#MyAppName}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon; Comment: "Launch Daymark"

[Run]
; Deliberately no post-install launch: installation tests control app launch.
