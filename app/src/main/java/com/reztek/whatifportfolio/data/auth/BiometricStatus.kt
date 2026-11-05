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
 * Outcome of a device biometric-availability check. A sealed hierarchy so
 * callers must handle every known case explicitly, same pattern as
 * [com.reztek.whatifportfolio.ui.auth.SignInUiState].
 */
sealed interface BiometricAvailability {
    /** Biometric hardware is present, enrolled, and class-3/strong capable. */
    data object Available : BiometricAvailability

    /** Hardware exists but the user has no biometrics enrolled yet. */
    data object NoneEnrolled : BiometricAvailability

    /** Hardware exists but is temporarily unavailable (e.g. in use, updating). */
    data object HardwareUnavailable : BiometricAvailability

    /** Device has no biometric hardware at all. */
    data object NoHardware : BiometricAvailability

    /** A security vulnerability was found; the OS requires an update first. */
    data object SecurityUpdateRequired : BiometricAvailability

    /** Any status code not explicitly mapped above. */
    data class Unknown(val code: Int) : BiometricAvailability
}

/**
 * Checks whether strong (class 3) biometric authentication is usable on this
 * device right now. Used by Settings to decide whether to offer the
 * biometric lock toggle, and by the lock screen to decide whether to show
 * the biometric prompt or fall back to a password.
 *
 * The status-mapping logic ([mapResultCode]) is a pure function kept
 * separate from the [Context]/[BiometricManager] lookup, so it can be unit
 * tested on the JVM without mocking Android framework classes.
 */
object BiometricStatus {

    private const val TAG = "BiometricStatus"

    const val REQUIRED_AUTHENTICATORS = BIOMETRIC_STRONG

    /** Queries the platform BiometricManager for the current device/user state. */
    fun checkAvailability(context: Context): BiometricAvailability {
        val resultCode = BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG)
        val availability = mapResultCode(resultCode)
        logResult(resultCode, availability)
        return availability
    }

    /** Maps a raw [BiometricManager.canAuthenticate] result code to [BiometricAvailability]. */
    fun mapResultCode(resultCode: Int): BiometricAvailability = when (resultCode) {
        BiometricManager.BIOMETRIC_SUCCESS ->
            BiometricAvailability.Available
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
            BiometricAvailability.NoneEnrolled
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
            BiometricAvailability.HardwareUnavailable
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
            BiometricAvailability.NoHardware
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
            BiometricAvailability.SecurityUpdateRequired
        else ->
            BiometricAvailability.Unknown(resultCode)
    }

    // Logs the outcome only - never log anything about the enrolled biometric itself.
    private fun logResult(resultCode: Int, availability: BiometricAvailability) {
        when (availability) {
            is BiometricAvailability.Available ->
                Log.i(TAG, "Biometric authentication available (code=$resultCode)")
            is BiometricAvailability.Unknown ->
                Log.w(TAG, "Unrecognised biometric status code=$resultCode")
            else ->
                Log.i(TAG, "Biometric authentication unavailable: $availability (code=$resultCode)")
        }
    }
}
