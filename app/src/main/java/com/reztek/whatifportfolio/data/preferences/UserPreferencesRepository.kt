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

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DISPLAY_CURRENCY = stringPreferencesKey("display_currency")
        val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val APP_LOCALE = stringPreferencesKey("app_locale")
    }

    companion object {
        private const val TAG = "UserPreferencesRepo"
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
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
                useDarkTheme = prefs[Keys.USE_DARK_THEME] ?: false,
                biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                appLocale = prefs[Keys.APP_LOCALE] ?: ""
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

    suspend fun setBiometricEnabled(enabled: Boolean) {
        Log.d(TAG, "Persisting biometric lock preference: $enabled")
        context.dataStore.edit { prefs -> prefs[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        Log.d(TAG, "Persisting notifications preference: $enabled")
        context.dataStore.edit { prefs -> prefs[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAppLocale(tag: String) {
        Log.d(TAG, "Persisting app locale preference: '$tag'")
        context.dataStore.edit { prefs -> prefs[Keys.APP_LOCALE] = tag }
    }
}
