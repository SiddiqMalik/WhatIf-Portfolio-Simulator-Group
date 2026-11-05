# Branch Integration Sequence & Merge Strategy

This document establishes the official integration strategy for merging functional team branches into `main` for the final POE submission.

## Integration Order & Rubric Breakdown

1. **Member 1 (Jordan)** - `origin/Jordan-Biometric-Auth-Compare`
    - Biometric Authentication (`BiometricPrompt`) & Session linking
    - User-Defined Feature 4: Interactive asset comparison matrix

2. **Member 2 (Ash)** - `origin/Ash-Cloud-REST-API-&-Backend-Integration`
    - Express REST API connection (`https://whatif-api.vercel.app`)
    - User-Defined Feature 5: Offline data persistence (RoomDB) & pending sync queue

3. **Member 3 (David)** - `origin/David-Notifications-Language-UI`
    - Firebase Cloud Messaging (FCM) real-time push notifications
    - Multi-language localization (English, isiXhosa, Afrikaans) & persistent locale

4. **Member 4 (Siddiq)** - `origin/Siddiq-CI-Docs-Release`
    - Branch integration, release engineering, and CI hardening
    - Final POE documentation, Play Store submission assets, and AI usage statement

## Conflict Resolution Protocol
- Retain additive code sections annotated with `// PART3-<NAME>`.
- Verification command required following every merge: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.