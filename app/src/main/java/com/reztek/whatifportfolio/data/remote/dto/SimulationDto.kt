package com.reztek.whatifportfolio.data.remote.dto

data class SimulationDto(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val initialInvestment: Double,
    val recurringContribution: Double,
    val frequency: String,
    val status: String,
    val totalContributed: Double,
    val finalValue: Double,
    val profitLoss: Double,
    val percentReturn: Double,
    val realValue: Double?,
    val realProfitLoss: Double?,
    val realReturnPct: Double?,
    val createdAt: String,
    val updatedAt: String
)

data class SimulationListResponse(val data: List<SimulationDto>)

data class AllocationDto(
    val symbol: String,
    val type: String,
    val percent: Double
)

data class CreateSimulationRequest(
    val name: String,
    val startDate: String,
    val endDate: String? = null,
    val initialInvestment: Double,
    val recurringContribution: Double,
    val allocations: List<AllocationDto>
)
