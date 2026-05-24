# Budget Widget

A daily budget tracking Android app built with Kotlin, Jetpack Compose, Room, and Material 3.

The app helps users track expenses, manage a monthly budget limit, and understand how much can still be spent today. It also exposes the current daily budget state through:
- A phone home screen widget (resizable)
- A Wear OS tile (display-only)

## What The App Does

- Stores expenses in a local Room database
- Lets users add, edit, and delete expense entries
- Lets users set a monthly budget limit
- Calculates and displays:
  - Today left to spend
  - Today spent
  - Today's limit
  - Month limit, month spent, month left, and days left
- Shows expense history for the current month
- Publishes budget state to a home screen widget and Wear tile

## Core User Flows

### Main Screen
- Shows a Today summary card:
  - Prominent line: `x left today (y spent)`
  - Secondary line: `z today's limit`
- Shows Month summary card:
  - Limit
  - Days Left
  - Left
  - Spent
- Shows current month expenses list
- Allows updating monthly limit via dialog
- Navigates to Add/Edit expense screen

### Add/Edit Expense Screen
- Supports creating a new expense and editing/deleting existing expenses
- Date uses system date picker (manual typing disabled)
- Save button is disabled until all required fields are valid:
  - Valid date
  - Amount > 0
  - Non-empty description
- Save emits completion event and returns to the previous screen reliably

### Phone Widget
- Displays two lines:
  - `x Left`
  - `y Limit`
- Center-aligned text
- Fully resizable in both directions (launcher limitations still apply)
- Tapping widget opens the app

### Wear Tile
- Read-only tile displaying:
  - `x Left`
  - `y Limit`
- No data entry on the tile

## Platform Components Used

- Kotlin + Coroutines + Flow
- Jetpack Compose UI
- Material 3
- Navigation Compose
- Android ViewModel
- Room (local storage)
- SharedPreferences (monthly limit + daily limit snapshot cache)
- AppWidgetProvider + RemoteViews (phone widget)
- Wear Tiles API (Wear OS tile)
- ADB for development deploy/run

## Architecture

A lightweight clean layering approach is used:

- UI layer:
  - Compose screens (`ui/main`, `ui/addedit`)
  - ViewModels hold screen state and orchestrate business logic
- Domain/business layer:
  - Business rules implemented mainly in `MainViewModel` + `BudgetPreferences`
- Data layer:
  - `ExpenseRepository` wraps DAO operations and triggers widget/tile refreshes
  - `ExpenseDao` + `ExpenseEntity` define persistence model
  - `BudgetPreferences` stores monthly limit and daily limit cache snapshot
- Composition root:
  - `BudgetTrackerApp` + `AppContainer` perform manual dependency wiring

This keeps the app simple, explicit, and easy to reason about for a single-module project.

## Data Model

### ExpenseEntity
- `id: Long` (auto-generated)
- `dateEpochDay: Long` (LocalDate epoch-day storage)
- `amount: Double`
- `description: String`

The app stores dates as epoch-day for straightforward range filtering and sorting.

## Business Logic Details

### Current Month Expense Window
`observeCurrentMonthExpenses()` queries expenses between first and last day of current month.

### Today's Metrics
From current month expenses:
- `todaySpent` = sum of expenses where `date == today`
- `monthSpentBeforeToday` = sum of expenses where `date < today`

### Daily Limit Snapshot Rule
The app uses a "start-of-day" daily limit concept.

Daily limit is computed as:

`todayLimit = (monthlyLimit - monthSpentBeforeToday) / daysLeftInMonth`

Then:

`todayLeftToSpend = todayLimit - todaySpent`

### Why Snapshot Cache Exists
The daily limit should remain stable during the same day while today's spending changes. A SharedPreferences snapshot is used for this.

Cache validation includes:
- date
- monthly limit
- month spent before today
- days left in month

If any of these inputs change (for example, a backdated expense is added/edited/deleted), the cache is invalidated/recomputed so today's limit updates correctly.

### Monthly Limit Updates
When monthly limit changes:
- Stored monthly limit updates
- Daily limit snapshot keys are cleared
- Widget and tile are refreshed

## Navigation & Back Behavior

- Navigation is handled by Navigation Compose
- `home` is start destination
- Add/Edit returns using `popBackStack()` with a safe fallback to home if needed
- Predictive back is enabled in manifest via `enableOnBackInvokedCallback=true`

## Widget & Tile Refresh Strategy

To keep external surfaces in sync, refreshes are triggered after:
- Expense add
- Expense update
- Expense delete
- Monthly limit change

Widget also listens for date/time/timezone changes.

## Design Decisions

1. Room + Flow for reactive local data:
- Current month list and totals update automatically when DB changes

2. SharedPreferences for budget config and lightweight snapshot caching:
- Simple, no extra schema overhead for non-relational settings

3. Epoch-day date storage:
- Fast and reliable date range queries

4. Manual DI (`AppContainer`) instead of introducing a DI framework:
- Reduced complexity for app scale

5. Minimal-friction forms:
- Date picker to avoid invalid date typing
- Save button disabled for invalid form state

6. Dynamic Material theming:
- Uses dynamic color on supported Android versions with fallback palettes

7. Adaptive launcher icon:
- Foreground, background, and monochrome layers for modern launcher support

## Project Structure

```text
app/src/main/
  AndroidManifest.xml
  java/dev/prem/budgetwidget/
    BudgetTrackerApp.kt
    MainActivity.kt
    navigation/
      BudgetNavGraph.kt
      Destinations.kt
    data/
      local/
        AppDatabase.kt
        ExpenseDao.kt
        ExpenseEntity.kt
      preferences/
        BudgetPreferences.kt
      repository/
        ExpenseRepository.kt
    ui/
      main/
        MainScreen.kt
        MainViewModel.kt
      addedit/
        AddEditExpenseScreen.kt
        AddEditExpenseViewModel.kt
      theme/
        Theme.kt
        Color.kt
        Type.kt
      util/
        Formatters.kt
    widget/
      BudgetDailyWidgetProvider.kt
    wear/
      BudgetDailyTileService.kt
  res/
    layout/widget_daily_budget.xml
    xml/budget_daily_widget_info.xml
    drawable/*
    mipmap-anydpi-v26/*
    values/*
```

## Build & Run

### Prerequisites
- Android Studio (JBR/Java 17)
- Android SDK configured
- Gradle wrapper in project
- ADB-connected device or emulator

### Build
```powershell
./gradlew.bat :app:assembleDebug
```

### Install On Connected Device
```powershell
./gradlew.bat :app:installDebug
```

### Launch App
```powershell
adb shell am start -n dev.prem.budgetwidget/.MainActivity
```

### Specific Device Launch
```powershell
adb -s <device_id> shell am start -n dev.prem.budgetwidget/.MainActivity
```

## Notes About Widgets & Tiles

- Launcher widget resize behavior can still be constrained by the phone launcher implementation even if app metadata allows full resizing.
- Wear tile code is included and builds in this app module, but runtime validation requires a Wear OS device/emulator connected as an ADB target.

## Current Version Highlights

- Daily limit algorithm based on start-of-day month-left state
- Correct recalculation when backdated transactions change budget inputs
- Reliable add/edit save completion flow and navigation
- Predictive back enabled
- Resizable home widget + Wear tile
- Dynamic Material color support
- Custom adaptive app icon
