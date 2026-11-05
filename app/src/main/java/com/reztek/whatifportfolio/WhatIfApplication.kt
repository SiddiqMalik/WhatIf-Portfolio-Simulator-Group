package com.reztek.whatifportfolio

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.reztek.whatifportfolio.data.preferences.LocaleHelper
import com.reztek.whatifportfolio.data.preferences.UserPreferencesRepository
import com.reztek.whatifportfolio.notifications.ensureSimulationNotificationChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "WhatIfApplication"

/**
 * Application entry point. Initialises Firebase, notification channel, and
 * persisted locale — PART3-DAVID.
 */
class WhatIfApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application onCreate")
        FirebaseApp.initializeApp(this)
        ensureSimulationNotificationChannel(this)

        appScope.launch {
            runCatching {
                val prefs = UserPreferencesRepository(this@WhatIfApplication)
                    .userPreferencesFlow
                    .first()
                LocaleHelper.applyAppLocale(prefs.appLocale)
            }.onFailure { Log.e(TAG, "Failed to restore locale", it) }
        }
    }
}
