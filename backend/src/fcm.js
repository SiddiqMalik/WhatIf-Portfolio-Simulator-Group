import { getMessaging } from "firebase-admin/messaging";
import { getApps } from "firebase-admin/app";
import { getDb } from "./firebase.js";

/**
 * Notification copy for the simulation-ready push — PART3-DAVID.
 * Kept in sync with functions/index.js and the Android string resources.
 */
const NOTIFICATION_COPY = {
  en: {
    title: "Simulation ready",
    plain: "Your what-if scenario has finished. Tap to open results.",
    named: (name) => `"${name}" has finished. Tap to open results.`,
  },
  af: {
    title: "Simulasie gereed",
    plain: "Jou what-if-scenario is klaar. Tik om resultate oop te maak.",
    named: (name) => `"${name}" is klaar. Tik om resultate oop te maak.`,
  },
  xh: {
    title: "Isimulations ilungile",
    plain: "Imeko yakho ye-what-if igqibile. Cofa ukuvula iziphumo.",
    named: (name) => `"${name}" igqibile. Cofa ukuvula iziphumo.`,
  },
};

function copyFor(locale) {
  if (locale === "af" || locale === "xh") return NOTIFICATION_COPY[locale];
  return NOTIFICATION_COPY.en;
}

/**
 * Best-effort FCM notify after a simulation is created — PART3-DAVID.
 * Never throws to the HTTP handler; failures are logged only.
 *
 * The message includes a notification payload so Android shows it when the
 * app is in the background or closed, and a data payload so a tap opens
 * SimulationResults. No click_action is set: a custom action such as
 * FLUTTER_NOTIFICATION_CLICK drops the tap unless an intent-filter matches it.
 */
export async function notifySimulationReady(uid, simulationId, simulationName) {
  try {
    const userDoc = await getDb().collection("users").doc(uid).get();
    const data = userDoc.exists ? userDoc.data() || {} : {};
    if (data.notificationsEnabled === false) {
      console.log(`[fcm] Notifications disabled for uid=${uid}; skip`);
      return;
    }
    const token = data.fcmToken;
    if (!token) {
      console.log(`[fcm] No fcmToken for uid=${uid}; skip notification`);
      return;
    }

    const copy = copyFor(data.appLocale);
    const title = copy.title;
    const body = simulationName ? copy.named(simulationName) : copy.plain;

    const message = {
      token,
      notification: { title, body },
      data: {
        simulationId: String(simulationId),
        type: "simulation_ready",
        title,
        body,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "simulations",
        },
      },
    };

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
