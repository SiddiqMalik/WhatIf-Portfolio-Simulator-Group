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

    /**
     * Decides, once per launch, whether this session is protected. Deciding once
     * means switching the setting on in Settings does not lock the user out
     * mid-session; the lock applies from the next fresh launch.
     */
    fun shouldLockAtLaunch(hasFirebaseSession: Boolean, biometricEnabled: Boolean): Boolean =
        hasFirebaseSession && biometricEnabled

    /** The lock screen is shown while the launch decision is "lock" and the user has not unlocked yet. */
    fun shouldShowLock(lockAtLaunch: Boolean?, biometricUnlocked: Boolean): Boolean =
        lockAtLaunch == true && !biometricUnlocked
}
