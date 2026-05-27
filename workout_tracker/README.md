# Workout Tracker

Workout Tracker is an Android gym log app focused on quick set entry and exercise progression.

## Implemented Features

- Workout types:
  - Weight and reps
  - Reps only
  - Weight and time
  - Time only
  - Distance and time
  - Distance only
- Default categories:
  - back, biceps, calves, cardio, chest, core, forearms, lats, legs, shoulder, triceps
- Custom categories
- Custom exercises with name, description, type, and category
- Home screen starts on Today and shows all exercises tracked today
- Top app bar action on Today to start tracking
- Exercise picker grouped by category with search
- Track Exercise page:
  - Dynamic entry fields based on selected workout type
  - Track set button
  - Optional set comment while logging
  - Existing set comment indicator with edit/delete/cancel dialog
  - Weight field flanked by decrement/increment buttons
  - Manual weight input with two decimal support
  - Per-exercise configurable weight increment value
  - Last performed snapshot:
    - Date
    - Exercise order in that workout
    - Full set details from that workout
- Right-swipe flow from Track page:
  - Swipe right once: full exercise history
  - Swipe right again: exercise statistics
- Statistics page:
  - Last-session 1RM with date (for weighted rep-based exercises)
  - All-time 1RM with date
  - Line chart with metric switch:
    - 1RM
    - Volume
    - Max weight

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Room (SQLite)
- MVVM with ViewModel + StateFlow

## Build

From workout_tracker:

```bash
./gradlew assembleDebug
```

Install and launch on a connected device:

```bash
adb devices -l
adb -s <deviceId> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <deviceId> shell monkey -p com.example.workouttracker -c android.intent.category.LAUNCHER 1
```
