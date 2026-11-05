# Generative AI Usage Statement

## Overview & Scope
Amidst the development of the OPSC7312/PROG7314 POE project, Generative AI tools (specifically Google Gemini and ChatGPT) were used strictly as auxiliary support mechanisms for productivity, debugging, workflow optimization, and technical documentation drafting.

---

## Areas of Assistance

1. **Git Workflow & CI/CD Pipeline Configuration**:
   - Aided in structuring hardened GitHub Actions workflow syntax (`.github/workflows/android.yml`).
   - Guided terminal commands for git branch management, merge conflict resolution, and baseline environment synchronization (Gradle 8.9 / JDK 21 setup).

2. **Documentation Formatting & Structuring**:
   - Issued out Markdown templates for technical documentation, including `README.md`, `MERGE.md`, `RELEASE_NOTES.md`, and Play Store release guidelines.
   - Structured team contribution summaries aligned with module assessment criteria.

3. **Code Quality & Diagnostics**:
   - Offered initial diagnostic pointers for build failures, line-ending compatibility issues (CRLF vs LF), and dependency resolution.

---

## Human Verification & Team Responsibility

All core software application code, architectural decisions, business engine logic, Kotlin ViewModel implementations, RoomDB schemas, REST API connections, FCM push notification setup, and UI Composable/XML layouts were designed, authored, and rigorously tested by the group members (Jordan, Ash, David, and Siddiq).

Every suggestion, configuration block, or command produced by AI tools was manually reviewed, verified against project requirements, compiled, and executed locally on physical devices before being committed to the shared repository. AI was not utilized to generate production business logic or bypass individual technical contributions.

---

## Human Testing & Verification Evidence

| Subsystem | Test Command / Method | Verification Status | Target Environment |
| :--- | :--- | :--- | :--- |
| **Calculation Engine** | `./gradlew testDebugUnitTest` | 100% Passed (0 Failures) | Local JVM / GitHub Actions |
| **Biometric Auth** | Physical Fingerprint Prompt | Verified Hardware Callback | Android 14 Physical Device |
| **Offline Room Queue** | Airplane Mode Action & Reconnect | Sync Payload Dispatched | Pixel 8 Physical Device |
| **FCM Push Events** | Cloud Messaging Test Payload | Notification Bar Banner | Android 14 Physical Device |