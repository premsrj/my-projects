# Finance Calculators (Android)

A native Android app that converts your original web-based finance calculators into a Kotlin + Jetpack Compose experience.

## What This App Does

The app provides three calculators tailored to your financial planning workflow:

- Inflation Calculator: Projects value forward and backward in time using inflation assumptions.
- SWP Calculator: Simulates systematic withdrawals, debt/equity allocation, inflation-adjusted withdrawals, and corpus longevity.
- Investment Based Calculator: Projects retirement corpus and pension from NPS, debt, and mutual fund investments, including a yearly corpus progression chart.


## How It Works

### Core Calculation Engine

The formula logic is implemented in a dedicated Kotlin engine:

- `FinanceCalculatorEngine.calculateInflation(...)`
- `FinanceCalculatorEngine.calculateSwp(...)`
- `FinanceCalculatorEngine.calculateInvestment(...)`

This keeps business logic separate from UI code so the app remains easier to test and maintain.

### Screen-Level UI

Each calculator has a dedicated Compose screen with numeric form fields and a calculate action:

- Inflation screen
- SWP screen
- Investment screen

Results are rendered as readable output lines below the form.

### Yearly Corpus Chart (Investment Page)

The Investment page now includes a chart-based visualization for yearly corpus progression.

Implementation details:

- During calculation, the engine captures one data point per year-end (age, nominal corpus, inflation-adjusted corpus).
- The screen renders these points using a custom Compose `Canvas` chart.
- Two lines are shown:
   - Total corpus (nominal)
   - Inflation-adjusted corpus (real value)
- A legend and displayed age range help interpret the plotted values.

This gives a quick visual view of growth trajectory instead of relying only on text output.

### Local Data Persistence

All entry fields are persisted locally when the user taps Calculate.

Persistence behavior:

- Values are saved per screen using SharedPreferences.
- On reopening the app and returning to the same screen, previously saved values are loaded automatically.
- Defaults are used only when no prior values are available.

## Platform and Tooling

- Language: Kotlin
- UI toolkit: Jetpack Compose
- Design system: Material 3 with dynamic color (Material You)
- Navigation: Navigation Compose
- Build system: Gradle (Kotlin DSL) + Android Gradle Plugin
- Minimum Android version: Android 12 (API 31)
- Target SDK: 35
- Compile SDK: 35

## Architecture Design

A simple layered structure is used:

- UI layer: Compose screens and app navigation.
- Domain/calculation layer: Pure Kotlin financial computations.
- Data persistence layer: SharedPreferences helper for form state.
- Visualization layer: Compose Canvas chart rendered from yearly investment progression points.

Key design intent:

- Keep calculations deterministic and independent from UI framework.
- Keep persistence implementation lightweight and local.
- Preserve existing calculator behavior from the original HTML implementation.

## Design Decisions

- Compose-first UI for modern Android and faster iteration.
- Material 3 dynamic color for Android 12+ personalization.
- Predictive back compatibility through platform back callback integration.
- Bottom navigation for quick switching between calculators.
- Direct port of existing formulas to avoid behavioral drift.
- Year-end data capture in the investment engine to support charting without duplicate simulation logic.

## App Components Used

- `ComponentActivity`
- Compose `Scaffold`, top app bar, cards, text fields, buttons
- Compose `Canvas` for custom chart drawing
- `NavHost` and composable destinations
- State holders via `rememberSaveable`
- `LaunchedEffect` for loading persisted values per screen

## Platform Components Used

- Android Manifest app/activity configuration
- SharedPreferences for local persistence
- Adaptive app icon resources (foreground/background/monochrome)
- Backup and data extraction XML resources

## Project Structure (High Level)

- `app/src/main/java/com/example/financecalculators/MainActivity.kt`
- `app/src/main/java/com/example/financecalculators/FinanceApp.kt`
- `app/src/main/java/com/example/financecalculators/calculators/CalculatorScreens.kt`
- `app/src/main/java/com/example/financecalculators/calculators/FinanceCalculators.kt`
- `app/src/main/java/com/example/financecalculators/data/CalculatorPreferences.kt`
- `app/src/main/java/com/example/financecalculators/ui/theme/*`
- `app/src/main/AndroidManifest.xml`

## Build and Run

1. Ensure Java 17 is used by Gradle.
2. Ensure Android SDK path is configured in `local.properties`.
3. Build:
   - Windows: `gradlew.bat assembleDebug`
4. Install and launch (ADB):
   - `adb install -r app\\build\\outputs\\apk\\debug\\app-debug.apk`
   - `adb shell am start -n com.example.financecalculators/.MainActivity`

## Notes for Future Enhancements

- Add unit tests for all calculator formulas.
- Add export/share options for results.
- Add input validation hints and error highlighting per field.
