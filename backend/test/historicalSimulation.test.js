import test from "node:test";
import assert from "node:assert/strict";
import { runSimulation } from "../src/simulationEngine.js";
import { findUncoveredAsset, uncoveredAssetMessage } from "../src/seriesCoverage.js";

const cpi = [
  { period: "2024-01", index: 100 },
  { period: "2024-12", index: 110 },
];

test("monthly backtest buys at the first price on or after each contribution date", () => {
  const result = runSimulation({
    startDate: "2024-01-01",
    endDate: "2024-03-01",
    initialInvestment: 1000,
    recurringContribution: 100,
    frequency: "monthly",
    allocations: [{ symbol: "AAA", type: "equity", percent: 100 }],
    assetSeries: {
      AAA: [
        { date: "2023-12-29", close: 9 },
        { date: "2024-01-31", close: 10 },
        { date: "2024-02-29", close: 20 },
        { date: "2024-03-29", close: 40 },
      ],
    },
    cpiSeries: cpi,
  });
  assert.equal(result.totalContributed, 1200);
  const expectedUnits = 1000 / 10 + 100 / 20 + 100 / 40;
  assert.ok(Math.abs(result.perAsset[0].units - expectedUnits) < 1e-9);
  assert.ok(Math.abs(result.finalValue - expectedUnits * 20) < 1e-9);
  assert.equal(result.timeSeries.length, 3);
});

test("allocations split contributions by percent", () => {
  const result = runSimulation({
    startDate: "2024-01-01",
    endDate: "2024-01-15",
    initialInvestment: 1000,
    recurringContribution: 0,
    allocations: [
      { symbol: "AAA", type: "equity", percent: 60 },
      { symbol: "BBB", type: "equity", percent: 40 },
    ],
    assetSeries: {
      AAA: [{ date: "2023-12-29", close: 10 }, { date: "2024-01-02", close: 10 }],
      BBB: [{ date: "2023-12-29", close: 20 }, { date: "2024-01-02", close: 20 }],
    },
    cpiSeries: cpi,
  });
  assert.equal(result.perAsset[0].contributed, 600);
  assert.equal(result.perAsset[1].contributed, 400);
  assert.equal(result.totalContributed, 1000);
});

test("contributions before an asset's first price are skipped by the engine", () => {
  const result = runSimulation({
    startDate: "2024-01-01",
    endDate: "2024-03-01",
    initialInvestment: 1000,
    recurringContribution: 0,
    allocations: [{ symbol: "AAA", type: "equity", percent: 100 }],
    assetSeries: { AAA: [{ date: "2024-02-15", close: 10 }] },
    cpiSeries: cpi,
  });
  assert.equal(result.totalContributed, 0);
});

test("coverage check flags an asset whose history starts after the start date", () => {
  const uncovered = findUncoveredAsset(
    { AAA: [{ date: "2023-12-29", close: 1 }], BTC: [{ date: "2025-06-01", close: 1 }] },
    "2024-01-01"
  );
  assert.deepEqual(uncovered, { symbol: "BTC", firstDate: "2025-06-01" });
  assert.match(uncoveredAssetMessage(uncovered, "2024-01-01"), /BTC only has price history from 2025-06-01/);
});

test("coverage check accepts history that begins on or before the start date", () => {
  assert.equal(findUncoveredAsset({ AAA: [{ date: "2023-12-29", close: 1 }] }, "2024-01-01"), null);
  assert.equal(findUncoveredAsset({ AAA: [{ date: "2024-01-01", close: 1 }] }, "2024-01-01"), null);
});

test("coverage check flags an empty series", () => {
  const uncovered = findUncoveredAsset({ AAA: [] }, "2024-01-01");
  assert.deepEqual(uncovered, { symbol: "AAA", firstDate: null });
  assert.match(uncoveredAssetMessage(uncovered, "2024-01-01"), /No historical price data/);
});
