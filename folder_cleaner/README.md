# Folder Cleaner

Folder Cleaner is a modern Android utility app that deletes old files from user-selected folders while protecting excluded file types.

The app supports:
- Manual cleanup from the UI
- Automatic cleanup with WorkManager scheduling
- Folder include list with per-folder enable/disable switches
- File extension ignore list
- Run history with reclaimed-size analytics and trend chart
- Backup export/import for device migration

## What The App Does

Folder Cleaner helps users reclaim storage safely by applying rules instead of deleting everything.

Core behavior:
- User sets `Days to keep file`
- User chooses folders to include in cleanup
- User sets file extensions to ignore (example: `.pdf`, `.nomedia`)
- Cleanup deletes only files older than the configured threshold
- Cleanup only scans direct children of selected folders (no recursive subfolder scan)
- Disabled folders are skipped

## Primary Use Cases

- Keep Downloads or media cache folders trimmed automatically
- Remove old temporary files while preserving important document/media extensions
- Run daily maintenance only when the phone is charging and optionally idle
- Migrate setup between devices using backup JSON

## Platform Targets

- Compile SDK: 35
- Target SDK: 35
- Min SDK: 32
- Language: Kotlin
- UI: Jetpack Compose + Material 3

## Key Features

### 1) Cleanup Configuration
- Numeric `Days to keep file` setting persisted with DataStore
- Folder selection via Storage Access Framework (OpenDocumentTree)
- Extension exclusion list with add/remove controls

### 2) Manual Cleanup
- `Clean Up` button runs cleanup logic immediately (not queued)
- Completion notification includes deleted count and reclaimed size

### 3) Automatic Cleanup
- WorkManager periodic work
- Schedule configurable in-app:
  - Interval in days (minimum 1)
  - Require charging (toggle)
  - Require device idle (toggle)
- Scheduling is applied every app start using latest saved schedule

### 4) Run History + Analytics
- Stores latest 100 runs only
- Each run stores:
  - timestamp
  - trigger (manual/automatic)
  - scanned/deleted/skipped/failed counts
  - reclaimed bytes
- History screen includes:
  - total reclaimed storage summary
  - per-run reclaimed storage
  - trend line chart (recent runs)

### 5) Backup And Migration
- Export JSON backup containing:
  - days to keep
  - schedule settings
  - selected folders (uri/displayName/isEnabled)
  - ignored file extensions
- Import backup restores all of the above
- On new devices, user may need to re-grant folder URI permissions

## How To Use

1. Open app.
2. Set `Days to keep file`.
3. Open `Folders to clean` and add folders.
4. Open `File types to ignore` and add extensions.
5. Optional: open `Schedule controls` and set interval/constraints.
6. Optional: open `Backup and migration` to export configuration.
7. Tap `Clean Up` to run manual cleanup immediately.
8. Open `Run history` to review outcomes and reclaimed storage trend.

## Cleanup Algorithm

For each enabled selected folder:
1. Resolve folder tree URI using `DocumentFile.fromTreeUri`.
2. List only direct child entries.
3. Ignore non-file entries.
4. Ignore files matching excluded extensions.
5. Ignore files with invalid timestamp or newer than cutoff.
6. Delete eligible files.
7. Track counters and total reclaimed bytes.
8. Save run record and prune history to latest 100 records.

## Permissions And Storage Model

### Declared permissions
- `READ_EXTERNAL_STORAGE` (maxSdkVersion=32)
- `WRITE_EXTERNAL_STORAGE` (maxSdkVersion=32)
- `MANAGE_EXTERNAL_STORAGE`
- `POST_NOTIFICATIONS`

### Runtime behavior
- App guides user to grant all-files access where needed
- Folder selection uses SAF URI permissions (`takePersistableUriPermission`)
- Notifications use Android notification channel and runtime permission on Android 13+

## Architecture

The app uses a simple clean MVVM-style structure:

- UI layer (Compose screens + Navigation)
- ViewModel layer (state + actions)
- Repository layer (business logic)
- Persistence layer (Room + DataStore)
- Background layer (WorkManager)

### Why this architecture
- Keeps cleanup logic centralized in repository
- Keeps UI declarative and state-driven
- Supports both manual and worker-triggered cleanup through same logic
- Easy to evolve feature set (schedule controls, backup, analytics)

