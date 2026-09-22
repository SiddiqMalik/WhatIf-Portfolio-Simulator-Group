import { initializeApp, getApps, cert } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { initializeFirestore } from "firebase-admin/firestore";

let app;
if (!getApps().length) {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
  app = initializeApp({ credential: cert(serviceAccount) });
} else {
  app = getApps()[0];
}

const db = initializeFirestore(app, { preferRest: true });

export function getDb() {
  return db;
}

export async function requireAuth(req, res, next) {
  const header = req.headers.authorization || "";
  const match = header.match(/^Bearer (.+)$/);
  if (!match) {
    return res.status(401).json({ error: { code: "unauthenticated", message: "Missing Bearer token" } });
  }
  try {
    const decoded = await getAuth(app).verifyIdToken(match[1]);
    req.uid = decoded.uid;
    next();
  } catch (err) {
    res.status(401).json({ error: { code: "unauthenticated", message: "Invalid or expired token" } });
  }
}

// Only used by scripts/getTestToken.js for local testing of protected routes.
export async function createCustomToken(uid) {
  return getAuth(app).createCustomToken(uid);
}
