# my-projects

This repository is a mono repo that contains multiple Android projects maintained in a single place. The structure is organized by project folder so each app can evolve independently while still sharing the same workspace, tooling habits, and development conventions. This setup makes it easier to compare implementations, reuse architectural ideas, and manage Android experiments and production-ready utilities side by side.

## Projects

### budget_widget

Budget Widget is a Kotlin Android budgeting app focused on daily spending control. It tracks expenses with Room, supports monthly budget limits, computes a daily spend allowance, and surfaces budget status both in the main app UI and external surfaces like a home screen widget and Wear OS tile. The project emphasizes practical personal-finance workflows with quick-entry forms, month-level summaries, and lightweight local persistence.

### finance_calculators

Finance Calculators is a native Android conversion of web-based financial tools, implemented with Jetpack Compose and a dedicated Kotlin calculation engine. It includes inflation, SWP, and investment-based retirement calculators, with persisted form inputs and clear result rendering. The investment module also includes a yearly corpus progression chart, making the app useful for planning scenarios where both numerical outputs and trend visualization are important.

### folder_cleaner

Folder Cleaner is a storage-maintenance utility app that deletes old files from user-selected folders while respecting excluded file extensions. It supports manual cleanup, configurable WorkManager-based scheduled cleanup, run history with reclaimed-size analytics and trend charting, per-folder enable/disable control, and JSON backup/import for migration across devices. The app is built with Compose, Room, DataStore, and modern Android platform components for a configurable and safety-oriented cleanup workflow.

## Notes

- Repository-level agent and workflow guidance is documented in AGENTS.md.
- Each project contains its own README with implementation-specific details and setup instructions.
