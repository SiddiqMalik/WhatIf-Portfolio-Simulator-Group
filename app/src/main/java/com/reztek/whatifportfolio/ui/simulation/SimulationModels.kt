package com.reztek.whatifportfolio.ui.simulation

data class AllocationRow(
    val symbol: String,
    val name: String,
    val percent: String
)

data class SimulationUiState(
    val name: String = "",
    val initialInvestment: String = "10000",
    val recurringContribution: String = "500",
    val frequency: String = "monthly",
    val startDate: String = "",
    val endDate: String = "",
    val allocations: List<AllocationRow> = emptyList(),
    val allocationTotal: Float = 0f,
    val isValid: Boolean = true,
    val isRunning: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val allocationError: String? = null,
    val runError: String? = null,
    val createdSimulationId: String? = null
)