package com.reztek.whatifportfolio.data.engine

import kotlin.math.pow

data class SimulationPoint(
    val year: Int,
    val nominalValue: Double,
    val realValue: Double,
    val totalContributions: Double
)

data class SimulationResult(
    val finalNominalValue: Double,
    val finalRealValue: Double,
    val totalContributions: Double,
    val totalInterestEarned: Double,
    val yearlyDataPoints: List<SimulationPoint>
)

object SimulationEngine {

    fun calculateSimulation(
        initialInvestment: Double,
        monthlyContribution: Double,
        annualReturnRatePercent: Double,
        annualInflationRatePercent: Double,
        timeHorizonYears: Int
    ): SimulationResult {
        val monthlyReturnRate = (annualReturnRatePercent / 100.0) / 12.0
        val monthlyInflationRate = (annualInflationRatePercent / 100.0) / 12.0

        val dataPoints = mutableListOf<SimulationPoint>()

        var currentNominal = initialInvestment
        var currentReal = initialInvestment
        var totalContributed = initialInvestment

        dataPoints.add(
            SimulationPoint(
                year = 0,
                nominalValue = currentNominal,
                realValue = currentReal,
                totalContributions = totalContributed
            )
        )

        for (year in 1..timeHorizonYears) {
            for (month in 1..12) {
                currentNominal = (currentNominal + monthlyContribution) * (1.0 + monthlyReturnRate)
                totalContributed += monthlyContribution
            }

            val cumulativeInflationFactor = (1.0 + annualInflationRatePercent / 100.0).pow(year)
            currentReal = currentNominal / cumulativeInflationFactor

            dataPoints.add(
                SimulationPoint(
                    year = year,
                    nominalValue = currentNominal,
                    realValue = currentReal,
                    totalContributions = totalContributed
                )
            )
        }

        return SimulationResult(
            finalNominalValue = currentNominal,
            finalRealValue = currentReal,
            totalContributions = totalContributed,
            totalInterestEarned = currentNominal - totalContributed,
            yearlyDataPoints = dataPoints
        )
    }
}