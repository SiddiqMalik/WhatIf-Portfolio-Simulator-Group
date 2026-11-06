package com.reztek.whatifportfolio.ui.simulation

import com.reztek.whatifportfolio.data.engine.SimulationEngine
import com.reztek.whatifportfolio.data.local.SavedSimulationEntity
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.data.remote.dto.TimeSeriesPointDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparisonSimulationMapperTest {

    @Test
    fun remoteRecordMapsValuesAndTimeline() {
        val dto = SimulationDto(
            id = "abc-123",
            name = "Growth plan",
            startDate = "2026-01-01",
            endDate = "2030-01-01",
            initialInvestment = 1000.0,
            recurringContribution = 100.0,
            status = "completed",
            totalContributed = 5800.0,
            finalValue = 7200.0,
            profitLoss = 1400.0,
            percentReturn = 24.14,
            realValue = 6800.0,
            timeSeries = listOf(
                TimeSeriesPointDto(
                    date = "2026-01-01",
                    contributed = 1000.0,
                    nominalValue = 1000.0,
                    realValue = 980.0
                )
            ),
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )

        val mapped = ComparisonSimulationMapper.fromRemote(dto)

        assertEquals("remote-abc-123", mapped.id)
        assertEquals("Growth plan", mapped.title)
        assertEquals("2026-01-01 → 2030-01-01", mapped.dateRange)
        assertEquals(5800.0, mapped.totalContributed, 0.001)
        assertEquals(7200.0, mapped.finalValue, 0.001)
        assertEquals(6800.0, mapped.realValue!!, 0.001)
        assertEquals(24.14, mapped.percentReturn!!, 0.001)
        assertEquals(1, mapped.chartPoints.size)
        assertEquals("2026-01-01", mapped.chartPoints.first().date)
    }

    @Test
    fun remoteMissingTimelineStaysEmptyAndRealValueMayBeNull() {
        val dto = SimulationDto(
            id = "remote-only",
            name = "No points",
            startDate = "2026-01-01",
            endDate = "2027-01-01",
            initialInvestment = 100.0,
            recurringContribution = 0.0,
            status = "completed",
            totalContributed = 100.0,
            finalValue = 110.0,
            profitLoss = 10.0,
            percentReturn = 10.0,
            realValue = null,
            timeSeries = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )

        val mapped = ComparisonSimulationMapper.fromRemote(dto)

        assertTrue(mapped.chartPoints.isEmpty())
        assertNull(mapped.realValue)
    }

    private fun localEntity(
        years: Int,
        initial: Double = 1000.0,
        monthly: Double = 100.0,
        returnRate: Double = 8.0,
        inflation: Double = 4.0
    ): SavedSimulationEntity {
        val result = SimulationEngine.calculateSimulation(initial, monthly, returnRate, inflation, years)
        return SavedSimulationEntity(
            id = 42L,
            title = "Local projection",
            initialInvestment = initial,
            monthlyContribution = monthly,
            returnRate = returnRate,
            inflationRate = inflation,
            years = years,
            finalNominalValue = result.finalNominalValue,
            finalRealValue = result.finalRealValue,
            createdAt = 0L
        )
    }

    @Test
    fun localRecordRecomputesTimelineWithSimulationEngine() {
        val entity = localEntity(years = 2)
        val expected = SimulationEngine.calculateSimulation(1000.0, 100.0, 8.0, 4.0, 2)

        val mapped = ComparisonSimulationMapper.fromLocal(entity)

        assertEquals("local-42", mapped.id)
        assertEquals("Local projection", mapped.title)
        assertEquals(3400.0, mapped.totalContributed, 0.001)
        assertEquals(entity.finalNominalValue, mapped.finalValue, 0.001)
        assertEquals(entity.finalRealValue, mapped.realValue!!, 0.001)
        assertEquals(3, mapped.chartPoints.size)
        assertEquals("Year 0", mapped.chartPoints.first().date)
        assertEquals(1000.0, mapped.chartPoints.first().nominalValue, 0.001)
        assertEquals(expected.finalNominalValue, mapped.chartPoints.last().nominalValue, 0.001)
        assertEquals(expected.finalRealValue, mapped.chartPoints.last().realValue!!, 0.001)
        assertEquals(3400.0, mapped.chartPoints.last().contributed, 0.001)
    }

    @Test
    fun localReturnPercentMatchesFinalValueAgainstContributions() {
        val entity = localEntity(years = 5)
        val contributed = 1000.0 + 100.0 * 12 * 5

        val mapped = ComparisonSimulationMapper.fromLocal(entity)

        val expected = (entity.finalNominalValue - contributed) / contributed * 100.0
        assertEquals(expected, mapped.percentReturn!!, 0.0001)
        assertTrue(mapped.percentReturn!! > 0.0)
    }

    @Test
    fun localNegativeReturnGivesNegativePercentAndFallingTimeline() {
        val entity = localEntity(years = 3, initial = 5000.0, monthly = 0.0, returnRate = -10.0)

        val mapped = ComparisonSimulationMapper.fromLocal(entity)

        assertTrue(mapped.percentReturn!! < 0.0)
        assertEquals(4, mapped.chartPoints.size)
        assertTrue(mapped.chartPoints.last().nominalValue < mapped.chartPoints.first().nominalValue)
    }

    @Test
    fun localDifferentDurationsProduceTimelinesOfTheirOwnLength() {
        val short = ComparisonSimulationMapper.fromLocal(localEntity(years = 1))
        val long = ComparisonSimulationMapper.fromLocal(localEntity(years = 10))

        assertEquals(2, short.chartPoints.size)
        assertEquals(11, long.chartPoints.size)
    }

    @Test
    fun localWithNoYearsHasNoTimelineInsteadOfASinglePointChart() {
        val mapped = ComparisonSimulationMapper.fromLocal(localEntity(years = 0))

        assertTrue(mapped.chartPoints.isEmpty())
    }

    @Test
    fun localWithNoContributionsHasNoReturnPercent() {
        val mapped = ComparisonSimulationMapper.fromLocal(
            localEntity(years = 2, initial = 0.0, monthly = 0.0)
        )

        assertNull(mapped.percentReturn)
    }

    @Test
    fun localWithInvalidNumbersDropsTheTimelineAndReturn() {
        val entity = localEntity(years = 2).copy(returnRate = Double.NaN, finalNominalValue = Double.NaN)

        val mapped = ComparisonSimulationMapper.fromLocal(entity)

        assertTrue(mapped.chartPoints.isEmpty())
        assertNull(mapped.percentReturn)
    }

    @Test
    fun localTotalContributed_isMarkedDerived_remoteIsNot() {
        val local = ComparisonSimulationMapper.fromLocal(
            SavedSimulationEntity(
                id = 1L,
                title = "Local",
                initialInvestment = 500.0,
                monthlyContribution = 50.0,
                returnRate = 7.0,
                inflationRate = 4.0,
                years = 1,
                finalNominalValue = 1200.0,
                finalRealValue = 1150.0,
                createdAt = 0L
            )
        )
        val remote = ComparisonSimulationMapper.fromRemote(
            SimulationDto(
                id = "r1",
                name = "Remote",
                startDate = "2026-01-01",
                endDate = "2027-01-01",
                initialInvestment = 500.0,
                recurringContribution = 50.0,
                status = "completed",
                totalContributed = 1100.0,
                finalValue = 1200.0,
                profitLoss = 100.0,
                percentReturn = 9.09,
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-01T00:00:00Z"
            )
        )

        assertEquals(1100.0, local.totalContributed, 0.001)
        assertTrue(local.contributedIsDerived)
        assertTrue(!remote.contributedIsDerived)
    }

    @Test
    fun localTotalContributed_isMarkedDerived_remoteIsNot() {
        val local = ComparisonSimulationMapper.fromLocal(
            SavedSimulationEntity(
                id = 1L,
                title = "Local",
                initialInvestment = 500.0,
                monthlyContribution = 50.0,
                returnRate = 7.0,
                inflationRate = 4.0,
                years = 1,
                finalNominalValue = 1200.0,
                finalRealValue = 1150.0,
                createdAt = 0L
            )
        )
        val remote = ComparisonSimulationMapper.fromRemote(
            SimulationDto(
                id = "r1",
                name = "Remote",
                startDate = "2026-01-01",
                endDate = "2027-01-01",
                initialInvestment = 500.0,
                recurringContribution = 50.0,
                status = "completed",
                totalContributed = 1100.0,
                finalValue = 1200.0,
                profitLoss = 100.0,
                percentReturn = 9.09,
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-01T00:00:00Z"
            )
        )

        assertEquals(1100.0, local.totalContributed, 0.001)
        assertTrue(local.contributedIsDerived)
        assertTrue(!remote.contributedIsDerived)
    }
}