## Data Model

### Room tables

`selected_folders`
- `uri` (PK)
- `displayName`
- `isEnabled`

`ignored_extensions`
- `extension` (PK)

`cleanup_runs`
- `id` (PK autogen)
- `executedAtMillis`
- `trigger`
- `scannedCount`
- `deletedCount`
- `skippedCount`
- `failedCount`
- `reclaimedBytes`

### DataStore keys
- `days_to_keep`
- `schedule_interval_days`
- `schedule_requires_charging`
- `schedule_requires_idle`

## Work Scheduling Details

- Uses unique periodic work name to avoid duplicate workers.
- Existing periodic work is updated when schedule changes.
- Work constraints are derived from user schedule settings.
- Interval is bounded to minimum 1 day.

## Backup File Format (JSON)

Example structure:

```json
{
  "version": 1,
  "exportedAtMillis": 1716550000000,
  "settings": {
    "daysToKeep": 30,
    "schedule": {
      "intervalDays": 1,
      "requiresCharging": true,
      "requiresDeviceIdle": true
    }
  },
  "folders": [
    {
      "uri": "content://...",
      "displayName": "Download",
      "isEnabled": true
    }
  ],
  "ignoredExtensions": [".pdf", ".nomedia"]
}
```

## UI Screens

- Home
  - Days to keep
  - navigation to folders, ignored types, run history, schedule, backup
  - cleanup action
- Folders to clean
  - add folder picker
  - per-folder enable switch
  - remove folder
- File types to ignore
  - add extension dialog
  - remove extension
- Run history
  - run list
  - reclaimed size stats and trend chart
- Schedule controls
  - interval input
  - charging/idle toggles
- Backup and migration
  - export JSON
  - import JSON

## Platform Components Used

- Jetpack Compose (UI)
- Material 3 (theming, dynamic color)
- Navigation Compose
- Android ViewModel
- Room (SQLite persistence)
- DataStore Preferences
- WorkManager
- NotificationManager + NotificationChannel
- Storage Access Framework (`OpenDocumentTree`, `OpenDocument`, `CreateDocument`)
- `DocumentFile` for file operations on tree URIs

## Libraries/Tools

- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- KSP
- Room 2.6.1
- WorkManager 2.10.0
- DataStore 1.1.2
- Compose BOM 2025.02.00

## Build And Run

From `folder_cleaner`:

```bash
./gradlew assembleDebug
```

Install and launch on connected device:

```bash
adb devices -l
adb -s <deviceId> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <deviceId> shell monkey -p com.example.foldercleaner -c android.intent.category.LAUNCHER 1
```

## Project Structure (Important Files)

- `app/src/main/java/com/example/foldercleaner/data/CleanupRepository.kt`
- `app/src/main/java/com/example/foldercleaner/data/AppDatabase.kt`
- `app/src/main/java/com/example/foldercleaner/work/WorkScheduler.kt`
- `app/src/main/java/com/example/foldercleaner/work/CleanupWorker.kt`
- `app/src/main/java/com/example/foldercleaner/ui/navigation/FolderCleanerRoot.kt`
- `app/src/main/java/com/example/foldercleaner/ui/home/HomeScreen.kt`
- `app/src/main/java/com/example/foldercleaner/ui/history/RunHistoryScreen.kt`
- `app/src/main/java/com/example/foldercleaner/ui/schedule/ScheduleScreen.kt`
- `app/src/main/java/com/example/foldercleaner/ui/backup/BackupScreen.kt`

## Privacy And Safety Notes

- Cleanup runs only against user-selected folders.
- App does not upload user files.
- Backup export contains configuration metadata only.
- Deletion is permanent (no recycle bin in current implementation).

## Known Limitations

- File age is based on `lastModified` metadata (not guaranteed creation time).
- Folder URI permissions may need re-selection after migration/device change.
- Trend chart currently focuses on reclaimed bytes and recent points.

## Future Enhancements

- Dry-run preview mode before deletion
- Safe-delete quarantine mode with retention window
- Per-folder retention days
- CSV export of run history analytics
- Optional foreground service progress for very large folders

## License

Internal project. Add an explicit license file if distribution is planned.
