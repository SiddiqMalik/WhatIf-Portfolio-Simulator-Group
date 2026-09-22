const ALPHA_VANTAGE_BASE = "https://www.alphavantage.co/query";
const COINGECKO_BASE = "https://api.coingecko.com/api/v3";

async function searchEquities(query) {
  const key = process.env.ALPHA_VANTAGE_KEY;
  if (!key) return [];
  const url = `${ALPHA_VANTAGE_BASE}?function=SYMBOL_SEARCH&keywords=${encodeURIComponent(query)}&apikey=${key}`;
  const res = await fetch(url);
  const data = await res.json();
  if (!data.bestMatches) {
    console.error("Alpha Vantage returned no matches:", data.Note || data.Information || data);
    return [];
  }
  return data.bestMatches.map((m) => ({
    symbol: m["1. symbol"],
    name: m["2. name"],
    type: "equity",
    region: m["4. region"],
    currency: m["8. currency"],
  }));
}

async function searchCrypto(query) {
  const key = process.env.COINGECKO_API_KEY;
  const url = `${COINGECKO_BASE}/search?query=${encodeURIComponent(query)}`;
  const headers = key ? { "x-cg-demo-api-key": key } : {};
  const res = await fetch(url, { headers });
  const data = await res.json();
  const coins = data.coins || [];
  return coins.slice(0, 10).map((c) => ({
    symbol: c.id,
    ticker: (c.symbol || "").toUpperCase(),
    name: c.name,
    type: "crypto",
  }));
}

export async function searchAssets(query, type) {
  const wantEquity = type === "all" || type === "equity";
  const wantCrypto = type === "all" || type === "crypto";
  const [equities, crypto] = await Promise.all([
    wantEquity ? searchEquities(query) : Promise.resolve([]),
    wantCrypto ? searchCrypto(query) : Promise.resolve([]),
  ]);
  return [...equities, ...crypto];
}
