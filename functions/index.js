/**
 * Firebase Cloud Functions — PART3-DAVID
 *
 * Install + deploy:
 *   npm install -g firebase-tools   # once
 *   firebase login
 *   firebase use whatif-portfolio
 *   cd functions && npm install
 *   firebase deploy --only functions
 *
 * Production push path: the Vercel API (backend/src/fcm.js) sends FCM after
 * POST /v1/simulations. This Cloud Function is the Firebase CLI-managed
 * endpoint for demos / manual tests without re-running a full backtest.
 */
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onRequest } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";

initializeApp();

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

async function sendSimulationReady(uid, simulationId, simulationName) {
  const userSnap = await getFirestore().collection("users").doc(uid).get();
  const data = userSnap.exists ? userSnap.data() || {} : {};
  if (data.notificationsEnabled === false) {
    logger.info("Notifications disabled; skip", { uid });
    return { sent: false, reason: "disabled" };
  }
  const token = data.fcmToken;
  if (!token) {
    logger.info("No fcmToken; skip", { uid });
    return { sent: false, reason: "no_token" };
  }

  const copy = copyFor(data.appLocale);
  const title = copy.title;
  const body = simulationName ? copy.named(simulationName) : copy.plain;

  const messageId = await getMessaging().send({
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
      notification: { channelId: "simulations" },
    },
  });

  logger.info("FCM sent", { messageId, uid, simulationId });
  return { sent: true, messageId };
}

/**
 * Manual HTTPS endpoint for demos.
 * POST JSON: { "uid": "...", "simulationId": "...", "name": "optional" }
 * Optional header: X-WhatIf-Secret = process.env.WHATIF_FCM_SECRET
 */
export const sendSimulationNotification = onRequest(
  { cors: true },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).json({ error: "POST only" });
      return;
    }
    const secret = process.env.WHATIF_FCM_SECRET;
    if (secret && req.get("X-WhatIf-Secret") !== secret) {
      res.status(401).json({ error: "unauthorized" });
      return;
    }
    const { uid, simulationId, name } = req.body || {};
    if (!uid || !simulationId) {
      res.status(400).json({ error: "uid and simulationId are required" });
      return;
    }
    try {
      const result = await sendSimulationReady(uid, simulationId, name || "");
      res.json(result);
    } catch (err) {
      logger.error(err);
      res.status(500).json({
        error: "send_failed",
        message: String(err.message || err),
      });
    }
  }
);
