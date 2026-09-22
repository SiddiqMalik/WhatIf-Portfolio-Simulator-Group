package com.reztek.whatifportfolio

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.reztek.whatifportfolio.data.preferences.UserPreferencesRepository
import com.reztek.whatifportfolio.navigation.WhatIfNavGraph
import com.reztek.whatifportfolio.ui.auth.SignInViewModel
import com.reztek.whatifportfolio.ui.simulation.SimulationBuilderScreen
import com.reztek.whatifportfolio.ui.theme.WhatIfPortfolioTheme
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

/**
 * Single-Activity host for the whole Compose UI.
 */
class MainActivity : ComponentActivity() {

    private val webClientId = "YOUR_FIREBASE_WEB_CLIENT_ID.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        enableEdgeToEdge()

        val preferencesRepository = UserPreferencesRepository(applicationContext)

        setContent {
            val preferences by preferencesRepository.userPreferencesFlow
                .collectAsStateWithLifecycle(initialValue = com.reztek.whatifportfolio.data.preferences.UserPreferences())

            WhatIfPortfolioTheme(darkTheme = preferences.useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val signInViewModel: SignInViewModel = viewModel()

                    // Renders your SimulationBuilderScreen directly inside the app layout.
                    // If you want to view the full screen immediately, SimulationBuilderScreen() is passed here.
                    SimulationBuilderScreen()
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

    private fun launchGoogleSignIn(signInViewModel: SignInViewModel) {
        val credentialManager = CredentialManager.create(this)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Requesting Google credential")
                val result = credentialManager.getCredential(this@MainActivity, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                signInViewModel.signInWithGoogleIdToken(googleIdTokenCredential.idToken)
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "Google credential prompt cancelled by user")
                signInViewModel.onSignInCancelled()
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential Manager request failed", e)
                signInViewModel.onSignInCancelled()
            }
        }
    }
}