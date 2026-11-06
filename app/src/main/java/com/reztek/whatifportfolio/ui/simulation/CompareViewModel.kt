package com.reztek.whatifportfolio.ui.simulation

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reztek.whatifportfolio.data.local.SimulationDatabase
import com.reztek.whatifportfolio.data.local.SimulationRepository
import com.reztek.whatifportfolio.data.repository.RemoteSimulationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface CompareUiState {
    data object Loading : CompareUiState
    data class Ready(val first: ComparisonSimulation, val second: ComparisonSimulation) : CompareUiState

    /** [canRetry] is false for problems a retry cannot fix (bad or duplicate ids). */
    data class Error(val message: String, val canRetry: Boolean) : CompareUiState
}

/** A selected simulation no longer exists (deleted locally, or HTTP 404 from the API). */
private class MissingSimulationException : Exception()

/**
 * Loads the two selected simulations and exposes a Loading / Ready / Error state.
 *
 * Keys are the stable [SavedListItem.key] values. "local-<id>" is read from Room
 * and "remote-<id>" from the REST API (GET /v1/simulations/{id}, which includes
 * the time series that the chart needs). Repositories are injected through the
 * constructor so the ViewModel can be unit tested with MockK.
 */
class CompareViewModel(
    private val firstKey: String,
    private val secondKey: String,
    private val localRepository: SimulationRepository,
    private val remoteRepository: RemoteSimulationRepository = RemoteSimulationRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "CompareViewModel"

        const val MESSAGE_INVALID_KEY =
            "One of the selected simulations could not be read. Go back and choose two again."
        const val MESSAGE_SAME_SIMULATION =
            "Choose two different simulations to compare."
        const val MESSAGE_MISSING =
            "One of the selected simulations no longer exists. Go back and choose two again."
        const val MESSAGE_OFFLINE =
            "Couldn't reach the server. Check your connection and try again."
        const val MESSAGE_SERVER =
            "The server couldn't load a simulation right now. Try again."
        const val MESSAGE_UNKNOWN =
            "Something went wrong while loading the comparison. Try again."

        /** Builds the ViewModel with the app's real repositories. */
        fun factory(application: Application, firstKey: String, secondKey: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CompareViewModel(
                        firstKey = firstKey,
                        secondKey = secondKey,
                        localRepository = SimulationRepository(
                            SimulationDatabase.getDatabase(application).simulationDao()
                        )
                    ) as T
            }
    }

    private val _uiState = MutableStateFlow<CompareUiState>(CompareUiState.Loading)
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    private var requestedKeys: Pair<String, String>? = null

    private var loadJob: Job? = null

    init {
        Log.d(TAG, "CompareViewModel created")
        load()
    }

    /** Re-runs the load after an error. */
    fun retry() {
        Log.i(TAG, "Retry requested")
        load()
    }

    private fun load() {
        val first = CompareKey.parse(firstKey)
        val second = CompareKey.parse(secondKey)
        if (first == null || second == null) {
            Log.w(TAG, "Rejected comparison: malformed key")
            _uiState.value = CompareUiState.Error(MESSAGE_INVALID_KEY, canRetry = false)
            return
        }
        if (first == second) {
            Log.w(TAG, "Rejected comparison: same simulation selected twice")
            _uiState.value = CompareUiState.Error(MESSAGE_SAME_SIMULATION, canRetry = false)
            return
        }

        loadJob?.cancel()
        _uiState.value = CompareUiState.Loading
        loadJob = viewModelScope.launch {
            Log.d(TAG, "Loading comparison: ${first.typeLabel} + ${second.typeLabel}")
            try {
                val firstSimulation = loadOne(first)
                val secondSimulation = loadOne(second)
                Log.i(TAG, "Comparison ready: ${first.typeLabel} + ${second.typeLabel}")
                _uiState.value = CompareUiState.Ready(firstSimulation, secondSimulation)
            } catch (e: CancellationException) {
                throw e
            } catch (_: MissingSimulationException) {
                Log.w(TAG, "Comparison failed: simulation missing or deleted")
                _uiState.value = CompareUiState.Error(MESSAGE_MISSING, canRetry = false)
            } catch (e: HttpException) {
                Log.w(TAG, "Comparison failed: HTTP ${e.code()}")
                _uiState.value = CompareUiState.Error(MESSAGE_SERVER, canRetry = true)
            } catch (e: IOException) {
                Log.w(TAG, "Comparison failed: network ${e.javaClass.simpleName}")
                _uiState.value = CompareUiState.Error(MESSAGE_OFFLINE, canRetry = true)
            } catch (e: Exception) {
                Log.e(TAG, "Comparison failed: ${e.javaClass.simpleName}")
                _uiState.value = CompareUiState.Error(MESSAGE_UNKNOWN, canRetry = true)
            }
        }
    }

    private suspend fun loadOne(key: CompareKey): ComparisonSimulation = when (key) {
        is CompareKey.Local -> {
            val entity = localRepository.getSimulationById(key.id) ?: throw MissingSimulationException()
            ComparisonSimulationMapper.fromLocal(entity)
        }
        is CompareKey.Remote -> try {
            ComparisonSimulationMapper.fromRemote(remoteRepository.getSimulation(key.id))
        } catch (e: HttpException) {
            if (e.code() == 404) throw MissingSimulationException() else throw e
        }
    }
}
