<p align="center"><img src="design/branding/Logo.png" width="96" alt="MindSpring logo"></p>

# MindSpring: mental wellness and habit tracking for Android

A native Android app for building habits, checking in on mood and staying calm. It's built in **Kotlin** with **Jetpack Compose** and a local **Room** database.

This repository is the Android app from my SE4041 (Mobile Application Design and Development) assignment. MindSpring has since grown into a full product with a synced web app: [case study on ibnxr.com](https://www.ibnxr.com/work/mindspring/).

<p align="center">
  <img src="design/screens/Home%20Dashboard.png" width="200" alt="Home dashboard">
  <img src="design/screens/Habits%20List.png" width="200" alt="Habits list">
  <img src="design/screens/Mood%20Check-in.png" width="200" alt="Mood check-in">
  <img src="design/screens/Insights%20(Dark).png" width="200" alt="Insights, dark theme">
</p>

## Features
- **Habits:** create, track and complete habits, with a detail view, stats and a completion celebration.
- **Mood:** daily check-ins and mood history.
- **Calm:** a guided breathing exercise and a gratitude journal.
- **Insights:** charts and an insight engine that turns your history into patterns.
- **Reminders:** notification channels and full-screen alarms.
- **Accounts:** sign-up and login with hashed passwords, plus local backup.
- **Light and dark themes**, from a custom design system.

## Architecture
| Layer | What's there |
|---|---|
| UI | Jetpack Compose screens, a shared component library, Navigation Compose, ViewModels |
| Domain | Habit stats, task logic, the insight engine, validators, password hashing |
| Data | A Room database (Kotlin codegen, exported schemas), repositories, DataStore settings, backup |
| System | Reminder scheduling, notification channels, an alarm activity |

`AppContainer` wires the dependencies by hand. Code is in `app/src/main/java/com/mindspring/app/`.

## Build
Open the project in Android Studio and run the `app` configuration. Or from the command line:

```bash
./gradlew assembleDebug
```

`minSdk 26` · `targetSdk 36`

## Design
The UI prototype screens and the design system are in [`design/`](design/). The assignment report is in [`report/`](report/).

---
Built by [Mohamed Asan](https://github.com/InfinityBrainNetworks) · [IBN](https://www.ibnxr.com)
