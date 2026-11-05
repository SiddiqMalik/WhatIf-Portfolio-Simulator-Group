package com.reztek.whatifportfolio.data.auth

import androidx.biometric.BiometricPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests for biometricErrorMessage - a pure function, so no prompt or Context is needed.
class BiometricErrorMapperTest {

    @Test
    fun negativeButton_isCancellation_withoutFallback() {
        val info = biometricErrorMessage(BiometricPrompt.ERROR_NEGATIVE_BUTTON, "Cancel")

        assertTrue(info.message.startsWith("Unlock cancelled"))
        assertFalse(info.openPasswordFallback)
    }

    @Test
    fun userCanceled_isCancellation_withoutFallback() {
        val info = biometricErrorMessage(BiometricPrompt.ERROR_USER_CANCELED, "User canceled")

        assertTrue(info.message.startsWith("Unlock cancelled"))
        assertFalse(info.openPasswordFallback)
    }

    @Test
    fun systemCanceled_asksToTryAgain_withoutFallback() {
        val info = biometricErrorMessage(BiometricPrompt.ERROR_CANCELED, "Canceled")

        assertTrue(info.message.contains("interrupted"))
        assertFalse(info.openPasswordFallback)
    }

    @Test
    fun lockout_opensPasswordFallback() {
        val info = biometricErrorMessage(BiometricPrompt.ERROR_LOCKOUT, "Too many attempts")

        assertTrue(info.message.contains("Too many attempts"))
        assertTrue(info.openPasswordFallback)
    }

    @Test
    fun permanentLockout_opensPasswordFallback() {
        val info = biometricErrorMessage(BiometricPrompt.ERROR_LOCKOUT_PERMANENT, "Locked")

        assertTrue(info.message.contains("locked"))
        assertTrue(info.openPasswordFallback)
    }

    @Test
    fun noBiometricsEnrolled_opensPasswordFallback() {
        val info = biometricErrorMessage(BiometricPrompt.ERROR_NO_BIOMETRICS, "None enrolled")

        assertTrue(info.message.contains("enrolled"))
        assertTrue(info.openPasswordFallback)
    }

    @Test
    fun hardwareNotPresentOrUnavailable_opensPasswordFallback() {
        listOf(
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE,
            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL
        ).forEach { code ->
            val info = biometricErrorMessage(code, "platform text")

            assertEquals("Biometrics are unavailable. Use your password instead.", info.message)
            assertTrue("code $code should open the fallback", info.openPasswordFallback)
        }
    }

    @Test
    fun timeout_asksToTryAgain_withoutFallback() {
        val info = biometricErrorMessage(BiometricPrompt.ERROR_TIMEOUT, "Timeout")

        assertTrue(info.message.contains("timed out"))
        assertFalse(info.openPasswordFallback)
    }

    @Test
    fun unableToProcess_asksToTryAgain_withoutFallback() {
        val info = biometricErrorMessage(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, "Unable")

        assertTrue(info.message.contains("Try again"))
        assertFalse(info.openPasswordFallback)
    }

    @Test
    fun unknownCode_usesPlatformMessage_withoutFallback() {
        val info = biometricErrorMessage(-999, "Vendor specific failure")

        assertEquals("Vendor specific failure", info.message)
        assertFalse(info.openPasswordFallback)
    }

    @Test
    fun unknownCodeWithBlankPlatformMessage_usesGenericMessage() {
        val info = biometricErrorMessage(-999, "  ")

        assertTrue(info.message.contains("Biometric authentication failed"))
        assertFalse(info.openPasswordFallback)
    }

    @Test
    fun notRecognisedMessage_isStable() {
        assertEquals("Not recognised. Try again.", BIOMETRIC_NOT_RECOGNISED_MESSAGE)
    }
}
