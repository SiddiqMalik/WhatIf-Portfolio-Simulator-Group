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

async function sendSimulationReady(uid, simulationId, simulationName) {
  const userSnap = await getFirestore().collection("users").doc(uid).get();
  const token = userSnap.exists ? userSnap.data()?.fcmToken : null;
  if (!token) {
    logger.info("No fcmToken; skip", { uid });
    return { sent: false, reason: "no_token" };
  }

  const title = "Simulation ready";
  const body = simulationName
    ? `"${simulationName}" has finished. Tap to open results.`
    : "Your what-if scenario has finished. Tap to open results.";

  const messageId = await getMessaging().send({
    token,
    notification: { title, body },
    data: {
      simulationId: String(simulationId),
      type: "simulation_ready",
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
