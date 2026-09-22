package com.reztek.whatifportfolio.data.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulationEngineTest {

    private val delta = 0.01 // cents, given the sizes of values here

    @Test
    fun calculateSimulation_withNoGrowthOrContributions_returnsInitialValueUnchanged() {
        val result = SimulationEngine.calculateSimulation(
            initialInvestment = 1000.0,
            monthlyContribution = 0.0,
            annualReturnRatePercent = 0.0,
            annualInflationRatePercent = 0.0,
            timeHorizonYears = 1
        )

        assertEquals(1000.0, result.finalNominalValue, delta)
        assertEquals(1000.0, result.finalRealValue, delta)
        assertEquals(1000.0, result.totalContributions, delta)
        assertEquals(0.0, result.totalInterestEarned, delta)
    }

    @Test
    fun calculateSimulation_withZeroYears_returnsOnlyTheStartingDataPoint() {
        val result = SimulationEngine.calculateSimulation(
            initialInvestment = 5000.0,
            monthlyContribution = 200.0,
            annualReturnRatePercent = 7.0,
            annualInflationRatePercent = 4.0,
            timeHorizonYears = 0
        )

        assertEquals(1, result.yearlyDataPoints.size)
        assertEquals(5000.0, result.finalNominalValue, delta)
        assertEquals(5000.0, result.totalContributions, delta)
    }

    @Test
    fun calculateSimulation_withMonthlyContributionsOnly_accumulatesTotalContributions() {
        // 0% return/inflation isolates the contribution total from any growth.
        val result = SimulationEngine.calculateSimulation(
            initialInvestment = 0.0,
            monthlyContribution = 1000.0,
            annualReturnRatePercent = 12.0,
            annualInflationRatePercent = 0.0,
            timeHorizonYears = 1
        )

        assertEquals(12000.0, result.totalContributions, delta)
        assertEquals(12809.33, result.finalNominalValue, delta)
        assertEquals(809.33, result.totalInterestEarned, delta)
    }

    @Test
    fun calculateSimulation_withInflation_realValueIsLowerThanNominal() {
        val result = SimulationEngine.calculateSimulation(
            initialInvestment = 10000.0,
            monthlyContribution = 0.0,
            annualReturnRatePercent = 0.0,
            annualInflationRatePercent = 5.0,
            timeHorizonYears = 10
        )

        assertEquals(10000.0, result.finalNominalValue, delta)
        assertEquals(6139.13, result.finalRealValue, delta)
        assertTrue(result.finalRealValue < result.finalNominalValue)
    }

    @Test
    fun calculateSimulation_withZeroInflation_realValueEqualsNominalValue() {
        val result = SimulationEngine.calculateSimulation(
            initialInvestment = 500.0,
            monthlyContribution = 200.0,
            annualReturnRatePercent = 0.0,
            annualInflationRatePercent = 0.0,
            timeHorizonYears = 2
        )

        assertEquals(5300.0, result.totalContributions, delta)
        assertEquals(result.finalNominalValue, result.finalRealValue, delta)
    }

    @Test
    fun calculateSimulation_generatesOneDataPointPerYearPlusStartingPoint() {
        val result = SimulationEngine.calculateSimulation(
            initialInvestment = 1000.0,
            monthlyContribution = 500.0,
            annualReturnRatePercent = 8.0,
            annualInflationRatePercent = 3.0,
            timeHorizonYears = 3
        )

        assertEquals(4, result.yearlyDataPoints.size) // year 0 through year 3
        assertEquals(listOf(0, 1, 2, 3), result.yearlyDataPoints.map { it.year })
    }

    @Test
    fun calculateSimulation_withPositiveGrowth_nominalValueIncreasesEachYear() {
        val result = SimulationEngine.calculateSimulation(
            initialInvestment = 1000.0,
            monthlyContribution = 500.0,
            annualReturnRatePercent = 8.0,
            annualInflationRatePercent = 3.0,
            timeHorizonYears = 3
        )

        val nominalValues = result.yearlyDataPoints.map { it.nominalValue }
        for (i in 1 until nominalValues.size) {
            assertTrue(nominalValues[i] > nominalValues[i - 1])
        }
    }

    @Test
    fun calculateSimulation_totalInterestEarned_equalsNominalValueMinusContributions() {
        val result = SimulationEngine.calculateSimulation(
            initialInvestment = 2000.0,
            monthlyContribution = 300.0,
            annualReturnRatePercent = 6.0,
            annualInflationRatePercent = 4.0,
            timeHorizonYears = 5
        )

        assertEquals(
            result.finalNominalValue - result.totalContributions,
            result.totalInterestEarned,
            delta
        )
    }
}
