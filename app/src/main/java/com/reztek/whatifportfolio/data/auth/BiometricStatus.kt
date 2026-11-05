
package com.reztek.whatifportfolio.data.auth

import android.content.Context
import androidx.biometric.BiometricManager

/**
 * Describes whether biometric authentication can be used on this device.
 *
 * Use checkAvailability() before displaying or launching a biometric prompt.
 */
sealed class BiometricAvailability {
    data object Available : BiometricAvailability()
    data object NoneEnrolled : BiometricAvailability()
    data object NoHardware : BiometricAvailability()
    data object HardwareUnavailable : BiometricAvailability()
    data object SecurityUpdateRequired : BiometricAvailability()
    data class Unknown(val reason: String? = null) : BiometricAvailability()
}

/**
 * Checks biometric availability using AndroidX BiometricManager.
 *
 * Class 3 biometrics (strong fingerprint/face authentication) are preferred.
 * If the device does not support Class 3, Class 2 biometrics are checked as
 * a fallback.
 */
object BiometricStatus {

    fun checkAvailability(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)

        val strongResult = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        if (strongResult == BiometricManager.BIOMETRIC_SUCCESS) {
            return BiometricAvailability.Available
        }

        val weakResult = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        return when (weakResult) {
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

            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                BiometricAvailability.Unknown(
                    "Biometric availability could not be determined."
                )

            else ->
                BiometricAvailability.Unknown(
                    "BiometricManager returned status code: $weakResult"
                )
        }
    }
}
