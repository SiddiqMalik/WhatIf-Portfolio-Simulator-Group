package com.reztek.whatifportfolio.ui.simulation

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

    @Test
    fun localRecordUsesStableLocalIdAndDoesNotInventTimeline() {
        val entity = SavedSimulationEntity(
            id = 42L,
            title = "Local projection",
            initialInvestment = 1000.0,
            monthlyContribution = 100.0,
            returnRate = 8.0,
            inflationRate = 4.0,
            years = 2,
            finalNominalValue = 3800.0,
            finalRealValue = 3500.0,
            createdAt = 0L
        )

        val mapped = ComparisonSimulationMapper.fromLocal(entity)

        assertEquals("local-42", mapped.id)
        assertEquals("Local projection", mapped.title)
        assertEquals(3400.0, mapped.totalContributed, 0.001)
        assertEquals(3800.0, mapped.finalValue, 0.001)
        assertEquals(3500.0, mapped.realValue!!, 0.001)
        assertNull(mapped.percentReturn)
        assertTrue(mapped.chartPoints.isEmpty())
    }
}
