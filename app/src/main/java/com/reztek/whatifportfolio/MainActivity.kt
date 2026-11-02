package com.reztek.whatifportfolio

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reztek.whatifportfolio.data.preferences.UserPreferencesRepository
import com.reztek.whatifportfolio.navigation.WhatIfNavGraph
import com.reztek.whatifportfolio.notifications.NotificationIntentParser
import com.reztek.whatifportfolio.ui.theme.WhatIfPortfolioTheme

private const val TAG = "MainActivity"

/**
 * Single-Activity host. Handles FCM tap deep-links — PART3-DAVID.
 */
class MainActivity : ComponentActivity() {

    private var pendingSimulationId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        enableEdgeToEdge()
        pendingSimulationId = NotificationIntentParser.simulationIdFromIntent(intent)

        val preferencesRepository = UserPreferencesRepository(applicationContext)

        setContent {
            val preferences by preferencesRepository.userPreferencesFlow
                .collectAsStateWithLifecycle(
                    initialValue = com.reztek.whatifportfolio.data.preferences.UserPreferences()
                )

            WhatIfPortfolioTheme(darkTheme = preferences.useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WhatIfNavGraph(
                        pendingSimulationId = pendingSimulationId,
                        onPendingSimulationConsumed = { pendingSimulationId = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingSimulationId = NotificationIntentParser.simulationIdFromIntent(intent)
        Log.i(TAG, "onNewIntent simulationId=$pendingSimulationId")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}
