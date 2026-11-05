package com.reztek.whatifportfolio.data.auth

import android.content.Context
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

    /**
     * Checks whether biometric authentication is available on this device.
     *
     * Strong biometrics are checked first. If strong biometrics are not
     * available, weak biometrics are checked as a fallback.
     */
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

        return mapResultCode(weakResult)
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