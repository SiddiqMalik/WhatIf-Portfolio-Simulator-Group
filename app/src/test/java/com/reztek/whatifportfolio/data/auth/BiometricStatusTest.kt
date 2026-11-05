package com.reztek.whatifportfolio.data.auth

import androidx.biometric.BiometricManager
import org.junit.Assert.assertEquals
import org.junit.Test

// Tests for BiometricStatus.mapResultCode - a pure function, so no Context
// or BiometricManager instance needs to be mocked.
class BiometricStatusTest {

    @Test
    fun mapResultCode_withSuccessCode_returnsAvailable() {
        val result = BiometricStatus.mapResultCode(BiometricManager.BIOMETRIC_SUCCESS)

        assertEquals(BiometricAvailability.Available, result)
    }

    @Test
    fun mapResultCode_withNoneEnrolledCode_returnsNoneEnrolled() {
        val result = BiometricStatus.mapResultCode(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)

        assertEquals(BiometricAvailability.NoneEnrolled, result)
    }

    @Test
    fun mapResultCode_withHardwareUnavailableCode_returnsHardwareUnavailable() {
        val result = BiometricStatus.mapResultCode(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE)

        assertEquals(BiometricAvailability.HardwareUnavailable, result)
    }

    @Test
    fun mapResultCode_withNoHardwareCode_returnsNoHardware() {
        val result = BiometricStatus.mapResultCode(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)

        assertEquals(BiometricAvailability.NoHardware, result)
    }

    @Test
    fun mapResultCode_withSecurityUpdateRequiredCode_returnsSecurityUpdateRequired() {
        val result =
            BiometricStatus.mapResultCode(BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED)

        assertEquals(BiometricAvailability.SecurityUpdateRequired, result)
    }

    @Test
    fun mapResultCode_withUnrecognisedCode_returnsUnknownWithThatCode() {
        val unexpectedCode = -999

        val result = BiometricStatus.mapResultCode(unexpectedCode)

        assertEquals(BiometricAvailability.Unknown(unexpectedCode), result)
    }

    @Test
    fun requiredAuthenticators_isStrongOnly() {
        assertEquals(
            BiometricManager.Authenticators.BIOMETRIC_STRONG,
            BiometricStatus.REQUIRED_AUTHENTICATORS
        )
    }

    @Test
    fun checkAvailabilityWith_queriesOnlyTheStrongAuthenticator() {
        val queried = mutableListOf<Int>()

        val result = BiometricStatus.checkAvailabilityWith { authenticators ->
            queried.add(authenticators)
            BiometricManager.BIOMETRIC_SUCCESS
        }

        assertEquals(BiometricAvailability.Available, result)
        assertEquals(listOf(BiometricManager.Authenticators.BIOMETRIC_STRONG), queried)
    }

    @Test
    fun checkAvailabilityWith_weakOnlyDevice_isNotAvailable() {
        // A device whose only biometric is weak: the platform says "success" for
        // WEAK but not for STRONG. The prompt needs STRONG, so Settings must agree.
        val result = BiometricStatus.checkAvailabilityWith { authenticators ->
            if (authenticators == BiometricManager.Authenticators.BIOMETRIC_WEAK) {
                BiometricManager.BIOMETRIC_SUCCESS
            } else {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
            }
        }

        assertEquals(BiometricAvailability.NoneEnrolled, result)
    }

    @Test
    fun checkAvailabilityWith_mapsPlatformErrorsFromTheStrongCheck() {
        val result = BiometricStatus.checkAvailabilityWith {
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
        }

        assertEquals(BiometricAvailability.HardwareUnavailable, result)
    }
}
