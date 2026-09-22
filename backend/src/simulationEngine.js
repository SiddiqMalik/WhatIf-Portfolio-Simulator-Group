function findPriceOnOrAfter(series, date) {
  return series.find((p) => p.date >= date);
}

function findPriceOnOrBefore(series, date) {
  let match = null;
  for (const p of series) {
    if (p.date <= date) match = p;
    else break;
  }
  return match;
}

function addMonths(dateStr, months) {
  const d = new Date(dateStr + "T00:00:00Z");
  d.setUTCMonth(d.getUTCMonth() + months);
  return d.toISOString().slice(0, 10);
}

function findCpiForMonth(cpiSeries, dateStr) {
  const period = dateStr.slice(0, 7);
  const exact = cpiSeries.find((c) => c.period === period);
  if (exact) return exact.index;
  let match = null;
  for (const c of cpiSeries) {
    if (c.period <= period) match = c;
    else break;
  }
  return match ? match.index : null;
}

// Pure calculation: given already-fetched price series and CPI data, run the DCA simulation.
export function runSimulation({ startDate, endDate, initialInvestment, recurringContribution, allocations, assetSeries, cpiSeries }) {
  const contributionDates = [{ date: startDate, isInitial: true }];
  let cursor = addMonths(startDate, 1);
  while (cursor <= endDate) {
    contributionDates.push({ date: cursor, isInitial: false });
    cursor = addMonths(cursor, 1);
  }

  const perAsset = allocations.map((alloc) => {
    const series = assetSeries[alloc.symbol];
    let units = 0;
    let contributed = 0;
    const events = [];
    for (const c of contributionDates) {
      const amount = (c.isInitial ? initialInvestment : recurringContribution) * (alloc.percent / 100);
      if (amount <= 0) continue;
      if (!series.length || c.date < series[0].date) continue; // before this asset's data starts
      const priceRow = findPriceOnOrAfter(series, c.date);
      if (!priceRow) continue; // beyond available data
      const boughtUnits = amount / priceRow.close;
      units += boughtUnits;
      contributed += amount;
      events.push({ date: c.date, amount, price: priceRow.close, units: boughtUnits });
    }
    return { symbol: alloc.symbol, type: alloc.type, percent: alloc.percent, units, contributed, events };
  });

  const totalContributed = perAsset.reduce((sum, a) => sum + a.contributed, 0);

  const finalValue = perAsset.reduce((sum, a) => {
    const series = assetSeries[a.symbol];
    const lastPrice = findPriceOnOrBefore(series, endDate);
    return sum + (lastPrice ? a.units * lastPrice.close : 0);
  }, 0);

  const profitLoss = finalValue - totalContributed;
  const nominalReturnPct = totalContributed > 0 ? (profitLoss / totalContributed) * 100 : 0;

  const cpiStart = findCpiForMonth(cpiSeries, startDate);
  const cpiEnd = findCpiForMonth(cpiSeries, endDate);
  let realValue = null, realProfitLoss = null, realReturnPct = null;
  if (cpiStart && cpiEnd) {
    realValue = finalValue * (cpiStart / cpiEnd);
    realProfitLoss = realValue - totalContributed;
    realReturnPct = totalContributed > 0 ? (realProfitLoss / totalContributed) * 100 : 0;
  }

  const timeSeries = [];
  const cumulativeUnits = {};
  let cumulativeContributed = 0;
  for (const c of contributionDates) {
    let nominalValue = 0;
    for (const a of perAsset) {
      const ev = a.events.find((e) => e.date === c.date);
      cumulativeUnits[a.symbol] = (cumulativeUnits[a.symbol] || 0) + (ev ? ev.units : 0);
      const series = assetSeries[a.symbol];
      const priceRow = findPriceOnOrAfter(series, c.date) || findPriceOnOrBefore(series, c.date);
      if (priceRow) nominalValue += (cumulativeUnits[a.symbol] || 0) * priceRow.close;
      if (ev) cumulativeContributed += ev.amount;
    }
    const cpiAtDate = findCpiForMonth(cpiSeries, c.date);
    const realAtDate = cpiStart && cpiAtDate ? nominalValue * (cpiStart / cpiAtDate) : null;
    timeSeries.push({ date: c.date, contributed: cumulativeContributed, nominalValue, realValue: realAtDate });
  }

  return {
    totalContributed, finalValue, profitLoss, nominalReturnPct,
    realValue, realProfitLoss, realReturnPct,
    perAsset: perAsset.map((a) => ({ symbol: a.symbol, type: a.type, percent: a.percent, units: a.units, contributed: a.contributed })),
    timeSeries,
  };
}
