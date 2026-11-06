package com.reztek.whatifportfolio.ui.simulation

import kotlin.math.abs

enum class ComparisonWinner { FIRST, SECOND, TIE, NONE }

enum class ComparisonMetric { TOTAL_CONTRIBUTED, FINAL_VALUE, REAL_VALUE, RETURN }

data class ComparisonSummary(
    val basis: ComparisonMetric,
    val winner: ComparisonWinner,
    val amount: Double
)

object ComparisonHighlights {
    const val EQUAL_TOLERANCE = 0.005

    fun winner(first: Double?, second: Double?): ComparisonWinner {
        val difference = difference(first, second) ?: return ComparisonWinner.NONE
        return when {
            abs(difference) < EQUAL_TOLERANCE -> ComparisonWinner.TIE
            difference < 0 -> ComparisonWinner.FIRST
            else -> ComparisonWinner.SECOND
        }
    }

    fun difference(first: Double?, second: Double?): Double? {
        if (first == null || second == null || !first.isFinite() || !second.isFinite()) return null
        return second - first
    }

    fun winnerFor(metric: ComparisonMetric, first: ComparisonSimulation, second: ComparisonSimulation): ComparisonWinner =
        when (metric) {
            ComparisonMetric.TOTAL_CONTRIBUTED -> ComparisonWinner.NONE
            ComparisonMetric.FINAL_VALUE -> winner(first.finalValue, second.finalValue)
            ComparisonMetric.REAL_VALUE -> winner(first.realValue, second.realValue)
            ComparisonMetric.RETURN -> winner(first.percentReturn, second.percentReturn)
        }

    fun differenceFor(metric: ComparisonMetric, first: ComparisonSimulation, second: ComparisonSimulation): Double? =
        when (metric) {
            ComparisonMetric.TOTAL_CONTRIBUTED -> difference(first.totalContributed, second.totalContributed)
            ComparisonMetric.FINAL_VALUE -> difference(first.finalValue, second.finalValue)
            ComparisonMetric.REAL_VALUE -> difference(first.realValue, second.realValue)
            ComparisonMetric.RETURN -> difference(first.percentReturn, second.percentReturn)
        }

    fun summary(first: ComparisonSimulation, second: ComparisonSimulation): ComparisonSummary? {
        val real = winnerFor(ComparisonMetric.REAL_VALUE, first, second)
        if (real != ComparisonWinner.NONE) {
            return ComparisonSummary(
                ComparisonMetric.REAL_VALUE,
                real,
                abs(differenceFor(ComparisonMetric.REAL_VALUE, first, second) ?: 0.0)
            )
        }
        val final = winnerFor(ComparisonMetric.FINAL_VALUE, first, second)
        if (final != ComparisonWinner.NONE) {
            return ComparisonSummary(
                ComparisonMetric.FINAL_VALUE,
                final,
                abs(differenceFor(ComparisonMetric.FINAL_VALUE, first, second) ?: 0.0)
            )
        }
        return null
    }
}
