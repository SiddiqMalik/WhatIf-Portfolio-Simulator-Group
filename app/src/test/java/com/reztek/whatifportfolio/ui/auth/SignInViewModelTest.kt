package com.reztek.whatifportfolio.ui.auth

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.reztek.whatifportfolio.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Tests for SignInViewModel. FirebaseAuth is mocked, and Task results come
// from Tasks.forResult/forException so no real Firebase call is made.
class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var firebaseAuth: FirebaseAuth

    @Before
    fun setUp() {
        firebaseAuth = mockk(relaxed = true)
        every { firebaseAuth.currentUser } returns null
    }

    @Test
    fun `initial state is Idle`() {
        val viewModel = SignInViewModel(firebaseAuth)

        assertEquals(SignInUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun isAlreadySignedIn_whenCurrentUserExists_returnsTrue() {
        every { firebaseAuth.currentUser } returns mockk<FirebaseUser>()
        val viewModel = SignInViewModel(firebaseAuth)

        assertTrue(viewModel.isAlreadySignedIn())
    }

    @Test
    fun isAlreadySignedIn_whenNoCurrentUser_returnsFalse() {
        every { firebaseAuth.currentUser } returns null
        val viewModel = SignInViewModel(firebaseAuth)

        assertFalse(viewModel.isAlreadySignedIn())
    }

    @Test
    fun signIn_withBlankEmail_emitsValidationError() {
        val viewModel = SignInViewModel(firebaseAuth)

        viewModel.signIn("  ", "password1")

        val state = viewModel.uiState.value
        assertTrue(state is SignInUiState.Error)
        assertEquals("Enter your email address.", (state as SignInUiState.Error).message)
    }

    @Test
    fun signIn_whenCredentialsValid_emitsSuccess() =
        runTest(mainDispatcherRule.testDispatcher) {
            val authResult = mockk<AuthResult>()
            every { firebaseAuth.signInWithEmailAndPassword("user@example.com", "password1") } returns
                Tasks.forResult(authResult)
            val viewModel = SignInViewModel(firebaseAuth)

            viewModel.signIn("user@example.com", "password1")

            assertEquals(SignInUiState.Success, viewModel.uiState.value)
            verify { firebaseAuth.signInWithEmailAndPassword("user@example.com", "password1") }
        }

    @Test
    fun signIn_whenFirebaseFails_emitsFriendlyError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val authException = mockk<FirebaseAuthException>()
            every { authException.errorCode } returns "ERROR_INVALID_CREDENTIAL"
            every { authException.localizedMessage } returns "ignored"
            every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns
                Tasks.forException(authException)
            val viewModel = SignInViewModel(firebaseAuth)

            viewModel.signIn("user@example.com", "password1")

            val state = viewModel.uiState.value
            assertTrue(state is SignInUiState.Error)
            assertEquals("Incorrect email or password.", (state as SignInUiState.Error).message)
        }

    @Test
    fun register_whenSuccessful_emitsSuccess() =
        runTest(mainDispatcherRule.testDispatcher) {
            val authResult = mockk<AuthResult>()
            every { firebaseAuth.createUserWithEmailAndPassword("new@example.com", "password1") } returns
                Tasks.forResult(authResult)
            val viewModel = SignInViewModel(firebaseAuth)

            viewModel.register("new@example.com", "password1")

            assertEquals(SignInUiState.Success, viewModel.uiState.value)
            verify { firebaseAuth.createUserWithEmailAndPassword("new@example.com", "password1") }
        }

    @Test
    fun consumeError_afterFailedSignIn_returnsStateToIdle() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns
                Tasks.forException(RuntimeException("network unreachable"))
            val viewModel = SignInViewModel(firebaseAuth)
            viewModel.signIn("user@example.com", "password1")
            assertTrue(viewModel.uiState.value is SignInUiState.Error)

            viewModel.consumeError()

            assertEquals(SignInUiState.Idle, viewModel.uiState.value)
        }
}
