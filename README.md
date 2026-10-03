# 1nf0run

1nf0run is an offline Android auto-runner in which an office worker races through five increasingly difficult levels. The player jumps across procedurally assembled platforms, avoids hardware-themed hazards and enemies, collects data tokens, and unlocks the next stages.

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_custom.png" alt="1nf0run app icon" width="180">
  <img src="app/src/main/res/drawable/levelmapscreen.png" alt="1nf0run level map artwork" width="180">
  <img src="app/src/main/res/drawable/game_finishedscreen.png" alt="1nf0run completion artwork" width="180">
</p>

## Features

- Five levels with distinct visual themes and difficulty settings
- Custom `SurfaceView` game loop with fixed-timestep updates
- Procedurally assembled platform, hazard, enemy, and collectible patterns
- Variable-height jumping, collision handling, lives, scoring, and power-ups
- Local save slots, unlock progression, and highscores backed by Room
- Pause, restart, retry, and local data-reset flows
- Fully offline operation with no network permission or external service dependency

## Tech stack

- Java 11 language target
- Android SDK 36
- Android Views and Material Components
- AndroidX Room for local persistence
- Gradle Wrapper 9.1.0 and Android Gradle Plugin 9.0.0
- JUnit 4 and AndroidX Test

## Getting started

### Requirements

- A recent Android Studio installation
- JDK 21 (the bundled Android Studio runtime is suitable)
- Android SDK 36
- An emulator or Android device running Android 8.0 (API 26) or newer

### Open and run

1. Clone the repository.
2. Open its root directory in Android Studio.
3. Allow Gradle to download and synchronize the declared dependencies.
4. Select an emulator or connected device.
5. Run the `app` configuration.

### Command-line verification

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

On macOS or Linux:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is generated under `app/build/outputs/apk/debug/`.

## Project structure

```text
app/src/main/java/de/hsos/prog3/inforun/
├── data/       Room database, entities, DAOs, and repositories
├── game/       Game loop, world generation, entities, and physics
└── ui/         Activities and the pause dialog

app/src/main/res/
├── drawable/   Game artwork and UI backgrounds
├── layout/     Android view layouts
└── values/     Strings, colors, and themes
```

## Visual assets and AI disclosure

All visual game assets in this repository were generated specifically for 1nf0run with AI tools by the project authors and were subsequently selected, edited, and integrated by them. Several background images retain C2PA content-credential metadata identifying ChatGPT/GPT-4o as the generating tool. No third-party stock artwork is included.

The application does not connect to OpenAI or any other AI service at runtime and does not require an API key.

## Authors

- Thomas Koch
- Marie Müller

## License

This repository is publicly available for portfolio and educational review. The project code and original visual assets are not released under an open-source license. See [LICENSE](LICENSE) for details. Third-party libraries retain their respective licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
