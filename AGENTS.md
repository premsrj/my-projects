# Agent Setup Notes for Android Projects

When creating or modifying Android projects under this folder, configure both Java SDK and Android SDK early so Gradle can build successfully.

## Required Java SDK
- Use Java 17 for Gradle/Android builds.
- If the system default Java is newer and causes build script errors, pin Gradle to Android Studio JBR in project `gradle.properties`:
  - `org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr`

## Required Android SDK
- Ensure Android SDK path is configured in each Android project via `local.properties`:
  - `sdk.dir=C\\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk`
- Alternatively, set `ANDROID_HOME` to a valid SDK location.

## Quick Validation
- Run `./gradlew tasks` (or `gradlew.bat tasks` on Windows) to confirm toolchain setup.
- Then run `assembleDebug` before making wider code changes.
