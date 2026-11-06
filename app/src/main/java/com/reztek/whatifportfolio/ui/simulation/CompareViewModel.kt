package com.reztek.whatifportfolio.ui.simulation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reztek.whatifportfolio.data.local.SimulationDatabase
import com.reztek.whatifportfolio.data.local.SimulationRepository
import com.reztek.whatifportfolio.data.repository.RemoteSimulationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CompareUiState {
    data object Loading : CompareUiState
    data class Ready(val first: ComparisonSimulation, val second: ComparisonSimulation) : CompareUiState
    data class Error(val message: String) : CompareUiState
}

private class CompareLoadException(message: String) : Exception(message)

class CompareViewModel @JvmOverloads constructor(
    application: Application,
    private val remote: RemoteSimulationRepository = RemoteSimulationRepository(),
    private val local: SimulationRepository = SimulationRepository(
        SimulationDatabase.getDatabase(application).simulationDao()
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CompareUiState>(CompareUiState.Loading)
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    private var requestedKeys: Pair<String, String>? = null

    fun load(firstKey: String, secondKey: String) {
        val keys = firstKey to secondKey
        if (requestedKeys == keys && _uiState.value !is CompareUiState.Error) return
        requestedKeys = keys

        if (firstKey == secondKey) {
            _uiState.value = CompareUiState.Error("Choose two different simulations to compare.")
            return
        }

        _uiState.value = CompareUiState.Loading
        viewModelScope.launch {
            try {
                val (first, second) = coroutineScope {
                    val a = async { resolve(firstKey) }
                    val b = async { resolve(secondKey) }
                    a.await() to b.await()
                }
                _uiState.value = CompareUiState.Ready(first, second)
            } catch (e: CancellationException) {
                throw e
            } catch (e: CompareLoadException) {
                _uiState.value = CompareUiState.Error(e.message.orEmpty())
            } catch (e: Exception) {
                _uiState.value = CompareUiState.Error(
                    e.localizedMessage ?: "Couldn't load the simulations to compare."
                )
            }
        }
    }

    fun retry() {
        val keys = requestedKeys ?: return
        load(keys.first, keys.second)
    }

    private suspend fun resolve(key: String): ComparisonSimulation =
        when (val parsed = CompareKey.parse(key)) {
            is CompareKey.Remote ->
                ComparisonSimulationMapper.fromRemote(remote.getSimulation(parsed.id))
            is CompareKey.Local ->
                local.getSimulationById(parsed.id)?.let(ComparisonSimulationMapper::fromLocal)
                    ?: throw CompareLoadException("A selected on-device simulation no longer exists.")
            null -> throw CompareLoadException("One of the selected simulations couldn't be recognised.")
        }
}
