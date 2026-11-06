package com.reztek.whatifportfolio.ui.simulation

import com.reztek.whatifportfolio.data.engine.SimulationEngine
import com.reztek.whatifportfolio.data.local.SavedSimulationEntity
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.data.remote.dto.TimeSeriesPointDto
import com.reztek.whatifportfolio.ui.chart.ChartSeriesPoint

/** Maps persisted records to the comparison UI model without inventing timeline samples. */
object ComparisonSimulationMapper {
    fun fromRemote(dto: SimulationDto): ComparisonSimulation =
        ComparisonSimulation(
            id = "remote-${dto.id}",
            title = dto.name,
            dateRange = "${dto.startDate} → ${dto.endDate}",
            totalContributed = dto.totalContributed,
            finalValue = dto.finalValue,
            realValue = dto.realValue,
            percentReturn = dto.percentReturn,
            chartPoints = dto.timeSeries.orEmpty().map { it.toChartPoint() }
        )

    fun fromLocal(entity: SavedSimulationEntity): ComparisonSimulation {
        val totalContributed = entity.initialInvestment +
            (entity.monthlyContribution * entity.years * 12)
        return ComparisonSimulation(
            id = "local-${entity.id}",
            title = entity.title,
            dateRange = "Saved ${java.text.DateFormat.getDateInstance().format(java.util.Date(entity.createdAt))}",
            // Derived, not stored: same formula as SimulationEngine
            // (initial + monthly x 12 x years). Room keeps only the inputs.
            totalContributed = totalContributed,
            totalContributed = entity.initialInvestment +
                (entity.monthlyContribution * entity.years * 12),
            contributedIsDerived = true,
            finalValue = entity.finalNominalValue,
            realValue = entity.finalRealValue,
            percentReturn = percentReturn(entity.finalNominalValue, totalContributed),
            chartPoints = localTimeline(entity)
        )
    }

    private const val MAX_TIMELINE_YEARS = 200

    private fun percentReturn(finalValue: Double, totalContributed: Double): Double? {
        if (!finalValue.isFinite() || !totalContributed.isFinite() || totalContributed <= 0.0) return null
        return (finalValue - totalContributed) / totalContributed * 100.0
    }

    private fun localTimeline(entity: SavedSimulationEntity): List<ChartSeriesPoint> {
        if (entity.years < 1 || entity.years > MAX_TIMELINE_YEARS) return emptyList()
        val result = SimulationEngine.calculateSimulation(
            initialInvestment = entity.initialInvestment,
            monthlyContribution = entity.monthlyContribution,
            annualReturnRatePercent = entity.returnRate,
            annualInflationRatePercent = entity.inflationRate,
            timeHorizonYears = entity.years
        )
        val points = result.yearlyDataPoints.map {
            ChartSeriesPoint(
                date = "Year ${it.year}",
                nominalValue = it.nominalValue,
                realValue = it.realValue,
                contributed = it.totalContributions
            )
        }
        val valid = points.all {
            it.nominalValue.isFinite() && it.realValue?.isFinite() == true && it.contributed.isFinite()
        }
        return if (valid) points else emptyList()
    }

    private fun TimeSeriesPointDto.toChartPoint() = ChartSeriesPoint(
        date = date,
        nominalValue = nominalValue,
        realValue = realValue,
        contributed = contributed
    )
}
