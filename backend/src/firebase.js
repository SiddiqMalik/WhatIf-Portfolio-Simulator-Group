import { readFileSync, existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { initializeApp, getApps, cert } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { initializeFirestore } from "firebase-admin/firestore";

function loadServiceAccount() {
  if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
    return JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
  }

  const configuredPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
  if (configuredPath && existsSync(configuredPath)) {
    return JSON.parse(readFileSync(configuredPath, "utf8"));
  }

  const localPath = join(dirname(fileURLToPath(import.meta.url)), "..", "serviceAccount.json");
  if (existsSync(localPath)) {
    return JSON.parse(readFileSync(localPath, "utf8"));
  }

  throw new Error(
    "Missing Firebase credentials. Set FIREBASE_SERVICE_ACCOUNT_JSON, " +
      "FIREBASE_SERVICE_ACCOUNT_PATH, or place serviceAccount.json in backend/."
  );
}

let app;
if (!getApps().length) {
  app = initializeApp({ credential: cert(loadServiceAccount()) });
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
