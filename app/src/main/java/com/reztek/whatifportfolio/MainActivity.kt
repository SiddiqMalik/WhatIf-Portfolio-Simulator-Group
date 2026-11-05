package com.reztek.whatifportfolio

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
<<<<<<< Updated upstream
=======
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
>>>>>>> Stashed changes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.rememberCoroutineScope
import com.reztek.whatifportfolio.data.auth.BiometricAvailability
import com.reztek.whatifportfolio.data.auth.BiometricGatePolicy
import com.reztek.whatifportfolio.data.auth.BiometricStatus
import com.reztek.whatifportfolio.data.preferences.UserPreferences
import com.reztek.whatifportfolio.data.preferences.UserPreferencesRepository
import com.reztek.whatifportfolio.navigation.WhatIfNavGraph
import com.reztek.whatifportfolio.ui.auth.BiometricLockScreen
import com.reztek.whatifportfolio.ui.auth.BiometricPrompter
import com.reztek.whatifportfolio.ui.auth.BiometricPurpose
import com.reztek.whatifportfolio.ui.auth.LocalBiometricPrompter
import com.reztek.whatifportfolio.ui.theme.WhatIfPortfolioTheme

private const val TAG = "MainActivity"

/**
 * Single-Activity host for the whole Compose UI.
 */
class MainActivity : FragmentActivity() {

<<<<<<< Updated upstream
=======
    // Survives rotation (so no re-lock) but not process death (so a restored app re-locks).
    private val appLockViewModel: AppLockViewModel by viewModels()

    private var pendingSimulationId by mutableStateOf<String?>(null)

    private lateinit var biometricPrompter: BiometricPrompter

>>>>>>> Stashed changes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        enableEdgeToEdge()

        biometricPrompter = BiometricPrompter(this, appLockViewModel)

        val preferencesRepository = UserPreferencesRepository(applicationContext)

        setContent {
            // null is intentional: wait for DataStore's first value so a persisted
            // enabled lock cannot briefly reveal the authenticated app on cold start.
            val preferences by preferencesRepository.userPreferencesFlow
                .map { it as UserPreferences? }
                .collectAsStateWithLifecycle(initialValue = null)
            var biometricUnlocked by remember { mutableStateOf(false) }
            var passwordError by remember { mutableStateOf<String?>(null) }
            var passwordLoading by remember { mutableStateOf(false) }
            val authScope = rememberCoroutineScope()
            val hasFirebaseSession = FirebaseAuth.getInstance().currentUser != null

<<<<<<< Updated upstream
            WhatIfPortfolioTheme(darkTheme = preferences?.useDarkTheme ?: false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val currentPreferences = preferences
                    if (currentPreferences == null) {
                        // Keep the initial surface blank while local preferences load.
                    } else if (BiometricGatePolicy.shouldLock(
                        preferencesLoaded = true,
                        hasFirebaseSession = hasFirebaseSession,
                        biometricEnabled = currentPreferences.biometricEnabled,
                        biometricUnlocked = biometricUnlocked
                    )) {
                        val availability = BiometricStatus.checkAvailability(this@MainActivity)
                        val prompt = rememberBiometricPrompt(
                            onAuthenticated = { biometricUnlocked = true },
                            onAuthenticationError = { /* Stay on the lock screen; user may retry. */ }
                        )
                        BiometricLockScreen(
                            onUnlock = prompt,
                            onPasswordUnlock = { password ->
                                val user = FirebaseAuth.getInstance().currentUser
                                val email = user?.email
                                if (user == null || email.isNullOrBlank()) {
                                    passwordError = "No email/password account is available for fallback."
                                } else {
                                    passwordLoading = true
                                    passwordError = null
                                    authScope.launch {
                                        try {
                                            user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
                                            biometricUnlocked = true
                                        } catch (_: Exception) {
                                            passwordError = "Password verification failed. Check your password and try again."
                                        } finally {
                                            passwordLoading = false
                                        }
                                    }
                                }
                            },
                            passwordError = passwordError,
                            passwordLoading = passwordLoading,
                            message = if (availability == BiometricAvailability.Available) {
                                "Use your device biometrics to continue, or verify your account password."
                            } else {
                                "Strong biometrics are currently unavailable. Verify your account password to continue."
                            }
                        )
                    } else {
                        WhatIfNavGraph()
=======
            CompositionLocalProvider(LocalBiometricPrompter provides biometricPrompter) {
                WhatIfPortfolioTheme(darkTheme = preferences?.useDarkTheme ?: false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (preferences == null || lockState.lockAtLaunch == null) {
                            // Keep the initial surface blank while preferences and the
                            // launch decision load, so the app is never revealed early.
                        } else if (lockState.showLock) {
                            val availability = remember { BiometricStatus.checkAvailability(this@MainActivity) }
                            val biometricAvailable = availability == BiometricAvailability.Available
                            BiometricLockScreen(
                                onUnlock = { biometricPrompter.show(BiometricPurpose.UNLOCK) },
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
                                onShowPasswordFallbackChange = appLockViewModel::onShowPasswordFallbackChange,
                                onSignOut = appLockViewModel::signOut
                            )
                        } else {
                            WhatIfNavGraph(
                                pendingSimulationId = pendingSimulationId,
                                onPendingSimulationConsumed = { pendingSimulationId = null }
                            )
                        }
>>>>>>> Stashed changes
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
