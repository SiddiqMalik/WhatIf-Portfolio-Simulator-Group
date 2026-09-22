import { createCustomToken } from "../src/firebase.js";

const uid = process.argv[2] || "test-user-1";
const webApiKey = process.env.FIREBASE_WEB_API_KEY;
if (!webApiKey) {
  console.error("Set FIREBASE_WEB_API_KEY in .env.local first.");
  process.exit(1);
}

const customToken = await createCustomToken(uid);

const res = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=${webApiKey}`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ token: customToken, returnSecureToken: true }),
});
const data = await res.json();
if (!data.idToken) {
  console.error("Failed to exchange custom token:", data);
  process.exit(1);
}
console.log(data.idToken);
