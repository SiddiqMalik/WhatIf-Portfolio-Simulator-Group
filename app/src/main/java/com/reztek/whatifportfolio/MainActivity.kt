package com.reztek.whatifportfolio

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.reztek.whatifportfolio.data.auth.BiometricAvailability
import com.reztek.whatifportfolio.data.auth.BiometricStatus
import com.reztek.whatifportfolio.data.preferences.UserPreferences
import com.reztek.whatifportfolio.data.preferences.UserPreferencesRepository
import com.reztek.whatifportfolio.navigation.WhatIfNavGraph
import com.reztek.whatifportfolio.ui.auth.AppLockViewModel
import com.reztek.whatifportfolio.ui.auth.BiometricLockScreen
import com.reztek.whatifportfolio.ui.auth.rememberBiometricPrompt
import com.reztek.whatifportfolio.ui.theme.WhatIfPortfolioTheme
import kotlinx.coroutines.flow.map

private const val TAG = "MainActivity"

/**
 * Single-Activity host for the whole Compose UI.
 */
class MainActivity : FragmentActivity() {

    // Survives rotation (so no re-lock) but not process death (so a restored app re-locks).
    private val appLockViewModel: AppLockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        enableEdgeToEdge()

        val preferencesRepository = UserPreferencesRepository(applicationContext)

        setContent {
            // null is intentional: wait for DataStore's first value so a persisted
            // enabled lock cannot briefly reveal the authenticated app on cold start.
            val preferences by preferencesRepository.userPreferencesFlow
                .map { it as UserPreferences? }
                .collectAsStateWithLifecycle(initialValue = null)
            val lockState by appLockViewModel.uiState.collectAsStateWithLifecycle()

            // Decide ONCE per launch. Enabling the toggle later in Settings does not
            // lock the current session; it applies from the next fresh launch.
            LaunchedEffect(preferences != null) {
                val loaded = preferences
                if (loaded != null) {
                    appLockViewModel.decideLockAtLaunch(
                        biometricEnabled = loaded.biometricEnabled,
                        hasFirebaseSession = FirebaseAuth.getInstance().currentUser != null
                    )
                }
            }

            WhatIfPortfolioTheme(darkTheme = preferences?.useDarkTheme ?: false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (preferences == null || lockState.lockAtLaunch == null) {
                        // Keep the initial surface blank while preferences and the
                        // launch decision load, so the app is never revealed early.
                    } else if (lockState.showLock) {
                        val availability = remember { BiometricStatus.checkAvailability(this@MainActivity) }
                        val biometricAvailable = availability == BiometricAvailability.Available
                        val prompt = rememberBiometricPrompt(
                            onAuthenticated = appLockViewModel::onBiometricSuccess,
                            onAuthenticationError = appLockViewModel::onBiometricError
                        )
                        BiometricLockScreen(
                            onUnlock = prompt,
                            onPasswordUnlock = appLockViewModel::onPasswordSubmitted,
                            message = if (biometricAvailable) {
                                "Use your device biometrics to continue, or verify your account password."
                            } else {
                                "Strong biometrics are currently unavailable. Verify your account password to continue."
                            },
                            errorMessage = lockState.biometricMessage,
                            passwordError = lockState.passwordError,
                            passwordLoading = lockState.passwordLoading,
                            autoPrompt = biometricAvailable,
                            biometricAvailable = biometricAvailable,
                            showPasswordFallback = lockState.showPasswordFallback,
                            onShowPasswordFallbackChange = appLockViewModel::onShowPasswordFallbackChange
                        )
                    } else {
                        WhatIfNavGraph()
                    }
                }
            }
        }
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
