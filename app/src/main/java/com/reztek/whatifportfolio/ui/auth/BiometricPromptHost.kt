package com.reztek.whatifportfolio.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
<<<<<<< Updated upstream
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
=======
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.reztek.whatifportfolio.data.auth.BiometricStatus
import com.reztek.whatifportfolio.data.auth.biometricErrorMessage

private const val TAG = "BiometricPromptHost"

enum class BiometricPurpose { UNLOCK, ENABLE_LOCK }

class BiometricPrompter(
    private val activity: FragmentActivity,
    private val appLockViewModel: AppLockViewModel
) {
    private val prompt = BiometricPrompt(
        activity,
        // ContextCompat works on minSdk 26; Context.mainExecutor needs API 28.
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.i(TAG, "Biometric authentication succeeded")
                appLockViewModel.onBiometricSuccess()
            }

            override fun onAuthenticationFailed() {
                // The prompt stays open and lets the user try again.
                Log.w(TAG, "Biometric attempt not recognised")
                appLockViewModel.onBiometricNotRecognised()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val info = biometricErrorMessage(errorCode, errString.toString())
                Log.w(TAG, "Biometric error code=$errorCode openFallback=${info.openPasswordFallback}")
                appLockViewModel.onBiometricError(info.message, info.openPasswordFallback)
            }
>>>>>>> Stashed changes
        }
    )

    private val unlockInfo = promptInfo(
        title = "Unlock WhatIf Portfolio",
        subtitle = "Confirm your identity to continue"
    )

    private val enableInfo = promptInfo(
        title = "Turn on biometric lock",
        subtitle = "Confirm your identity to enable the app lock"
    )

    fun show(purpose: BiometricPurpose) {
        if (activity.supportFragmentManager.isStateSaved) {
            Log.w(TAG, "Cannot show biometric prompt: activity state already saved")
            return
        }
        if (!appLockViewModel.beginPrompt(purpose)) {
            Log.d(TAG, "Prompt already in progress")
            return
        }
        Log.d(TAG, "Showing biometric prompt purpose=$purpose")
        prompt.authenticate(if (purpose == BiometricPurpose.UNLOCK) unlockInfo else enableInfo)
    }
<<<<<<< Updated upstream
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
=======

    private fun promptInfo(title: String, subtitle: String): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            // Same constant the availability check uses, so Settings and the prompt agree.
            .setAllowedAuthenticators(BiometricStatus.REQUIRED_AUTHENTICATORS)
            .setNegativeButtonText("Cancel")
            .build()
}

val LocalBiometricPrompter = staticCompositionLocalOf<BiometricPrompter?> { null }

/** Walks the ContextWrapper chain to find the hosting FragmentActivity, if any. */
fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
>>>>>>> Stashed changes
