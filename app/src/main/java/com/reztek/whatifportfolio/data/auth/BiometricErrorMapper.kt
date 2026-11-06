package com.reztek.whatifportfolio.data.auth

import androidx.biometric.BiometricPrompt

/**
 * What the lock screen should show after a BiometricPrompt error.
 *
 * @property openPasswordFallback true when biometrics cannot work right now
 * (lockout, no sensor, nothing enrolled) so the password section should open.
 */
data class BiometricErrorInfo(
    val message: String,
    val openPasswordFallback: Boolean
)

/** Shown while the prompt stays open after a non-matching fingerprint or face. */
const val BIOMETRIC_NOT_RECOGNISED_MESSAGE = "Not recognised. Try again."

private const val GENERIC_FAILURE_MESSAGE =
    "Biometric authentication failed. Try again or use your password."

/**
 * Pure mapping from a BiometricPrompt error code to a user-facing message, so it
 * can be unit tested without Android. The platform text is only used for codes
 * with no specific wording, and is never logged by this function.
 */
fun biometricErrorMessage(code: Int, platformMessage: String): BiometricErrorInfo = when (code) {
    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
    BiometricPrompt.ERROR_USER_CANCELED ->
        BiometricErrorInfo("Unlock cancelled. Tap the button to try again.", false)

    BiometricPrompt.ERROR_CANCELED ->
        BiometricErrorInfo("The biometric prompt was interrupted. Tap the button to try again.", false)

    BiometricPrompt.ERROR_LOCKOUT ->
        BiometricErrorInfo("Too many attempts. Wait a moment, or use your password instead.", true)

    BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
        BiometricErrorInfo("Biometrics are locked on this device. Use your password instead.", true)

    BiometricPrompt.ERROR_NO_BIOMETRICS ->
        BiometricErrorInfo("No fingerprint or face is enrolled. Use your password instead.", true)

    BiometricPrompt.ERROR_HW_NOT_PRESENT,
    BiometricPrompt.ERROR_HW_UNAVAILABLE,
    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ->
        BiometricErrorInfo("Biometrics are unavailable. Use your password instead.", true)

    BiometricPrompt.ERROR_TIMEOUT ->
        BiometricErrorInfo("The prompt timed out. Tap the button to try again.", false)

    BiometricPrompt.ERROR_UNABLE_TO_PROCESS ->
        BiometricErrorInfo("Couldn't read your biometric. Try again.", false)

    else ->
        BiometricErrorInfo(platformMessage.ifBlank { GENERIC_FAILURE_MESSAGE }, false)
}
