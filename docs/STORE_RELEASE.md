# Google Play Store Release & Publication Readiness

This document details the release preparation, publication guidelines, asset requirements, and production build steps for publishing the WhatIf Portfolio Simulator application to the Google Play Store.

---

## 1. Store Listing Metadata

- **Application Title**: WhatIf Portfolio Simulator
- **Short Description**: Simulate investment portfolios and evaluate nominal balance growth against CPI-adjusted purchasing power.
- **Full Description**:
  WhatIf Portfolio Simulator is a native Android application made to help long-term investors visualize how inflation erodes nominal investment growth. Featuring real-time scenario modeling, native biometric security, offline persistence, and multi-language support (English, isiXhosa, Afrikaans).
- **Category**: Finance
- **Content Rating**: Everyone (Unrestricted financial calculation utility)
- **Default Language**: English (`en-US`)

---

## 2. Store Publication Readiness Checklist

- [x] **Release Build Variant**: Minification and resource shrinking configured in `app/build.gradle.kts`.
- [x] **Keystore & Signing Config**: Production signing keys configured for release target builds.
- [x] **Localization Verification**: Tri-lingual string resources verified across `values/`, `values-af/`, and `values-xh/`.
- [x] **Screenshots & Promo Assets**: Screen captures prepared for Auth, Dashboard, Canvas Chart, and Settings.
- [x] **Privacy Policy**: Standard user privacy policy hosted and prepared for Google Play Console submission.

---

## 3. Production Release Build Commands

To build the signed Android App Bundle (AAB) for Play Store uploading:

```bash
./gradlew bundleRelease
```

The generated bundle is written to:

```
app/build/outputs/bundle/release/app-release.aab
```

To build a signed release APK for local installation and testing instead:

```bash
./gradlew assembleRelease
```

---

## 4. Privacy & Data Protection Compliance

* **Biometric Security**: Biometric credentials and fingerprint data are processed exclusively within the hardware-backed Android KeyStore on-device. No raw biometric templates leave the physical device or transmit to external servers.
* **Push Notification Tokens**: Device registration tokens generated via Firebase Cloud Messaging (FCM) are stored securely on the user's Firestore document (`users/{uid}`) solely to route targeted notifications.

---

## 5. Release Signing Setup

To generate production release builds locally or in GitHub Actions, configure the following secrets:

* `KEYSTORE_BASE64` (Base64-encoded `.jks` file)
* `KEY_ALIAS`
* `KEY_PASSWORD`
* `STORE_PASSWORD`

To produce the Base64 value for `KEYSTORE_BASE64`:

```bash
base64 -w 0 release-keystore.jks > keystore.base64.txt
```

In GitHub Actions, decode the secret back into a keystore file before running the release build:

```bash
echo "$KEYSTORE_BASE64" | base64 --decode > app/release-keystore.jks
./gradlew bundleRelease
```

> **Security note:** Never commit the `.jks` keystore file or any passwords to the repository. Store them only as GitHub repository secrets (Settings → Secrets and variables → Actions) and keep a secure offline backup of the keystore, since losing it prevents future updates to the published app.

### Continuous Integration & Release Verification
![GitHub Actions Build Passing](docs/screenshots/github-actions-passing.png)
![Signed Release APK Location](docs/screenshots/release-apk-folder.png)
![Gradle Build Output](docs/screenshots/build-successful-output.png)