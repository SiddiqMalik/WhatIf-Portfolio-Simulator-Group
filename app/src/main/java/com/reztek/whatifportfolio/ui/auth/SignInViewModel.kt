package com.reztek.whatifportfolio.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * UI state for the Login screen. A sealed hierarchy rather than a handful of
 * booleans, so the screen can only ever be in exactly one state at a time —
 * this is the "local UI state holder" pattern used consistently across all
 * three of Person 1's screens.
 */
sealed interface SignInUiState {
    data object Idle : SignInUiState
    data object Loading : SignInUiState
    data class Error(val message: String) : SignInUiState
    data object Success : SignInUiState
}

/**
 * Handles the "Sign in with Google" flow via Firebase Authentication.
 *
 * The actual Google credential is obtained in the Activity using
 * CredentialManager (Android's current recommended API, replacing the
 * deprecated GoogleSignInClient — see Google, 2026, "Credential Manager for
 * Android"); this ViewModel only owns the resulting UI state and the
 * Firebase side of the exchange, keeping the ViewModel testable and free of
 * Activity/Context references.
 */
class SignInViewModel(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    companion object {
        private const val TAG = "SignInViewModel"
    }

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "SignInViewModel created")
    }

    /** Whether a Firebase session already exists — lets the NavHost skip Login entirely. */
    fun isAlreadySignedIn(): Boolean = firebaseAuth.currentUser != null

    /**
     * Exchanges a Google ID token (obtained by the caller via Credential
     * Manager) for a Firebase session.
     */
    fun signInWithGoogleIdToken(idToken: String) {
        Log.i(TAG, "Beginning Firebase credential exchange")
        _uiState.value = SignInUiState.Loading
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(credential).await()
                Log.i(TAG, "Firebase sign-in succeeded for uid=${firebaseAuth.currentUser?.uid}")
                _uiState.value = SignInUiState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Firebase sign-in failed", e)
                _uiState.value = SignInUiState.Error(
                    e.localizedMessage ?: "Sign-in failed. Please try again."
                )
            }
        }
    }

    fun onSignInCancelled() {
        Log.d(TAG, "Google sign-in flow cancelled by user")
        _uiState.value = SignInUiState.Idle
    }

    fun consumeError() {
        _uiState.value = SignInUiState.Idle
    }
}

// Note: `.await()` on the Firebase Task returned by signInWithCredential()
// comes from the `kotlinx-coroutines-play-services` artifact
// (org.jetbrains.kotlinx:kotlinx-coroutines-play-services). Add it as a
// module dependency — see build.gradle.kts notes in README.md.
