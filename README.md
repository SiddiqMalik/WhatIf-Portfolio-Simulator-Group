<<<<<<< HEAD
# What-If Portfolio Simulator — Person 1: Core Android UI & Navigation

Covers the assigned scope (25 marks):
- Jetpack Compose screens: **SSO Sign-In**, **Dashboard**, **Settings**
- The **Navigation Wireflow** expressed in code (`WhatIfNavGraph.kt`)
- Local UI state holders (`SignInViewModel`, `DashboardViewModel`, `SettingsViewModel`)
- Basic user preferences persistence (`UserPreferencesRepository`, DataStore)

## How to drop this into Android Studio

1. Create a new Android Studio project (**Empty Activity**, Jetpack Compose,
   min SDK 26+), package name `com.reztek.whatifportfolio`.
2. Copy `app/src/main/java/com/reztek/whatifportfolio/**` over the generated
   package, replacing `MainActivity.kt`.
3. In the [Firebase Console](https://console.firebase.google.com), create a
   project, register the app with this package name, enable **Authentication
   → Google**, enable **Firestore**, and download `google-services.json` into
   `app/`.
4. Copy the dependencies in `app/build.gradle.kts.snippet` into your real
   `app/build.gradle.kts`, and add the Google Services plugin classpath to
   the project-level `build.gradle.kts`.
5. In `MainActivity.kt`, replace `webClientId` with the **Web client** OAuth
   ID from `google-services.json` (`client_type: 3`).
6. Run on a **physical device** signed into a Google account (Credential
   Manager's Google ID flow is unreliable on emulators without Play Services
   configured).

## What's stubbed vs. real

| Screen / piece                  | Status                                         |
|----------------------------------|-------------------------------------------------|
| Sign-In                          | Fully implemented (Credential Manager + Firebase Auth) |
| Dashboard                        | Fully implemented (reads Firestore `simulations` subcollection) |
| Settings                         | Fully implemented (DataStore + Firestore currency mirror) |
| Navigation graph                 | Fully wired for all 7 screens |
| Simulation Builder / Results / Saved List / Detail | `PlaceholderScreen` — Person 2/3 scope |

The placeholder screens exist purely so the app **compiles and runs
end-to-end on a device**, satisfying the "must successfully compile and run"
prototype requirement even before the rest of the team's screens land. Swap
each `PlaceholderScreen(...)` call in `WhatIfNavGraph.kt` for the real
composable as it's completed.

## Design notes

- **Colour language**: teal (`TealPrimary`) / navy (`NavyDeep`) throughout,
  matching the app icon concept from Deliverable 2 and reserved for the
  nominal-vs-real-value distinction used later in the results chart.
- **State holders**: every screen's ViewModel exposes a single sealed
  `UiState` (or a plain data class where only one shape is possible, as in
  Settings) via `StateFlow`, following the unidirectional-data-flow pattern
  recommended in the official
  [Jetpack Compose state guidance](https://developer.android.com/develop/ui/compose/state).
- **Logging**: every ViewModel and `MainActivity`'s lifecycle callbacks log
  through `android.util.Log` (tagged per class) so `adb logcat` shows a clear
  trace of auth state, navigation edges, and Firestore reads/writes —
  satisfying the "functional logging to demonstrate a clear programmatic
  understanding of lifecycle and state transitions" deliverable.
- **Validation-ready pattern**: Settings and Sign-In both surface errors as
  part of their `UiState` rather than one-off `Toast`s, so the same inline
  error pattern documented for Simulation Builder (Deliverable 3, Screen 9)
  is already established for the rest of the team to reuse.

## References for adopted patterns (cite in your report)

- Google (2026). *Credential Manager — Android Developers.*
- Google (2026). *Jetpack DataStore Preferences — Android Developers.*
- Google (2026). *Navigation with Compose — Android Developers.*
- Firebase (2026). *Authenticate Using Google Sign-In on Android.*
=======
# WhatIf-Portfolio-Simulator-Group

>>>>>>> main
