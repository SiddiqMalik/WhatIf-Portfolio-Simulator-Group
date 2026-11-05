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
- **Offline Storage (RoomDB) & Sync Queue**: Built a persistent offline storage system using RoomDB to allow seamless app usage without internet connection.
- **Automated Synchronization**: Added connectivity detectors to automatically flush and synchronize pending local actions once network connectivity is restored.

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