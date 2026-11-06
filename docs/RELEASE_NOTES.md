# Release Notes - Final POE Submission

## Overview
This release consolidates all team feature contributions, offline synchronisation, native biometric security, real-time cloud notifications, multi-language localization, and hardened CI/CD pipeline capabilities into a unified production-ready release candidate for the final POE submission.

---

## Member 1 Contributions (Jordan) — Authentication & Custom Feature
- **Native Biometric Authentication**: Integrated `BiometricPrompt` with support for fingerprints and face recognition, dynamically bound to the active user session.
- **Biometric Hardware Error Handling**: Handled edge cases including un-enrolled hardware, user cancellations, and failed attempts gracefully with user alerts and logging.
- **Asset Comparison Matrix (Feature 4)**: Implemented an interactive comparison screen allowing side-by-side analysis of simulated asset metrics.

---

## Member 2 Contributions (Ash) — Offline Persistence & Backend API
- **Hosted REST API Integration**: Connected the app to the production cloud microservice (`https://whatif-api.vercel.app`).
- **Offline Storage (RoomDB)**: The Saved list now reads from a Room cache, so it opens instantly and works without internet. Database migrations (v1 to v3) keep existing data.
- **Offline rename and delete**: Changes are applied locally at once and queued. The Saved screen shows an offline banner and a "Pending sync" chip on queued items.
- **Automatic background sync (WorkManager)**: Queued changes are sent to the REST API when a network returns, with retries and backoff. Conflicts are resolved by last write wins; a `404` drops the change.
- **Offline results**: Saved simulation results are cached and can be reopened offline, marked with an "Offline copy" note.
- **Cached asset search**: Previously searched assets can be searched again offline.
- **Tests**: 30 new unit tests cover the sync queue, retry and conflict rules, and both caches.
- **Known limitations**: Re-running a simulation and deleting from the detail screen need a connection. Offline search only covers assets searched before while online.

---

## Member 3 Contributions (David) — Real-Time Notifications & Localization
- **Firebase Cloud Messaging (FCM)**: Configured FCM push notification capabilities for foreground and background push events.
- **Multi-Language Support**: Added persistent localization for **English**, **isiXhosa**, and **Afrikaans** via standard Android String resources.
- **UI & Accessibility Enhancement**: Polished theme consistency, improved input validation feedback, and streamlined app navigation state across all screens.

---

## Member 4 Contributions (Siddiq) — DevOps, CI/CD & Integration
- **GitHub Actions CI/CD Pipeline**: Configured `.github/workflows/android.yml` to automatically execute unit tests (`testDebugUnitTest`) and debug builds (`assembleDebug`) on remote pushes.
- **Branch Integration & Code Hardening**: Cleaned repository workspace, deduplicated source files, resolved merge markers, and maintained a zero-warning baseline build.
- **Submission Documentation**: Created `docs/MERGE.md`, `docs/RELEASE_NOTES.md`, updated technical report documentation in `README.md`, and prepared release artifacts for Play Store submission.