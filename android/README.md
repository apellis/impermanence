# Impermanence Android

This module contains the Android port of Impermanence using Jetpack Compose, Kotlin, and Jetpack DataStore for persistence.

## Requirements

* Android Studio Ladybug or newer
* Android SDK 34
* A device or emulator running Android 8.0 (API 26) or higher

## Getting Started

1. Open the `android/` directory in Android Studio and let it sync Gradle.
2. Place the meditation bell audio file at `app/src/main/res/raw/bell_0.mp3`. The player will fall back to other filename variants if needed (see `model/BellCatalog.kt`).
3. Build and run the `app` configuration.

### Data & Settings

* Day schedules and metadata are persisted with a `DataStore<List<Day>>` serializer at `days.json`.
* User settings (24-hour clock, looping day schedules) are managed via `DataStore<Preferences>`.

### Modules

* `model/` – shared data models (`Day`, `Segment`, `Bell`, themes).
* `data/` – DataStore serializers and repository implementations.
* `ui/` – Jetpack Compose UI, navigation graph, and screens.
* `domain/` – timer engine and bell playback utilities.

## Notes

* Drag-and-drop reordering in lists is provided via explicit move controls.
* Manual bell controls live on the active day screen and persist with the underlying day record.
* The screen stays awake while a day is actively running; this behavior mirrors the iOS app.

### Command-line build

Use the bundled wrapper: `./gradlew assembleDebug`.
