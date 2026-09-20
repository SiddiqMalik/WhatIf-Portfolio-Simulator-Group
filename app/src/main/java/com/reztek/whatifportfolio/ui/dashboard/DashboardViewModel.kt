package com.reztek.whatifportfolio.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** A lightweight row model for a simulation summary — enough to render a Dashboard list row. */
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
 * Per Deliverable 3 (Screen 2 spec): reads the 3 most recently updated
 * documents from the signed-in user's `simulations` subcollection, ordered
 * by `updatedAt` descending. This is a ONE-TIME fetch (not a live listener)
 * — the Dashboard is a snapshot/landing view; the Saved Simulations List is
 * where a live listener belongs, per its own spec.
 */
class DashboardViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
        private const val RECENT_SIMULATIONS_LIMIT = 3L
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
                val snapshot = firestore
                    .collection("users").document(user.uid)
                    .collection("simulations")
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .limit(RECENT_SIMULATIONS_LIMIT)
                    .get()
                    .await()

                val summaries = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val finalValue = doc.getDouble("finalValue") ?: 0.0
                    val percentReturn = doc.getDouble("percentReturn") ?: 0.0
                    val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    SimulationSummary(doc.id, name, finalValue, percentReturn, updatedAt)
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
