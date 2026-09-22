package com.reztek.whatifportfolio.ui.simulation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.data.repository.RemoteSimulationRepository
import com.reztek.whatifportfolio.ui.chart.ChartSeriesPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ChartWindow { ONE_YEAR, FIVE_YEARS, MAX }

sealed interface SimulationDetailUiState {
    data object Loading : SimulationDetailUiState
    data class Ready(val simulation: SimulationDto, val window: ChartWindow = ChartWindow.MAX) :
        SimulationDetailUiState
    data class Error(val message: String) : SimulationDetailUiState
}

class SimulationResultsViewModel(
    private val repository: RemoteSimulationRepository = RemoteSimulationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SimulationDetailUiState>(SimulationDetailUiState.Loading)
    val uiState: StateFlow<SimulationDetailUiState> = _uiState.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun load(simulationId: String) {
        viewModelScope.launch {
            _uiState.value = SimulationDetailUiState.Loading
            try {
                val sim = repository.getSimulation(simulationId)
                _uiState.value = SimulationDetailUiState.Ready(sim)
            } catch (e: Exception) {
                _uiState.value = SimulationDetailUiState.Error(
                    e.localizedMessage ?: "Couldn't load simulation results."
                )
            }
        }
    }

    fun setWindow(window: ChartWindow) {
        val current = _uiState.value
        if (current is SimulationDetailUiState.Ready) {
            _uiState.value = current.copy(window = window)
        }
    }

    fun saveSimulation() {
        val current = _uiState.value as? SimulationDetailUiState.Ready ?: return
        viewModelScope.launch {
            _saving.value = true
            try {
                val updated = repository.updateSimulation(
                    current.simulation.id,
                    mapOf("status" to "saved", "name" to current.simulation.name)
                )
                _uiState.value = current.copy(simulation = updated)
                _message.value = "Simulation saved"
            } catch (e: Exception) {
                _message.value = e.localizedMessage ?: "Save failed"
            } finally {
                _saving.value = false
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun chartPoints(simulation: SimulationDto, window: ChartWindow): List<ChartSeriesPoint> {
        val series = simulation.timeSeries.orEmpty()
        if (series.isEmpty()) return emptyList()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val end = runCatching { LocalDate.parse(simulation.endDate, formatter) }.getOrNull()
        val filtered = when (window) {
            ChartWindow.MAX -> series
            ChartWindow.ONE_YEAR -> {
                val cut = end?.minusYears(1)?.toString()
                if (cut == null) series else series.filter { it.date >= cut }
            }
            ChartWindow.FIVE_YEARS -> {
                val cut = end?.minusYears(5)?.toString()
                if (cut == null) series else series.filter { it.date >= cut }
            }
        }
        return filtered.map {
            ChartSeriesPoint(
                date = it.date,
                nominalValue = it.nominalValue,
                realValue = it.realValue,
                contributed = it.contributed
            )
        }
    }
}

class SimulationDetailViewModel(
    private val repository: RemoteSimulationRepository = RemoteSimulationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SimulationDetailUiState>(SimulationDetailUiState.Loading)
    val uiState: StateFlow<SimulationDetailUiState> = _uiState.asStateFlow()

    fun load(simulationId: String) {
        viewModelScope.launch {
            _uiState.value = SimulationDetailUiState.Loading
            try {
                _uiState.value = SimulationDetailUiState.Ready(repository.getSimulation(simulationId))
            } catch (e: Exception) {
                _uiState.value = SimulationDetailUiState.Error(
                    e.localizedMessage ?: "Couldn't load this simulation."
                )
            }
        }
    }

    fun refresh(simulationId: String) = load(simulationId)

    suspend fun delete(simulationId: String) {
        repository.deleteSimulation(simulationId)
    }

    suspend fun rerun(simulationId: String): String {
        val sim = repository.getSimulation(simulationId)
        val created = repository.createSimulation(
            com.reztek.whatifportfolio.data.remote.dto.CreateSimulationRequest(
                name = sim.name,
                startDate = sim.startDate,
                endDate = sim.endDate,
                initialInvestment = sim.initialInvestment,
                recurringContribution = sim.recurringContribution,
                frequency = sim.frequency,
                allocations = sim.allocations.orEmpty()
            )
        )
        return created.id
    }
}
