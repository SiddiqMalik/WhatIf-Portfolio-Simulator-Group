package com.reztek.whatifportfolio.ui.simulation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparisonHighlightsTest {

    private fun simulation(
        title: String = "Sim",
        contributed: Double = 1000.0,
        finalValue: Double = 1500.0,
        realValue: Double? = 1400.0,
        percentReturn: Double? = 50.0
    ) = ComparisonSimulation(
        id = title,
        title = title,
        dateRange = "",
        totalContributed = contributed,
        finalValue = finalValue,
        realValue = realValue,
        percentReturn = percentReturn,
        chartPoints = emptyList()
    )

    @Test
    fun higherFirstValueWinsForFirst() {
        assertEquals(ComparisonWinner.FIRST, ComparisonHighlights.winner(200.0, 100.0))
    }

    @Test
    fun higherSecondValueWinsForSecond() {
        assertEquals(ComparisonWinner.SECOND, ComparisonHighlights.winner(100.0, 200.0))
    }

    @Test
    fun equalValuesAreATieAndNeverAWinner() {
        assertEquals(ComparisonWinner.TIE, ComparisonHighlights.winner(100.0, 100.0))
        assertEquals(ComparisonWinner.TIE, ComparisonHighlights.winner(100.0, 100.004))
    }

    @Test
    fun differenceOfAFullCentIsNotATie() {
        assertEquals(ComparisonWinner.SECOND, ComparisonHighlights.winner(100.0, 100.01))
    }

    @Test
    fun missingOrInvalidValuesProduceNoWinner() {
        assertEquals(ComparisonWinner.NONE, ComparisonHighlights.winner(null, 100.0))
        assertEquals(ComparisonWinner.NONE, ComparisonHighlights.winner(100.0, null))
        assertEquals(ComparisonWinner.NONE, ComparisonHighlights.winner(Double.NaN, 100.0))
        assertEquals(ComparisonWinner.NONE, ComparisonHighlights.winner(100.0, Double.POSITIVE_INFINITY))
    }

    @Test
    fun negativeReturnsRankCorrectly() {
        assertEquals(ComparisonWinner.FIRST, ComparisonHighlights.winner(-5.0, -20.0))
        assertEquals(ComparisonWinner.SECOND, ComparisonHighlights.winner(-5.0, 3.0))
    }

    @Test
    fun differenceIsSecondMinusFirst() {
        assertEquals(-50.0, ComparisonHighlights.difference(150.0, 100.0)!!, 0.0001)
        assertEquals(50.0, ComparisonHighlights.difference(100.0, 150.0)!!, 0.0001)
        assertNull(ComparisonHighlights.difference(null, 1.0))
    }

    @Test
    fun totalContributedIsNeverHighlighted() {
        val first = simulation(contributed = 500.0)
        val second = simulation(contributed = 5000.0)

        assertEquals(ComparisonWinner.NONE, ComparisonHighlights.winnerFor(ComparisonMetric.TOTAL_CONTRIBUTED, first, second))
        assertEquals(4500.0, ComparisonHighlights.differenceFor(ComparisonMetric.TOTAL_CONTRIBUTED, first, second)!!, 0.0001)
    }

    @Test
    fun eachPerformanceMetricIsJudgedOnItsOwnValue() {
        val first = simulation(finalValue = 2000.0, realValue = 1000.0, percentReturn = 10.0)
        val second = simulation(finalValue = 1500.0, realValue = 1200.0, percentReturn = 10.0)

        assertEquals(ComparisonWinner.FIRST, ComparisonHighlights.winnerFor(ComparisonMetric.FINAL_VALUE, first, second))
        assertEquals(ComparisonWinner.SECOND, ComparisonHighlights.winnerFor(ComparisonMetric.REAL_VALUE, first, second))
        assertEquals(ComparisonWinner.TIE, ComparisonHighlights.winnerFor(ComparisonMetric.RETURN, first, second))
    }

    @Test
    fun missingReturnOnOneSideGivesNoReturnWinner() {
        val first = simulation(percentReturn = null)
        val second = simulation(percentReturn = 12.0)

        assertEquals(ComparisonWinner.NONE, ComparisonHighlights.winnerFor(ComparisonMetric.RETURN, first, second))
        assertNull(ComparisonHighlights.differenceFor(ComparisonMetric.RETURN, first, second))
    }

    @Test
    fun summaryPrefersRealValue() {
        val first = simulation(finalValue = 3000.0, realValue = 1000.0)
        val second = simulation(finalValue = 2000.0, realValue = 1800.0)

        val summary = ComparisonHighlights.summary(first, second)!!

        assertEquals(ComparisonMetric.REAL_VALUE, summary.basis)
        assertEquals(ComparisonWinner.SECOND, summary.winner)
        assertEquals(800.0, summary.amount, 0.0001)
    }

    @Test
    fun summaryFallsBackToFinalValueWhenRealValueIsMissing() {
        val first = simulation(finalValue = 3000.0, realValue = null)
        val second = simulation(finalValue = 2000.0, realValue = 1800.0)

        val summary = ComparisonHighlights.summary(first, second)!!

        assertEquals(ComparisonMetric.FINAL_VALUE, summary.basis)
        assertEquals(ComparisonWinner.FIRST, summary.winner)
        assertEquals(1000.0, summary.amount, 0.0001)
    }

    @Test
    fun summaryReportsATieWhenRealValuesMatch() {
        val summary = ComparisonHighlights.summary(
            simulation(realValue = 900.0),
            simulation(realValue = 900.0)
        )!!

        assertEquals(ComparisonWinner.TIE, summary.winner)
    }

    @Test
    fun summaryIsNullWhenNothingCanBeCompared() {
        val first = simulation(finalValue = Double.NaN, realValue = null)
        val second = simulation(finalValue = 100.0, realValue = null)

        assertNull(ComparisonHighlights.summary(first, second))
        assertTrue(ComparisonHighlights.winnerFor(ComparisonMetric.FINAL_VALUE, first, second) == ComparisonWinner.NONE)
    }
}
