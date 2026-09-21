package com.reztek.whatifportfolio.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.reztek.whatifportfolio.data.preferences.UserPreferences
import com.reztek.whatifportfolio.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingsUiState(
    val accountEmail: String = "",
    val accountDisplayName: String = "",
    val preferences: UserPreferences = UserPreferences(),
    val isSavingCurrency: Boolean = false
)

/**
 * State holder for the Settings screen — "Account & Preferences" (Deliverable
 * 3, Screen 8).
 *
 * Combines two persistence layers deliberately:
 *  - [UserPreferencesRepository] (DataStore) is the single source of truth
 *    locally and is what the rest of the app reads from — so preferences
 *    keep working even if a Firestore write is in flight or fails.
 *  - Firestore's `users/{uid}` profile document is updated as a best-effort
 *    mirror, per the design spec's "simple one-time Firestore read/write for
 *    preferences instead of real-time listeners, reducing unnecessary
 *    database reads."
 */
class SettingsViewModel(
    application: Application,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val preferencesRepository = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "SettingsViewModel created")
        val user = auth.currentUser
        _uiState.value = _uiState.value.copy(
            accountEmail = user?.email.orEmpty(),
            accountDisplayName = user?.displayName.orEmpty()
        )

        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                Log.d(TAG, "Preferences updated from DataStore: $prefs")
                _uiState.value = _uiState.value.copy(preferences = prefs)
            }
        }
    }

    fun onCurrencySelected(currencyCode: String) {
        Log.i(TAG, "User selected display currency: $currencyCode")
        _uiState.value = _uiState.value.copy(isSavingCurrency = true)
        viewModelScope.launch {
            preferencesRepository.setDisplayCurrency(currencyCode)
            syncCurrencyToFirestoreProfile(currencyCode)
            _uiState.value = _uiState.value.copy(isSavingCurrency = false)
        }
    }

    fun onDarkThemeToggled(enabled: Boolean) {
        Log.i(TAG, "User toggled dark theme: $enabled")
        viewModelScope.launch {
            preferencesRepository.setUseDarkTheme(enabled)
        }
    }

    fun onLogOutClicked(onLoggedOut: () -> Unit) {
        Log.i(TAG, "User signed out uid=${auth.currentUser?.uid}")
        auth.signOut()
        onLoggedOut()
    }

    private suspend fun syncCurrencyToFirestoreProfile(currencyCode: String) {
        val uid = auth.currentUser?.uid ?: run {
            Log.w(TAG, "Skipping Firestore currency sync — no authenticated user")
            return
        }
        try {
            firestore.collection("users").document(uid)
                .set(mapOf("displayCurrency" to currencyCode), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Log.d(TAG, "Synced display currency to Firestore profile")
        } catch (e: Exception) {
            // Best-effort mirror: DataStore already has the value, so the app
            // keeps working correctly offline even if this write fails.
            Log.e(TAG, "Failed to sync display currency to Firestore", e)
        }
    }
}
