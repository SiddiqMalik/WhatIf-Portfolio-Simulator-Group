package com.reztek.whatifportfolio.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Top-level DataStore delegate, scoped to the app Context.
// Reference: Jetpack Preferences DataStore guide
// (https://developer.android.com/topic/libraries/architecture/datastore) —
// chosen over SharedPreferences because it is asynchronous (Flow-based,
// never blocks the UI thread) and type-safe.
private val Context.dataStore by preferencesDataStore(name = "user_preferences")

/**
 * Local-first persistence for user preferences (Person 1 scope: "local UI
 * state holders and basic user preferences persistence").
 *
 * This repository intentionally does NOT talk to Firestore directly — the
 * Settings ViewModel decides whether a given preference also needs to sync
 * to the user's Firestore profile (e.g. display currency does, per
 * Deliverable 3; dark-mode is device-local only). Keeping DataStore as the
 * single local source of truth means Settings still works correctly offline.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DISPLAY_CURRENCY = stringPreferencesKey("display_currency")
        val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
    }

    companion object {
        private const val TAG = "UserPreferencesRepo"
    }

    /** Reactive stream of the current preferences; emits a new value on every change. */
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            // DataStore throws IOException on read failure; degrade to defaults
            // rather than crashing the Settings screen.
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences, falling back to defaults", exception)
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserPreferences(
                displayCurrency = prefs[Keys.DISPLAY_CURRENCY] ?: "ZAR",
                useDarkTheme = prefs[Keys.USE_DARK_THEME] ?: false
            )
        }

    suspend fun setDisplayCurrency(currencyCode: String) {
        Log.d(TAG, "Persisting display currency preference: $currencyCode")
        context.dataStore.edit { prefs -> prefs[Keys.DISPLAY_CURRENCY] = currencyCode }
    }

    suspend fun setUseDarkTheme(enabled: Boolean) {
        Log.d(TAG, "Persisting dark theme preference: $enabled")
        context.dataStore.edit { prefs -> prefs[Keys.USE_DARK_THEME] = enabled }
    }
}
