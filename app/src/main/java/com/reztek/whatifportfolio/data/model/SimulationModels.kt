package com.reztek.whatifportfolio.data.model

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