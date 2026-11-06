package com.reztek.whatifportfolio.data.auth

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt

const val BIOMETRIC_NOT_RECOGNISED_MESSAGE = "Not recognised. Try again."

data class BiometricErrorInfo(
    val message: String,
    val openPasswordFallback: Boolean
)

fun biometricErrorMessage(errorCode: Int, errString: String): BiometricErrorInfo = when (errorCode) {
    BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON ->
        BiometricErrorInfo(errString, false)
    BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
        BiometricErrorInfo(errString, true)
    else ->
        BiometricErrorInfo(if (errString.isNotBlank()) errString else "Biometric authentication error", true)
}

fun biometricSettingsSubtitle(availability: BiometricAvailability, enabled: Boolean): String = when {
    enabled -> "Requires biometrics to open the app"
    availability is BiometricAvailability.Available -> "Protect your app with fingerprint or face unlock"
    availability is BiometricAvailability.NoneEnrolled -> "Enroll biometrics in device settings first"
    availability is BiometricAvailability.NoHardware -> "Not supported on this device"
    else -> "Biometrics currently unavailable"
}

fun isBiometricToggleEnabled(availability: BiometricAvailability, enabled: Boolean): Boolean =
    enabled || availability is BiometricAvailability.Available

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

    const val REQUIRED_AUTHENTICATORS = BIOMETRIC_STRONG

    /** Queries the platform BiometricManager for the current device/user state. */
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