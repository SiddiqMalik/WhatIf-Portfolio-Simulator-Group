package com.reztek.whatifportfolio.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.reztek.whatifportfolio.data.preferences.LocaleHelper
import com.reztek.whatifportfolio.data.preferences.UserPreferences
import com.reztek.whatifportfolio.data.preferences.UserPreferencesRepository
import com.reztek.whatifportfolio.notifications.FcmTokenRegistrar
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
 * Settings ViewModel — PART3-DAVID adds notifications + locale persistence.
 */
class SettingsViewModel @JvmOverloads constructor(
    application: Application,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val preferencesRepository: UserPreferencesRepository =
        UserPreferencesRepository(application)
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

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

        // Refresh FCM token whenever Settings opens while signed in — PART3-DAVID
        viewModelScope.launch {
            if (auth.currentUser != null) {
                FcmTokenRegistrar.registerCurrentToken()
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

    fun onBiometricToggled(enabled: Boolean) {
        Log.i(TAG, "User toggled biometric lock: $enabled")
        viewModelScope.launch {
            preferencesRepository.setBiometricEnabled(enabled)
        }
    }

    fun onNotificationsToggled(enabled: Boolean) {
        Log.i(TAG, "User toggled notifications: $enabled")
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
            if (enabled) {
                FcmTokenRegistrar.registerCurrentToken()
            }
        }
    }

    fun onLocaleSelected(tag: String) {
        Log.i(TAG, "User selected locale: '$tag'")
        viewModelScope.launch {
            preferencesRepository.setAppLocale(tag)
            LocaleHelper.applyAppLocale(tag)
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
                .set(
                    mapOf("displayCurrency" to currencyCode),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
            Log.d(TAG, "Synced display currency to Firestore profile")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync display currency to Firestore", e)
        }
    }
}
