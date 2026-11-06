export function findUncoveredAsset(assetSeries, startDate) {
  for (const [symbol, series] of Object.entries(assetSeries)) {
    const firstDate = series.length ? series[0].date : null;
    if (!firstDate || firstDate > startDate) {
      return { symbol, firstDate };
    }
  }
  return null;
}

export function uncoveredAssetMessage({ symbol, firstDate }, startDate) {
  if (!firstDate) {
    return `No historical price data is available for ${symbol}`;
  }
  return `${symbol} only has price history from ${firstDate}, which is after the chosen start date ${startDate}. Choose a start date on or after ${firstDate}.`;
}
