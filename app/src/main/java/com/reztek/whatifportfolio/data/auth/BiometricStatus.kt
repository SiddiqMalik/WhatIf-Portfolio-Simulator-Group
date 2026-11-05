package com.reztek.whatifportfolio.data.auth

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager

/**
 * Describes whether biometric authentication can be used on this device.
 * Use checkAvailability() before displaying or launching a biometric prompt.
 */
sealed class BiometricAvailability {
    data object Available : BiometricAvailability()
    data object NoneEnrolled : BiometricAvailability()
    data object NoHardware : BiometricAvailability()
    data object HardwareUnavailable : BiometricAvailability()
    data object SecurityUpdateRequired : BiometricAvailability()

    data class Unknown(val reason: String?) : BiometricAvailability()
}

/**
 * Checks biometric availability using AndroidX BiometricManager.
 * Class 3 biometrics (strong fingerprint/face authentication) are preferred.
 */
object BiometricStatus {

    private const val TAG = "BiometricStatus"

    const val REQUIRED_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG

    fun checkAvailability(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return checkAvailabilityWith { authenticators -> biometricManager.canAuthenticate(authenticators) }
    }

    fun checkAvailabilityWith(canAuthenticate: (Int) -> Int): BiometricAvailability {
        val result = mapResultCode(canAuthenticate(REQUIRED_AUTHENTICATORS))
        Log.i(TAG, "Biometric availability: $result")
        return result
    }

    /**
     * Maps raw BiometricManager status integer codes to corresponding BiometricAvailability states.
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
                BiometricAvailability.Unknown(
                    "BiometricManager returned status code: $resultCode"
                )
        }
    }

    /**
     * Overloaded helper to support direct string/reason mapping if tested explicitly.
     */
    fun mapResultCode(reason: String?): BiometricAvailability {
        return BiometricAvailability.Unknown(reason)
    }
}