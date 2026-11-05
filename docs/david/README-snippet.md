# David Minlah — Notifications, language, UI (Part 3)

## What shipped

- **FCM** on Android (`WhatIfFirebaseMessagingService`), token stored at `users/{uid}.fcmToken`
- **POST_NOTIFICATIONS** permission + Settings toggle
- Tap notification → `SimulationResults`
- **Vercel API** sends FCM after `POST /v1/simulations` (`backend/src/fcm.js`)
- **Firebase Cloud Function** `sendSimulationNotification` for manual / demo sends
- **English / Afrikaans / isiXhosa** via `values`, `values-af`, `values-xh` + Settings picker
- Bottom nav, Settings, Sign-In labels localised; AppCompat locale persistence

## Firebase Console checklist (do this once)

1. Open [Firebase Console](https://console.firebase.google.com/) → project **whatif-portfolio**.
2. **Build → Cloud Messaging**
   - Confirm Cloud Messaging API (V1) is enabled.
   - No need to upload an APNs key (Android only).
3. **Project settings → Cloud Messaging**
   - Ensure the Android app `com.reztek.whatifportfolio` is registered (matches `google-services.json`).
4. **Project settings → Service accounts**
   - The same service account used by Vercel (`FIREBASE_SERVICE_ACCOUNT_JSON`) can send FCM via Admin SDK. No extra key for Android client.
5. **Firestore**
   - `users/{uid}` already allows the signed-in user to write; the app writes `fcmToken` there.
6. **Deploy Cloud Functions (CLI)**

```bash
npm install -g firebase-tools
firebase login
cd /path/to/whatif-portfolio
firebase use whatif-portfolio
cd functions && npm install && cd ..
firebase deploy --only functions
```

7. Optional: set a secret for the HTTPS test function

```bash
firebase functions:secrets:set WHATIF_FCM_SECRET
# or in Google Cloud Console → Cloud Functions → env vars
```

8. Redeploy the **Vercel** backend so `backend/src/fcm.js` is live:

```bash
cd backend
vercel --prod
```

## How to test on a physical phone

1. Install a debug build signed into a real Firebase user.
2. Settings → enable **Push notifications** → allow the system permission.
3. Confirm Firestore `users/{yourUid}` has `fcmToken`.
4. Run a **Historical** simulation (online). When it finishes you should get a tray / heads-up notification.
5. Tap it → Results screen for that simulation.
6. Settings → Language → Afrikaans / isiXhosa → UI labels update; kill app and reopen to confirm persistence.

### Manual FCM test (no new simulation)

```bash
curl -X POST "https://<region>-whatif-portfolio.cloudfunctions.net/sendSimulationNotification" \
  -H "Content-Type: application/json" \
  -H "X-WhatIf-Secret: <your-secret-if-set>" \
  -d '{"uid":"<firebase-uid>","simulationId":"<existing-sim-id>","name":"Demo"}'
```

## Screenshots to capture for README

- Settings with notifications On + language Afrikaans
- System permission dialog
- Tray notification
- Results opened from tap
- Bottom nav in isiXhosa
