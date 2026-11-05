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