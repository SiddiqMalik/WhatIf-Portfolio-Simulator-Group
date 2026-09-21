package com.reztek.whatifportfolio.ui.auth

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.reztek.whatifportfolio.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
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
    private lateinit var credential: AuthCredential

    @Before
    fun setUp() {
        firebaseAuth = mockk(relaxed = true)
        credential = mockk()

        // Stub the static factory so we don't touch real Google Play Services.
        mockkStatic(GoogleAuthProvider::class)
        every { GoogleAuthProvider.getCredential(any(), any()) } returns credential

        every { firebaseAuth.currentUser } returns null
    }

    @After
    fun tearDown() {
        unmockkStatic(GoogleAuthProvider::class)
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
    fun signInWithGoogleIdToken_whenCredentialExchangeSucceeds_emitsSuccess() =
        runTest(mainDispatcherRule.testDispatcher) {
            val authResult = mockk<AuthResult>()
            every { firebaseAuth.signInWithCredential(credential) } returns
                Tasks.forResult(authResult)
            val viewModel = SignInViewModel(firebaseAuth)

            viewModel.signInWithGoogleIdToken("token-abc")

            assertEquals(SignInUiState.Success, viewModel.uiState.value)
            verify { GoogleAuthProvider.getCredential("token-abc", null) }
            verify { firebaseAuth.signInWithCredential(credential) }
        }

    @Test
    fun signInWithGoogleIdToken_whenFirebaseFails_emitsErrorWithFirebaseMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { firebaseAuth.signInWithCredential(credential) } returns
                Tasks.forException(RuntimeException("Token has expired"))
            val viewModel = SignInViewModel(firebaseAuth)

            viewModel.signInWithGoogleIdToken("expired-token")

            val state = viewModel.uiState.value
            assertTrue(state is SignInUiState.Error)
            assertEquals("Token has expired", (state as SignInUiState.Error).message)
        }

    @Test
    fun signInWithGoogleIdToken_whenFailureHasNoMessage_emitsFallbackMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            // A null exception message should fall back to a real message.
            every { firebaseAuth.signInWithCredential(credential) } returns
                Tasks.forException(RuntimeException())
            val viewModel = SignInViewModel(firebaseAuth)

            viewModel.signInWithGoogleIdToken("token")

            val state = viewModel.uiState.value
            assertTrue(state is SignInUiState.Error)
            assertEquals(
                "Sign-in failed. Please try again.",
                (state as SignInUiState.Error).message
            )
        }

    @Test
    fun onSignInCancelled_returnsStateToIdle() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { firebaseAuth.signInWithCredential(credential) } returns
                Tasks.forException(RuntimeException("boom"))
            val viewModel = SignInViewModel(firebaseAuth)
            viewModel.signInWithGoogleIdToken("token")

            viewModel.onSignInCancelled()

            assertEquals(SignInUiState.Idle, viewModel.uiState.value)
        }

    @Test
    fun consumeError_afterFailedSignIn_returnsStateToIdle() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { firebaseAuth.signInWithCredential(credential) } returns
                Tasks.forException(RuntimeException("network unreachable"))
            val viewModel = SignInViewModel(firebaseAuth)
            viewModel.signInWithGoogleIdToken("token")
            assertTrue(viewModel.uiState.value is SignInUiState.Error)

            viewModel.consumeError()

            assertEquals(SignInUiState.Idle, viewModel.uiState.value)
        }
}
