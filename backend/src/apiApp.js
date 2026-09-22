import express from "express";
import { requireAuth, getDb } from "./firebase.js";
import { searchAssets } from "./assetSearch.js";

const app = express();
app.use(express.json());

// Public health check: proves the API is deployed and reachable.
app.get("/v1/health", (_req, res) => {
  res.json({ status: "ok", service: "whatif-api", time: new Date().toISOString() });
});

// Protected route: proves token verification works. Requires a real Firebase ID token.
app.get("/v1/whoami", requireAuth, (req, res) => {
  res.json({ uid: req.uid });
});

// South African CPI index, monthly. Optional ?from=YYYY-MM&to=YYYY-MM.
app.get("/v1/cpi", async (req, res) => {
  try {
    const db = getDb();
    const snapshot = await db.collection("cpi").orderBy("__name__").get();
    let rows = snapshot.docs.map((doc) => ({ period: doc.id, index: doc.data().index }));
    const { from, to } = req.query;
    if (from) rows = rows.filter((r) => r.period >= from);
    if (to) rows = rows.filter((r) => r.period <= to);
    res.json({ base: "Dec 2024=100", data: rows });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: { code: "internal_error", message: "Failed to load CPI data" } });
  }
});

// Search stocks (Alpha Vantage) and/or crypto (CoinGecko). ?q=apple&type=all|equity|crypto
app.get("/v1/assets/search", async (req, res) => {
  const q = (req.query.q || "").toString().trim();
  const type = (req.query.type || "all").toString();
  if (!q) {
    return res.status(400).json({ error: { code: "invalid_request", message: "Query parameter 'q' is required" } });
  }
  try {
    const results = await searchAssets(q, type);
    res.json({ query: q, type, results });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: { code: "internal_error", message: "Failed to search assets" } });
  }
});

// Anything else gets a consistent JSON error.
app.use((req, res) => {
  res.status(404).json({
    error: { code: "not_found", message: `No route for ${req.method} ${req.path}` },
  });
});

export default app;
