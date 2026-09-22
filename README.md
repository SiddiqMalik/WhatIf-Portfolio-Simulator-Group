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

# What-If Portfolio Simulator — Person 2: Custom Cloud REST API & Backend Integration

Covers the assigned scope (25 marks):
- Build & host the serverless REST API (Node.js/Express, hosted on Vercel)
- Implement endpoints: `searchAssets`, `getAssetHistory`, `createSimulation`, and full CRUD
- Connect the mobile app's network layer (Retrofit) to the cloud endpoints

## How to run the backend

1. `cd backend`, run `npm install`.
2. Create `.env.local` with `FIREBASE_SERVICE_ACCOUNT_JSON`, `ALPHA_VANTAGE_KEY`,
   and `COINGECKO_API_KEY` (see [Firebase Console](https://console.firebase.google.com)
   → Project settings → Service accounts to generate the first one).
3. Run locally: `node --env-file=.env.local local.js` (serves on
   `http://localhost:3000`).
4. Deploy: `vercel --prod` from the `backend` folder (requires a
   [Vercel](https://vercel.com) account linked via `vercel login`).
5. The live API the app talks to is already deployed at
   `https://whatif-api.vercel.app` — no redeploy needed to run the Android app.

## API Reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/v1/health` | No | Service health check |
| GET | `/v1/whoami` | Yes | Verifies the caller's Firebase ID token |
| GET | `/v1/cpi` | No | South African CPI index (Stats SA, Dec 2024=100 base) |
| GET | `/v1/assets/search` | No | Searches stocks (Alpha Vantage) and/or crypto (CoinGecko) |
| GET | `/v1/assets/:symbol/history` | No | Historical prices, cached in Firestore after first fetch |
| POST | `/v1/simulations` | Yes | Runs a DCA backtest against real historical prices and CPI |
| GET | `/v1/simulations` | Yes | Lists the signed-in user's saved simulations |
| GET | `/v1/simulations/:id` | Yes | Retrieves one simulation |
| PUT | `/v1/simulations/:id` | Yes | Updates a simulation's name or status |
| DELETE | `/v1/simulations/:id` | Yes | Deletes a simulation |

## What's real vs. known limitations

| Piece | Status |
|---|---|
| All 10 endpoints above | Fully implemented, tested locally and against the live deployment |
| Firebase Auth verification | Fully implemented — every protected route checks a real ID token via the Admin SDK |
| Historical price caching | Fully implemented — Firestore `priceHistory` collection, populated on first request per symbol |
| Equity daily history | Limited to the most recent ~100 trading days (Alpha Vantage free-tier restriction) — weekly/monthly intervals return full history and are used for longer simulations |
| Crypto history | Limited to the past 365 days (CoinGecko free-tier restriction) |
| Android integration | Fully implemented — `data/remote/` (Retrofit), `RemoteSimulationRepository`; Dashboard fetches via `GET /v1/simulations` |

## Design notes

- **Hosting change from Part 1**: the original design specified Firebase Cloud
  Functions. That requires Firebase's paid Blaze plan, which wasn't available
  to the team, so the same Express API is hosted on [Vercel](https://vercel.com)'s
  free tier instead. Firebase Authentication and Firestore are unchanged.
- **Backtest vs. projection**: `createSimulation` runs a historical
  backtest — it fetches real recorded prices for each allocated asset and
  buys units at the actual price on each contribution date, then compares the
  result to real CPI data. This is distinct from the app's other simulation
  tool, which projects forward from a user-assumed return rate.
- **Firestore security rules**: `simulations`, `cpi`, and `priceHistory` are
  API-only (the Admin SDK bypasses rules; direct client reads are denied).
  Only `users/{uid}` profile data is directly readable/writable by the
  signed-in user.
- **Testing protected routes**: `backend/scripts/getTestToken.js` mints a
  real Firebase ID token for a test user, used to exercise auth-protected
  endpoints from the command line without the Android app.

## References for adopted patterns (cite in your report)

- Alpha Vantage (2026). *Stock Time Series APIs.*
- CoinGecko (2026). *CoinGecko API Documentation.*
- Firebase (2026). *Firebase Admin SDK — Verify ID Tokens.*
- Firebase (2026). *Cloud Firestore Security Rules.*
- Vercel (2026). *Deploying Node.js Serverless Functions.*
- Square (2026). *Retrofit — A Type-Safe HTTP Client for Android.*
