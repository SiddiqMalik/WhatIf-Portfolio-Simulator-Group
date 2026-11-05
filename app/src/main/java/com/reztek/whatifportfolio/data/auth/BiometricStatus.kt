package com.reztek.whatifportfolio.data.auth

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager

sealed class BiometricAvailability {
    data object Available : BiometricAvailability()
    data object NoneEnrolled : BiometricAvailability()
    data object NoHardware : BiometricAvailability()
    data object HardwareUnavailable : BiometricAvailability()
    data object SecurityUpdateRequired : BiometricAvailability()

    data class Unknown(val code: Int) : BiometricAvailability()
}

object BiometricStatus {

    private const val TAG = "BiometricStatus"

    /**
     * The one authenticator class the app accepts. The BiometricPrompt in
     * BiometricPromptHost uses this same constant, so "Available" in Settings
     * always means the prompt can actually be shown.
     */
    const val REQUIRED_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG

    /**
     * Checks whether strong biometric authentication is available on this device.
     * Weak biometrics (for example some face unlock) are NOT accepted because the
     * prompt requires BIOMETRIC_STRONG.
     */
    fun checkAvailability(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return checkAvailabilityWith { authenticators -> biometricManager.canAuthenticate(authenticators) }
    }

    /**
     * Same check with the platform call injected, so tests can verify that only
     * [REQUIRED_AUTHENTICATORS] is ever queried. Logs the outcome (no personal data).
     */
    fun checkAvailabilityWith(canAuthenticate: (Int) -> Int): BiometricAvailability {
        val result = mapResultCode(canAuthenticate(REQUIRED_AUTHENTICATORS))
        Log.i(TAG, "Biometric availability: $result")
        return result
    }

    /**
     * Converts a BiometricManager result code into a BiometricAvailability.
     *
     * This is a pure function, allowing it to be tested without Android
     * Context or a mocked BiometricManager.
     */
    fun mapResultCode(resultCode: Int): BiometricAvailability {
        return when (resultCode) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                BiometricAvailability.Available

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricAvailability.NoneEnrolled

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricAvailability.NoHardware

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricAvailability.HardwareUnavailable

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricAvailability.SecurityUpdateRequired

            else ->
                BiometricAvailability.Unknown(resultCode)
        }
    }
}