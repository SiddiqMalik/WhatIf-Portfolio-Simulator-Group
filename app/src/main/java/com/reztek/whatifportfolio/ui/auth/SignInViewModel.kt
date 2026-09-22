package com.reztek.whatifportfolio.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
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
 * Handles email/password sign-in and registration via Firebase Authentication.
 * The resulting Firebase ID token is attached to REST calls by [AuthInterceptor].
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
        if (firebaseAuth.currentUser != null) {
            _uiState.value = SignInUiState.Success
        }
    }

    /** Whether a Firebase session already exists — lets the NavHost skip Login entirely. */
    fun isAlreadySignedIn(): Boolean = firebaseAuth.currentUser != null

    fun signIn(email: String, password: String) {
        val trimmedEmail = email.trim()
        val validationError = validate(trimmedEmail, password)
        if (validationError != null) {
            _uiState.value = SignInUiState.Error(validationError)
            return
        }

        Log.i(TAG, "Beginning email/password sign-in")
        _uiState.value = SignInUiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).await()
                Log.i(TAG, "Firebase sign-in succeeded for uid=${firebaseAuth.currentUser?.uid}")
                _uiState.value = SignInUiState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Firebase sign-in failed", e)
                _uiState.value = SignInUiState.Error(friendlyAuthMessage(e, signingUp = false))
            }
        }
    }

    fun register(email: String, password: String) {
        val trimmedEmail = email.trim()
        val validationError = validate(trimmedEmail, password)
        if (validationError != null) {
            _uiState.value = SignInUiState.Error(validationError)
            return
        }

        Log.i(TAG, "Beginning email/password registration")
        _uiState.value = SignInUiState.Loading
        viewModelScope.launch {
            try {
                firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
                Log.i(TAG, "Firebase registration succeeded for uid=${firebaseAuth.currentUser?.uid}")
                _uiState.value = SignInUiState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Firebase registration failed", e)
                _uiState.value = SignInUiState.Error(friendlyAuthMessage(e, signingUp = true))
            }
        }
    }

    fun consumeError() {
        _uiState.value = SignInUiState.Idle
    }

    private fun validate(email: String, password: String): String? = when {
        email.isBlank() -> "Enter your email address."
        "@" !in email || "." !in email.substringAfter("@") -> "Enter a valid email address."
        password.length < 6 -> "Password must be at least 6 characters."
        else -> null
    }

    private fun friendlyAuthMessage(e: Exception, signingUp: Boolean): String {
        val code = (e as? FirebaseAuthException)?.errorCode
        return when (code) {
            "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                "Incorrect email or password."
            "ERROR_USER_NOT_FOUND" ->
                "No account found for that email. Create one first."
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "That email is already registered. Sign in instead."
            "ERROR_WEAK_PASSWORD" ->
                "Password must be at least 6 characters."
            "ERROR_NETWORK_REQUEST_FAILED" ->
                "Network error. Check your connection and try again."
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "Email/password sign-in is disabled in Firebase Console."
            else -> e.localizedMessage
                ?: if (signingUp) "Registration failed. Please try again."
                else "Sign-in failed. Please try again."
        }
    }
}
