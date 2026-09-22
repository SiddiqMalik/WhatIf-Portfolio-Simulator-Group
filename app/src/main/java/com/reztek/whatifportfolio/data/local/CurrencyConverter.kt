package com.reztek.whatifportfolio.data

enum class Currency(val symbol: String, val rateToUSD: Double) {
    USD("$", 1.0),
    ZAR("R", 18.25),
    EUR("€", 0.92),
    GBP("£", 0.78)
}

object CurrencyConverter {
    fun convert(amount: Double, from: Currency, to: Currency): Double {
        if (from == to) return amount
        val amountInUSD = amount / from.rateToUSD
        return amountInUSD * to.rateToUSD
    }

    fun format(amount: Double, currency: Currency): String {
        return "${currency.symbol}${String.format("%.2f", amount)}"
    }
}