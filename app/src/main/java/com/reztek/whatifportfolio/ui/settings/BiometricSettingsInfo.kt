package com.reztek.whatifportfolio.ui.settings

import com.reztek.whatifportfolio.data.auth.BiometricAvailability

// PART3-JORDAN: text shown under the biometric toggle in Settings.
// Kept as plain functions so they can be unit tested without Compose.

/** The toggle can be switched on only when biometrics work, but can always be switched off. */
fun isBiometricToggleEnabled(availability: BiometricAvailability, currentlyEnabled: Boolean): Boolean =
    availability is BiometricAvailability.Available || currentlyEnabled

fun biometricSettingsSubtitle(availability: BiometricAvailability, enabled: Boolean): String {
    val base = when (availability) {
        is BiometricAvailability.Available ->
            "Ask for your fingerprint or face when the app opens."
        is BiometricAvailability.NoneEnrolled ->
            "No fingerprint or face is enrolled. Add one in your device settings first."
        is BiometricAvailability.HardwareUnavailable ->
            "The biometric sensor is not available right now. Try again later."
        is BiometricAvailability.NoHardware ->
            "This device has no biometric hardware."
        is BiometricAvailability.SecurityUpdateRequired ->
            "A security update is needed before biometrics can be used."
        is BiometricAvailability.Unknown ->
            "Biometric status could not be determined on this device."
    }
    val unavailableButOn = enabled && availability !is BiometricAvailability.Available
    return if (unavailableButOn) "$base Your password will be used instead." else base
}
