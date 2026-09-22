import { FieldValue } from "firebase-admin/firestore";
import { getDb } from "./firebase.js";
import { getAssetHistory } from "./assetHistory.js";
import { getCpiSeries } from "./cpiData.js";
import { runSimulation } from "./simulationEngine.js";

export async function createSimulation(req, res) {
  const db = getDb();
  const body = req.body || {};

  const errors = [];
  const name = typeof body.name === "string" ? body.name.trim() : "";
  if (!name) errors.push("name is required");
  const startDate = body.startDate;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(startDate || "")) errors.push("startDate must be YYYY-MM-DD");
  const initialInvestment = Number(body.initialInvestment);
  if (!(initialInvestment >= 0)) errors.push("initialInvestment must be a number >= 0");
  const recurringContribution = Number(body.recurringContribution);
  if (!(recurringContribution >= 0)) errors.push("recurringContribution must be a number >= 0");
  const allocations = Array.isArray(body.allocations) ? body.allocations : [];
  if (allocations.length === 0) errors.push("allocations must be a non-empty array");
  const percentSum = allocations.reduce((s, a) => s + (Number(a.percent) || 0), 0);
  if (allocations.length > 0 && Math.abs(percentSum - 100) > 0.5) errors.push("allocation percent values must sum to 100");
  for (const a of allocations) {
    if (!a.symbol || !["equity", "crypto"].includes(a.type)) {
      errors.push("each allocation needs a symbol and type of 'equity' or 'crypto'");
      break;
    }
  }
  if (errors.length > 0) {
    return res.status(400).json({ error: { code: "invalid_request", message: errors.join("; ") } });
  }

  try {
    const assetSeries = {};
    for (const a of allocations) {
      const interval = a.type === "crypto" ? "daily" : "monthly";
      assetSeries[a.symbol] = await getAssetHistory(a.symbol, a.type, interval);
    }

    const lastDates = Object.values(assetSeries).map((s) => s[s.length - 1]?.date).filter(Boolean);
    const endDate = /^\d{4}-\d{2}-\d{2}$/.test(body.endDate || "")
      ? [...lastDates, body.endDate].sort()[0]
      : lastDates.sort()[0];

    if (!endDate || endDate < startDate) {
      return res.status(400).json({ error: { code: "invalid_request", message: "No overlapping historical data available for the selected assets and date range" } });
    }

    const cpiSeries = await getCpiSeries(db);
    const result = runSimulation({ startDate, endDate, initialInvestment, recurringContribution, allocations, assetSeries, cpiSeries });

    if (result.totalContributed === 0) {
      return res.status(400).json({ error: { code: "invalid_request", message: "No historical price data available for the selected assets in this date range" } });
    }

    const docFields = {
      name, startDate, endDate, initialInvestment, recurringContribution,
      frequency: "monthly", allocations, status: "draft",
      totalContributed: result.totalContributed,
      finalValue: result.finalValue,
      profitLoss: result.profitLoss,
      percentReturn: result.nominalReturnPct,
      realValue: result.realValue,
      realProfitLoss: result.realProfitLoss,
      realReturnPct: result.realReturnPct,
      perAsset: result.perAsset,
      timeSeries: result.timeSeries,
    };

    const ref = await db.collection("users").doc(req.uid).collection("simulations").add({
      ...docFields,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });

    const now = new Date().toISOString();
    res.status(201).json({ id: ref.id, ...docFields, createdAt: now, updatedAt: now });
  } catch (err) {
    console.error(err);
    res.status(502).json({ error: { code: "provider_error", message: "Failed to run simulation" } });
  }
}
