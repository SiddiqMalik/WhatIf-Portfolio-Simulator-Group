package com.reztek.whatifportfolio

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.reztek.whatifportfolio.data.preferences.LocaleHelper
import com.reztek.whatifportfolio.data.preferences.UserPreferencesRepository
import com.reztek.whatifportfolio.data.sync.OfflineDataCache
import com.reztek.whatifportfolio.data.sync.SyncScheduler
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

        // PART3-ASH: push any changes queued while offline (runs only once a network is available).
        SyncScheduler.enqueue(this)
        // PART3-ASH: local copies of simulation results and asset searches for offline use.
        OfflineDataCache.init(this)

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
