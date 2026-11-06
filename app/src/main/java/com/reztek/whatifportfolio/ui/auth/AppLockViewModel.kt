package com.reztek.whatifportfolio.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.reztek.whatifportfolio.data.auth.BIOMETRIC_NOT_RECOGNISED_MESSAGE
import com.reztek.whatifportfolio.data.auth.BiometricGatePolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class EnableLockResult { CONFIRMED, NOT_CONFIRMED }

data class AppLockUiState(
    /** null until the once-per-launch decision has been made. */
    val lockAtLaunch: Boolean? = null,
    val unlocked: Boolean = false,
    val biometricMessage: String? = null,
    val showPasswordFallback: Boolean = false,
    val passwordError: String? = null,
    val passwordLoading: Boolean = false,
    val promptPurpose: BiometricPurpose? = null,
    val enableResult: EnableLockResult? = null
) {
    val showLock: Boolean get() = BiometricGatePolicy.shouldShowLock(lockAtLaunch, unlocked)
}

/**
 * Holds the biometric lock state for the activity.
 *
 * It lives in a ViewModel (not in rememberSaveable) on purpose: the state survives
 * device rotation, so the app does not lock again mid-session, but it is NOT
 * restored after process death, so a killed-and-restored app is locked again.
 * Logging never includes the password, tokens or the account e-mail.
 */
class AppLockViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    companion object {
        private const val TAG = "AppLockViewModel"
        const val NO_PASSWORD_ACCOUNT_MESSAGE = "No email/password account is available for fallback."
        const val PASSWORD_FAILED_MESSAGE = "Password verification failed. Check your password and try again."
    }

    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    private var passwordJob: Job? = null

    /** Decides once per launch; later changes to the setting do not lock the current session. */
    fun decideLockAtLaunch(biometricEnabled: Boolean, hasFirebaseSession: Boolean) {
        _uiState.update { state ->
            if (state.lockAtLaunch != null) {
                state
            } else {
                val lock = BiometricGatePolicy.shouldLockAtLaunch(hasFirebaseSession, biometricEnabled)
                Log.i(TAG, "Lock decided for this launch: $lock")
                state.copy(lockAtLaunch = lock)
            }
        }
    }

    fun beginPrompt(purpose: BiometricPurpose): Boolean {
        var started = false
        _uiState.update { state ->
            if (state.promptPurpose != null) {
                state
            } else {
                started = true
                state.copy(
                    promptPurpose = purpose,
                    enableResult = if (purpose == BiometricPurpose.ENABLE_LOCK) null else state.enableResult
                )
            }
        }
        return started
    }

    fun onBiometricSuccess() {
        _uiState.update { state ->
            if (state.promptPurpose == BiometricPurpose.ENABLE_LOCK) {
                Log.i(TAG, "Biometric confirmed for enabling the lock")
                state.copy(promptPurpose = null, enableResult = EnableLockResult.CONFIRMED)
            } else {
                Log.i(TAG, "Unlocked with biometrics")
                state.copy(
                    promptPurpose = null,
                    unlocked = true,
                    biometricMessage = null,
                    passwordError = null
                )
            }
        }
    }

    fun onBiometricNotRecognised() {
        _uiState.update { state ->
            if (state.promptPurpose == BiometricPurpose.ENABLE_LOCK) {
                state
            } else {
                state.copy(biometricMessage = BIOMETRIC_NOT_RECOGNISED_MESSAGE)
            }
        }
    }

    fun onBiometricError(message: String, openPasswordFallback: Boolean) {
        _uiState.update { state ->
            if (state.promptPurpose == BiometricPurpose.ENABLE_LOCK) {
                state.copy(promptPurpose = null, enableResult = EnableLockResult.NOT_CONFIRMED)
            } else {
                state.copy(
                    promptPurpose = null,
                    biometricMessage = message,
                    showPasswordFallback = state.showPasswordFallback || openPasswordFallback
                )
            }
        }
    }

    fun consumeEnableResult() {
        _uiState.update { it.copy(enableResult = null) }
    }

    fun signOut() {
        Log.i(TAG, "User signed out from the lock screen")
        passwordJob?.cancel()
        auth.signOut()
        _uiState.update {
            it.copy(
                lockAtLaunch = false,
                unlocked = false,
                promptPurpose = null,
                biometricMessage = null,
                showPasswordFallback = false,
                passwordError = null,
                passwordLoading = false
            )
        }
    }

    fun onShowPasswordFallbackChange(show: Boolean) {
        _uiState.update { it.copy(showPasswordFallback = show) }
    }

    /** Re-verifies the signed-in user's account password with Firebase; never logs it. */
    fun onPasswordSubmitted(password: String) {
        val user = auth.currentUser
        val email = user?.email
        if (user == null || email.isNullOrBlank()) {
            Log.w(TAG, "Password fallback unavailable: no email/password account")
            _uiState.update { it.copy(passwordError = NO_PASSWORD_ACCOUNT_MESSAGE) }
            return
        }
        _uiState.update { it.copy(passwordLoading = true, passwordError = null) }
        passwordJob?.cancel()
        passwordJob = viewModelScope.launch {
            try {
                user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
                Log.i(TAG, "Unlocked with password fallback")
                _uiState.update { it.copy(unlocked = true, biometricMessage = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Password fallback failed: ${e.javaClass.simpleName}")
                _uiState.update { it.copy(passwordError = PASSWORD_FAILED_MESSAGE) }
            } finally {
                _uiState.update { it.copy(passwordLoading = false) }
            }
        }
    }
}
