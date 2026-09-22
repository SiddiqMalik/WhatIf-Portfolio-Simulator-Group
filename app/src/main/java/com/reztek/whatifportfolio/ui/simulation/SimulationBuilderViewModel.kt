package com.reztek.whatifportfolio.ui.simulation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SimulationBuilderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onInitialInvestmentChanged(value: String) {
        _uiState.update { it.copy(initialInvestment = value) }
    }

    fun onRecurringContributionChanged(value: String) {
        _uiState.update { it.copy(recurringContribution = value) }
    }

    fun onFrequencyChanged(frequency: String) {
        _uiState.update { it.copy(frequency = frequency) }
    }

    fun onStartDateChanged(date: String) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun onEndDateChanged(date: String) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun addAssets(newAllocations: List<AllocationRow>) {
        _uiState.update { state ->
            val updated = state.allocations.toMutableList()
            newAllocations.forEach { alloc ->
                if (updated.none { it.symbol.equals(alloc.symbol, ignoreCase = true) }) {
                    updated.add(alloc)
                }
            }
            state.copy(allocations = updated)
        }
        recalculateAllocationTotal()
    }

    fun removeAllocation(symbol: String) {
        _uiState.update { state ->
            state.copy(allocations = state.allocations.filterNot { it.symbol.equals(symbol, ignoreCase = true) })
        }
        recalculateAllocationTotal()
    }

    fun onAllocationPercentChanged(symbol: String, percent: String) {
        _uiState.update { state ->
            val updated = state.allocations.map {
                if (it.symbol.equals(symbol, ignoreCase = true)) it.copy(percent = percent) else it
            }
            state.copy(allocations = updated)
        }
        recalculateAllocationTotal()
    }

    private fun recalculateAllocationTotal() {
        _uiState.update { state ->
            val total = state.allocations.sumOf { it.percent.toDoubleOrNull() ?: 0.0 }.toFloat()
            state.copy(allocationTotal = total)
        }
    }

    fun runSimulation() {
        // Run logic implementation
    }

    fun consumeCreatedId() {
        _uiState.update { it.copy(createdSimulationId = null) }
    }

    fun prefillFromDraft(
        name: String,
        initialInvestment: Double,
        recurringContribution: Double,
        frequency: String,
        startDate: String,
        endDate: String,
        allocations: List<Any>
    ) {
        _uiState.update {
            it.copy(
                name = name,
                initialInvestment = initialInvestment.toString(),
                recurringContribution = recurringContribution.toString(),
                frequency = frequency,
                startDate = startDate,
                endDate = endDate
            )
        }
    }
}