package com.reztek.whatifportfolio.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricGatePolicyTest {

    @Test
    fun shouldLock_waitsForPreferencesToLoad() {
        assertFalse(
            BiometricGatePolicy.shouldLock(
                preferencesLoaded = false,
                hasFirebaseSession = true,
                biometricEnabled = true,
                biometricUnlocked = false
            )
        )
    }

    @Test
    fun shouldLock_requiresAnAuthenticatedFirebaseSession() {
        assertFalse(
            BiometricGatePolicy.shouldLock(
                preferencesLoaded = true,
                hasFirebaseSession = false,
                biometricEnabled = true,
                biometricUnlocked = false
            )
        )
    }

    @Test
    fun shouldLock_returnsFalseWhenPreferenceIsDisabled() {
        assertFalse(
            BiometricGatePolicy.shouldLock(
                preferencesLoaded = true,
                hasFirebaseSession = true,
                biometricEnabled = false,
                biometricUnlocked = false
            )
        )
    }

    @Test
    fun shouldLock_returnsFalseAfterSuccessfulUnlock() {
        assertFalse(
            BiometricGatePolicy.shouldLock(
                preferencesLoaded = true,
                hasFirebaseSession = true,
                biometricEnabled = true,
                biometricUnlocked = true
            )
        )
    }

    @Test
    fun shouldLock_returnsTrueForEnabledProtectedSessionNotYetUnlocked() {
        assertTrue(
            BiometricGatePolicy.shouldLock(
                preferencesLoaded = true,
                hasFirebaseSession = true,
                biometricEnabled = true,
                biometricUnlocked = false
            )
        )
    }
}
