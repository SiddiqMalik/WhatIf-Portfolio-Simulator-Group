import { getDb } from "./firebase.js";

const ALPHA_VANTAGE_BASE = "https://www.alphavantage.co/query";
const COINGECKO_BASE = "https://api.coingecko.com/api/v3";

function cacheDocId(type, symbol, interval) {
  const safeSymbol = symbol.replace(/[^A-Za-z0-9._-]/g, "_");
  return `${type}_${safeSymbol}_${interval}`;
}

// Note: Alpha Vantage's free tier only returns the last ~100 trading days for
// daily data (outputsize=full is now a premium feature). Weekly and monthly
// are unaffected and return full history, so use those for longer simulations.
async function fetchEquityHistory(symbol, interval) {
  const key = process.env.ALPHA_VANTAGE_KEY;
  const functionMap = {
    daily: "TIME_SERIES_DAILY",
    weekly: "TIME_SERIES_WEEKLY",
    monthly: "TIME_SERIES_MONTHLY",
  };
  const seriesKeyMap = {
    daily: "Time Series (Daily)",
    weekly: "Weekly Time Series",
    monthly: "Monthly Time Series",
  };
  const url = `${ALPHA_VANTAGE_BASE}?function=${functionMap[interval]}&symbol=${encodeURIComponent(symbol)}&apikey=${key}`;
  const res = await fetch(url);
  const data = await res.json();
  const seriesObj = data[seriesKeyMap[interval]];
  if (!seriesObj) {
    throw new Error(data.Note || data.Information || data["Error Message"] || "No data returned from Alpha Vantage");
  }
  return Object.entries(seriesObj)
    .map(([date, v]) => ({ date, close: parseFloat(v["4. close"]) }))
    .sort((a, b) => (a.date < b.date ? -1 : 1));
}

async function fetchCryptoHistory(symbol) {
  const key = process.env.COINGECKO_API_KEY;
  const now = Math.floor(Date.now() / 1000);
  const yearAgo = now - 365 * 24 * 60 * 60;
  const url = `${COINGECKO_BASE}/coins/${encodeURIComponent(symbol)}/market_chart/range?vs_currency=usd&from=${yearAgo}&to=${now}`;
  const headers = key ? { "x-cg-demo-api-key": key } : {};
  const res = await fetch(url, { headers });
  const data = await res.json();
  if (!data.prices) {
    throw new Error(data.error || "No data returned from CoinGecko");
  }
  return data.prices
    .map(([ms, price]) => ({ date: new Date(ms).toISOString().slice(0, 10), close: price }))
    .sort((a, b) => (a.date < b.date ? -1 : 1));
}

export async function getAssetHistory(symbol, type, interval) {
  const db = getDb();
  const ref = db.collection("priceHistory").doc(cacheDocId(type, symbol, interval));
  const snap = await ref.get();

  if (snap.exists) {
    return snap.data().series;
  }

  const series = type === "crypto" ? await fetchCryptoHistory(symbol) : await fetchEquityHistory(symbol, interval);

  await ref.set({
    symbol,
    type,
    interval,
    fetchedAt: new Date().toISOString(),
    series,
  });

  return series;
}
