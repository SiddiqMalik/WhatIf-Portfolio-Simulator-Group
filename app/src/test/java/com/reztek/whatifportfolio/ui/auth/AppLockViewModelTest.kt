package com.reztek.whatifportfolio.ui.auth

import com.google.firebase.auth.FirebaseAuth
import com.reztek.whatifportfolio.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Tests the lock state holder. No password/Firebase round-trip is exercised here;
// that part needs a real device (see README manual test checklist).
class AppLockViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: AppLockViewModel

    @Before
    fun setUp() {
        auth = mockk(relaxed = true)
        every { auth.currentUser } returns null
        viewModel = AppLockViewModel(auth)
    }

    @Test
    fun beforeTheDecision_nothingIsShownAsLocked() {
        val state = viewModel.uiState.value

        assertNull(state.lockAtLaunch)
        assertFalse(state.showLock)
    }

    @Test
    fun enabledWithSession_locksAtLaunch() {
        viewModel.decideLockAtLaunch(biometricEnabled = true, hasFirebaseSession = true)

        assertTrue(viewModel.uiState.value.showLock)
    }

    @Test
    fun decisionIsMadeOnlyOnce_soEnablingTheSettingLaterDoesNotLockTheSession() {
        viewModel.decideLockAtLaunch(biometricEnabled = false, hasFirebaseSession = true)
        viewModel.decideLockAtLaunch(biometricEnabled = true, hasFirebaseSession = true)

        assertEquals(false, viewModel.uiState.value.lockAtLaunch)
        assertFalse(viewModel.uiState.value.showLock)
    }

    @Test
    fun noSession_doesNotLock() {
        viewModel.decideLockAtLaunch(biometricEnabled = true, hasFirebaseSession = false)

        assertFalse(viewModel.uiState.value.showLock)
    }

    @Test
    fun biometricSuccess_unlocksAndClearsMessage() {
        viewModel.decideLockAtLaunch(biometricEnabled = true, hasFirebaseSession = true)
        viewModel.onBiometricError("Not recognised. Try again.", false)

        viewModel.onBiometricSuccess()

        val state = viewModel.uiState.value
        assertFalse(state.showLock)
        assertNull(state.biometricMessage)
    }

    @Test
    fun errorWithFallbackFlag_opensPasswordSection_andKeepsTheMessage() {
        viewModel.onBiometricError("Too many attempts.", true)

        val state = viewModel.uiState.value
        assertEquals("Too many attempts.", state.biometricMessage)
        assertTrue(state.showPasswordFallback)
    }

    @Test
    fun cancellation_keepsPasswordSectionClosed() {
        viewModel.onBiometricError("Unlock cancelled.", false)

        assertFalse(viewModel.uiState.value.showPasswordFallback)
    }

    @Test
    fun passwordFallback_withoutAnEmailAccount_showsAnError() {
        viewModel.onPasswordSubmitted("anything")

        val state = viewModel.uiState.value
        assertEquals(AppLockViewModel.NO_PASSWORD_ACCOUNT_MESSAGE, state.passwordError)
        assertFalse(state.passwordLoading)
    }
}
