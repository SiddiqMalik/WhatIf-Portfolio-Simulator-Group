package com.reztek.whatifportfolio.ui.simulation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reztek.whatifportfolio.data.remote.dto.AllocationDto
import com.reztek.whatifportfolio.data.remote.dto.AssetSearchResultDto
import com.reztek.whatifportfolio.data.remote.dto.CreateSimulationRequest
import com.reztek.whatifportfolio.data.repository.RemoteSimulationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AllocationRow(
    val symbol: String,
    val type: String,
    val name: String,
    val percent: String = "0"
)

data class SimulationBuilderUiState(
    val name: String = "",
    val initialInvestment: String = "10000",
    val recurringContribution: String = "500",
    val frequency: String = "monthly",
    val startDate: String = LocalDate.now().minusYears(5).toString(),
    val endDate: String = LocalDate.now().toString(),
    val allocations: List<AllocationRow> = emptyList(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val allocationError: String? = null,
    val isRunning: Boolean = false,
    val runError: String? = null,
    val createdSimulationId: String? = null
) {
    val allocationTotal: Double
        get() = allocations.sumOf { it.percent.toDoubleOrNull() ?: 0.0 }

    val isValid: Boolean
        get() = fieldErrors.isEmpty() &&
                allocationError == null &&
                allocations.isNotEmpty() &&
                kotlin.math.abs(allocationTotal - 100.0) <= 0.5 &&
                (initialInvestment.toDoubleOrNull() ?: -1.0) > 0 &&
                (recurringContribution.toDoubleOrNull() ?: -1.0) >= 0 &&
                name.isNotBlank()
}

data class AssetSelectorUiState(
    val query: String = "",
    val filter: String = "all",
    val results: List<AssetSearchResultDto> = emptyList(),
    val selected: Set<String> = emptySet(),
    val isSearching: Boolean = false,
    val error: String? = null
)

class SimulationBuilderViewModel(
    private val repository: RemoteSimulationRepository = RemoteSimulationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimulationBuilderUiState())
    val uiState: StateFlow<SimulationBuilderUiState> = _uiState.asStateFlow()

    private val _assetSelector = MutableStateFlow(AssetSelectorUiState())
    val assetSelector: StateFlow<AssetSelectorUiState> = _assetSelector.asStateFlow()

    private var searchJob: Job? = null

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, fieldErrors = it.fieldErrors - "name", runError = null) }
        revalidate()
    }

    fun onInitialInvestmentChanged(value: String) {
        _uiState.update { it.copy(initialInvestment = value, fieldErrors = it.fieldErrors - "initial", runError = null) }
        revalidate()
    }

    fun onRecurringContributionChanged(value: String) {
        _uiState.update {
            it.copy(recurringContribution = value, fieldErrors = it.fieldErrors - "contribution", runError = null)
        }
        revalidate()
    }

    fun onFrequencyChanged(value: String) {
        _uiState.update { it.copy(frequency = value) }
    }

    fun onStartDateChanged(value: String) {
        _uiState.update { it.copy(startDate = value, fieldErrors = it.fieldErrors - "dates", runError = null) }
        revalidate()
    }

    fun onEndDateChanged(value: String) {
        _uiState.update { it.copy(endDate = value, fieldErrors = it.fieldErrors - "dates", runError = null) }
        revalidate()
    }

    fun onAllocationPercentChanged(symbol: String, percent: String) {
        _uiState.update { state ->
            state.copy(
                allocations = state.allocations.map {
                    if (it.symbol == symbol) it.copy(percent = percent.filter { ch -> ch.isDigit() || ch == '.' })
                    else it
                },
                runError = null
            )
        }
        revalidate()
    }

    fun removeAllocation(symbol: String) {
        _uiState.update { state ->
            state.copy(allocations = state.allocations.filterNot { it.symbol == symbol })
        }
        revalidate()
    }

    fun addAssets(assets: List<AssetSearchResultDto>) {
        _uiState.update { state ->
            val existing = state.allocations.map { it.symbol }.toSet()
            val additions = assets
                .filter { it.symbol !in existing }
                .map {
                    AllocationRow(
                        symbol = it.symbol,
                        type = it.type,
                        name = it.name,
                        percent = "0"
                    )
                }
            state.copy(allocations = state.allocations + additions)
        }
        revalidate()
    }

    fun prefillFromDraft(
        name: String,
        initialInvestment: Double,
        recurringContribution: Double,
        frequency: String,
        startDate: String,
        endDate: String,
        allocations: List<AllocationDto>
    ) {
        _uiState.value = SimulationBuilderUiState(
            name = name,
            initialInvestment = initialInvestment.toString(),
            recurringContribution = recurringContribution.toString(),
            frequency = frequency.ifBlank { "monthly" },
            startDate = startDate,
            endDate = endDate,
            allocations = allocations.map {
                AllocationRow(
                    symbol = it.symbol,
                    type = it.type,
                    name = it.name ?: it.symbol,
                    percent = it.percent.toString()
                )
            }
        )
        revalidate()
    }

    fun onSearchQueryChanged(query: String) {
        _assetSelector.update { it.copy(query = query, error = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            performSearch()
        }
    }

    fun onFilterChanged(filter: String) {
        _assetSelector.update { it.copy(filter = filter) }
        viewModelScope.launch { performSearch() }
    }

    fun toggleAssetSelection(symbol: String) {
        _assetSelector.update { state ->
            val next = state.selected.toMutableSet()
            if (!next.add(symbol)) next.remove(symbol)
            state.copy(selected = next)
        }
    }

    fun clearAssetSelection() {
        _assetSelector.update { it.copy(selected = emptySet(), query = "", results = emptyList()) }
    }

    fun selectedAssets(): List<AssetSearchResultDto> {
        val state = _assetSelector.value
        return state.results.filter { it.symbol in state.selected }
    }

    fun runSimulation() {
        revalidate()
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, runError = null, createdSimulationId = null) }
            try {
                val request = CreateSimulationRequest(
                    name = state.name.trim(),
                    startDate = state.startDate,
                    endDate = state.endDate,
                    initialInvestment = state.initialInvestment.toDouble(),
                    recurringContribution = state.recurringContribution.toDouble(),
                    frequency = state.frequency,
                    allocations = state.allocations.map {
                        AllocationDto(
                            symbol = it.symbol,
                            type = it.type,
                            percent = it.percent.toDouble(),
                            name = it.name
                        )
                    }
                )
                val created = repository.createSimulation(request)
                _uiState.update {
                    it.copy(isRunning = false, createdSimulationId = created.id)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        runError = e.localizedMessage ?: "Couldn't run the simulation. Try again."
                    )
                }
            }
        }
    }

    fun consumeCreatedId() {
        _uiState.update { it.copy(createdSimulationId = null) }
    }

    private suspend fun performSearch() {
        val state = _assetSelector.value
        val q = state.query.trim()
        if (q.isEmpty()) {
            _assetSelector.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        _assetSelector.update { it.copy(isSearching = true, error = null) }
        try {
            val results = repository.searchAssets(q, state.filter)
            _assetSelector.update { it.copy(results = results, isSearching = false) }
        } catch (e: Exception) {
            _assetSelector.update {
                it.copy(
                    isSearching = false,
                    results = emptyList(),
                    error = e.localizedMessage ?: "Search failed"
                )
            }
        }
    }

    private fun revalidate() {
        _uiState.update { state ->
            val errors = mutableMapOf<String, String>()
            if (state.name.isBlank()) errors["name"] = "Give your scenario a name."
            val initial = state.initialInvestment.toDoubleOrNull()
            if (initial == null || initial <= 0) errors["initial"] = "Initial investment must be greater than 0."
            val contribution = state.recurringContribution.toDoubleOrNull()
            if (contribution == null || contribution < 0) {
                errors["contribution"] = "Contribution must be 0 or more."
            }
            try {
                val start = LocalDate.parse(state.startDate, DateTimeFormatter.ISO_LOCAL_DATE)
                val end = LocalDate.parse(state.endDate, DateTimeFormatter.ISO_LOCAL_DATE)
                if (!end.isAfter(start)) errors["dates"] = "End date must be after start date."
            } catch (_: Exception) {
                errors["dates"] = "Use valid dates (YYYY-MM-DD)."
            }

            val allocationError = when {
                state.allocations.isEmpty() -> "Add at least one asset."
                kotlin.math.abs(state.allocationTotal - 100.0) > 0.5 ->
                    "Allocations must total 100% (currently ${"%.1f".format(state.allocationTotal)}%)."
                state.allocations.any { (it.percent.toDoubleOrNull() ?: -1.0) < 0 } ->
                    "Allocation percentages must be 0 or more."
                else -> null
            }

            state.copy(fieldErrors = errors, allocationError = allocationError)
        }
    }
}