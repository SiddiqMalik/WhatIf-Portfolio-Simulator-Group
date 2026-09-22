package com.reztek.whatifportfolio.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reztek.whatifportfolio.data.repository.RemoteSimulationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/** A lightweight row model for a simulation summary - enough to render a Dashboard list row. */
data class SimulationSummary(
    val id: String,
    val name: String,
    val finalValue: Double,
    val percentReturn: Double,
    val updatedAtMillis: Long
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Loaded(
        val displayName: String,
        val recentSimulations: List<SimulationSummary>
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

/**
 * State holder for the Home/Dashboard screen.
 *
 * Per Deliverable 3 (Screen 2 spec): shows the 3 most recently updated
 * simulations. Data comes from the custom REST API (GET /v1/simulations)
 * rather than a direct Firestore read - Firestore security rules restrict
 * the `simulations` subcollection to the API's Admin SDK only, per the
 * project's architecture (the API does the heavy lifting). This is a
 * ONE-TIME fetch (not a live listener) - the Dashboard is a snapshot/
 * landing view; the Saved Simulations List is where a live listener
 * belongs, per its own spec.
 */
class DashboardViewModel(
    private val simulationRepository: RemoteSimulationRepository = RemoteSimulationRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
        private const val RECENT_SIMULATIONS_LIMIT = 3
    }

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "DashboardViewModel created")
        loadDashboard()
    }

    fun loadDashboard() {
        val user = auth.currentUser
        if (user == null) {
            Log.w(TAG, "loadDashboard() called with no authenticated user")
            _uiState.value = DashboardUiState.Error("You've been signed out.")
            return
        }

        Log.d(TAG, "Loading dashboard for uid=${user.uid}")
        _uiState.value = DashboardUiState.Loading

        viewModelScope.launch {
            try {
                val simulations = simulationRepository.listSimulations()

                val summaries = simulations
                    .sortedByDescending { runCatching { Instant.parse(it.updatedAt).toEpochMilli() }.getOrDefault(0L) }
                    .take(RECENT_SIMULATIONS_LIMIT)
                    .map { dto ->
                        SimulationSummary(
                            id = dto.id,
                            name = dto.name,
                            finalValue = dto.finalValue,
                            percentReturn = dto.percentReturn,
                            updatedAtMillis = runCatching { Instant.parse(dto.updatedAt).toEpochMilli() }.getOrDefault(0L)
                        )
                    }

                Log.i(TAG, "Loaded ${summaries.size} recent simulation(s)")
                _uiState.value = DashboardUiState.Loaded(
                    displayName = user.displayName?.substringBefore(" ") ?: "there",
                    recentSimulations = summaries
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load recent simulations", e)
                _uiState.value = DashboardUiState.Error(
                    "Couldn't load your simulations. Check your connection and try again."
                )
            }
        }
    }
}
