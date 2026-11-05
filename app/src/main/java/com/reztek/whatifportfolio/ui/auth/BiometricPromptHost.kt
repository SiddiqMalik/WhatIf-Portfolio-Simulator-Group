package com.reztek.whatifportfolio.ui.auth

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.biometric.BiometricPrompt
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
