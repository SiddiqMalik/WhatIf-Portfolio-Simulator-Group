import express from "express";
import { requireAuth, getDb } from "./firebase.js";
import { searchAssets } from "./assetSearch.js";
import { getAssetHistory } from "./assetHistory.js";
import { getCpiSeries } from "./cpiData.js";
import { createSimulation, listSimulations, getSimulation, updateSimulation, deleteSimulation } from "./simulations.js";

const app = express();
app.use(express.json());

app.get("/v1/health", (_req, res) => {
  res.json({ status: "ok", service: "whatif-api", time: new Date().toISOString() });
});

app.get("/v1/whoami", requireAuth, (req, res) => {
  res.json({ uid: req.uid });
});

app.get("/v1/cpi", async (req, res) => {
  try {
    const db = getDb();
    let rows = await getCpiSeries(db);
    const { from, to } = req.query;
    if (from) rows = rows.filter((r) => r.period >= from);
    if (to) rows = rows.filter((r) => r.period <= to);
    res.json({ base: "Dec 2024=100", data: rows });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: { code: "internal_error", message: "Failed to load CPI data" } });
  }
});

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

app.get("/v1/assets/:symbol/history", async (req, res) => {
  const { symbol } = req.params;
  const type = (req.query.type || "equity").toString();
  const interval = (req.query.interval || "daily").toString();
  const { from, to } = req.query;
  if (!["equity", "crypto"].includes(type)) {
    return res.status(400).json({ error: { code: "invalid_request", message: "type must be 'equity' or 'crypto'" } });
  }
  if (!["daily", "weekly", "monthly"].includes(interval)) {
    return res.status(400).json({ error: { code: "invalid_request", message: "interval must be 'daily', 'weekly' or 'monthly'" } });
  }
  if (type === "crypto" && interval !== "daily") {
    return res.status(400).json({ error: { code: "invalid_request", message: "crypto history is only available as 'daily' (past 365 days) on the free tier" } });
  }
  try {
    const series = await getAssetHistory(symbol, type, interval);
    let rows = series;
    if (from) rows = rows.filter((r) => r.date >= from);
    if (to) rows = rows.filter((r) => r.date <= to);
    res.json({ symbol, type, interval, data: rows });
  } catch (err) {
    console.error(err);
    res.status(502).json({ error: { code: "provider_error", message: "Failed to fetch historical price data" } });
  }
});

app.post("/v1/simulations", requireAuth, createSimulation);
app.get("/v1/simulations", requireAuth, listSimulations);
app.get("/v1/simulations/:id", requireAuth, getSimulation);
app.put("/v1/simulations/:id", requireAuth, updateSimulation);
app.delete("/v1/simulations/:id", requireAuth, deleteSimulation);

app.use((req, res) => {
  res.status(404).json({
    error: { code: "not_found", message: `No route for ${req.method} ${req.path}` },
  });
});

export default app;
