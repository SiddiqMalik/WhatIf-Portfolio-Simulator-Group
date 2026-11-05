import { getMessaging } from "firebase-admin/messaging";
import { getApps } from "firebase-admin/app";
import { getDb } from "./firebase.js";

/**
 * Best-effort FCM notify after a simulation is created — PART3-DAVID.
 * Never throws to the HTTP handler; failures are logged only.
 */
export async function notifySimulationReady(uid, simulationId, simulationName) {
  try {
    const userDoc = await getDb().collection("users").doc(uid).get();
    const token = userDoc.exists ? userDoc.data()?.fcmToken : null;
    if (!token) {
      console.log(`[fcm] No fcmToken for uid=${uid}; skip notification`);
      return;
    }

    const title = "Simulation ready";
    const body = simulationName
      ? `"${simulationName}" has finished. Tap to open results.`
      : "Your what-if scenario has finished. Tap to open results.";

    const message = {
      token,
      notification: { title, body },
      data: {
        simulationId: String(simulationId),
        type: "simulation_ready",
      },
      android: {
        priority: "high",
        notification: {
          channelId: "simulations",
          clickAction: "FLUTTER_NOTIFICATION_CLICK",
        },
      },
    };

    // Admin app is already initialised in firebase.js
    if (!getApps().length) {
      console.warn("[fcm] Firebase Admin not initialised");
      return;
    }
    const id = await getMessaging().send(message);
    console.log(`[fcm] Sent simulation-ready message ${id} to uid=${uid}`);
  } catch (err) {
    console.error("[fcm] Failed to send notification (non-fatal)", err);
  }
}
