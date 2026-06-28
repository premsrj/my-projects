# Food Tracker (Android)

Offline-first Android food tracker built with Jetpack Compose + Room.

## Features
- Local Room database seeded from CSV asset on first launch.
- Home dashboard with date navigation and donut charts.
- Optional daily targets: calories, protein, fat, carbs, fiber.
- Calorie card prioritized with larger visual space.
- Meals screen with fixed meals:
  - Breakfast
  - Morning Snack
  - Lunch
  - Evening Snack
  - Dinner
- Meal-level calorie target editing with live allocated-vs-daily counter.
- Food search with tokenized wildcard matching and relevance ranking.
- Add/Edit foods (including preloaded foods).
- Track quantity for selected food with computed nutrition totals.
- Full offline tracking by date.

## Tech Stack
- Kotlin
- Jetpack Compose (Material 3)
- Navigation Compose
- Room (SQLite)

## Build
Windows:
- gradlew.bat assembleDebug

## Run on Device (ADB)
- adb install -r app\\build\\outputs\\apk\\debug\\app-debug.apk
- adb shell am start -n dev.prem.foodtracker/.MainActivity
