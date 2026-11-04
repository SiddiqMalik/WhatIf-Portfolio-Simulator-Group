package com.reztek.whatifportfolio.data.auth

/** Pure decision logic for whether the saved Firebase session should be gated. */
object BiometricGatePolicy {
    /**
     * Returns true only after preferences have loaded, when a Firebase session
     * exists, biometric lock is enabled, and this process has not unlocked yet.
     */
    fun shouldLock(
        preferencesLoaded: Boolean,
        hasFirebaseSession: Boolean,
        biometricEnabled: Boolean,
        biometricUnlocked: Boolean
    ): Boolean = preferencesLoaded && hasFirebaseSession && biometricEnabled && !biometricUnlocked
}
