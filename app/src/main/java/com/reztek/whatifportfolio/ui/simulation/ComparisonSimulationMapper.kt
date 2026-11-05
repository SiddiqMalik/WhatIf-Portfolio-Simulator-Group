package com.reztek.whatifportfolio.ui.simulation

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

    fun fromLocal(entity: SavedSimulationEntity): ComparisonSimulation =
        ComparisonSimulation(
            id = "local-${entity.id}",
            title = entity.title,
            dateRange = "Saved ${java.text.DateFormat.getDateInstance().format(java.util.Date(entity.createdAt))}",
            // Derived, not stored: same formula as SimulationEngine
            // (initial + monthly x 12 x years). Room keeps only the inputs.
            totalContributed = entity.initialInvestment +
                (entity.monthlyContribution * entity.years * 12),
            contributedIsDerived = true,
            finalValue = entity.finalNominalValue,
            realValue = entity.finalRealValue,
            percentReturn = null,
            // Room currently stores only the final values, not annual points.
            chartPoints = emptyList()
        )

    private fun TimeSeriesPointDto.toChartPoint() = ChartSeriesPoint(
        date = date,
        nominalValue = nominalValue,
        realValue = realValue,
        contributed = contributed
    )
}
