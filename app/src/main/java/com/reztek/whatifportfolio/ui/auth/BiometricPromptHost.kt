package com.reztek.whatifportfolio.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity

/** Creates a strong-biometric prompt bound to the hosting ComponentActivity. */
@Composable
fun rememberBiometricPrompt(
    onAuthenticated: () -> Unit,
    onAuthenticationError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val prompt = remember(activity, onAuthenticated, onAuthenticationError) {
        activity?.let {
            BiometricPrompt(
                it,
                it.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onAuthenticated()
                    }

                    override fun onAuthenticationFailed() {
                        onAuthenticationError("Biometric not recognized. Try again.")
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        // Cancellation and lockout keep the app locked; surface the platform message.
                        onAuthenticationError(errString.toString())
                    }
                }
            )
        }
    }
    return remember(prompt) {
        {
            prompt?.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock WhatIf Portfolio")
                    .setSubtitle("Confirm your identity to continue")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText("Cancel")
                    .build()
            )
        }
    }
}
