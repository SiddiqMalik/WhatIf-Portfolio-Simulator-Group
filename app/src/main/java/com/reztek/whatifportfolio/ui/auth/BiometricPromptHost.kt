package com.reztek.whatifportfolio.ui.auth

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.reztek.whatifportfolio.R
import com.reztek.whatifportfolio.data.auth.BIOMETRIC_NOT_RECOGNISED_MESSAGE
import com.reztek.whatifportfolio.data.auth.BiometricStatus
import com.reztek.whatifportfolio.data.auth.biometricErrorMessage

private const val TAG = "BiometricPromptHost"

/**
 * Creates a strong-biometric prompt bound to the hosting FragmentActivity and
 * returns a function that shows it.
 *
 * The BiometricPrompt is created once per activity (not on every recomposition);
 * the callbacks are read through rememberUpdatedState so they always call the
 * latest lambdas without rebuilding the prompt.
 *
 * @param onAuthenticationError receives the message to show and whether the
 * password fallback should be opened.
 *
 * Logging records only outcomes and error codes - never passwords, tokens or
 * personal data.
 */
@Composable
fun rememberBiometricPrompt(
    onAuthenticated: () -> Unit,
    onAuthenticationError: (message: String, openPasswordFallback: Boolean) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val latestOnAuthenticated by rememberUpdatedState(onAuthenticated)
    val latestOnError by rememberUpdatedState(onAuthenticationError)

    val prompt = remember(activity) {
        activity?.let { host ->
            BiometricPrompt(
                host,
                // ContextCompat works on minSdk 26; Context.mainExecutor needs API 28.
                ContextCompat.getMainExecutor(host),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        Log.i(TAG, "Biometric authentication succeeded")
                        latestOnAuthenticated()
                    }

                    override fun onAuthenticationFailed() {
                        // The prompt stays open and lets the user try again.
                        Log.w(TAG, "Biometric attempt not recognised")
                        latestOnError(BIOMETRIC_NOT_RECOGNISED_MESSAGE, false)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        val info = biometricErrorMessage(errorCode, errString.toString())
                        Log.w(TAG, "Biometric error code=$errorCode openFallback=${info.openPasswordFallback}")
                        latestOnError(info.message, info.openPasswordFallback)
                    }
                }
            )
        }
    }

    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.biometric_prompt_subtitle)
    val promptCancel = stringResource(R.string.action_cancel)
    val promptInfo = remember(promptTitle, promptSubtitle, promptCancel) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setSubtitle(promptSubtitle)
            // Same constant the availability check uses, so Settings and the prompt agree.
            .setAllowedAuthenticators(BiometricStatus.REQUIRED_AUTHENTICATORS)
            .setNegativeButtonText(promptCancel)
            .build()
    }

    return remember(prompt, promptInfo) {
        val show: () -> Unit = {
            if (prompt == null) {
                Log.e(TAG, "Cannot show biometric prompt: host is not a FragmentActivity")
                latestOnError(
                    context.getString(R.string.biometric_error_unavailable),
                    true
                )
            } else {
                Log.d(TAG, "Showing biometric prompt")
                prompt.authenticate(promptInfo)
            }
        }
        show
    }
}

/** Walks the ContextWrapper chain to find the hosting FragmentActivity, if any. */
private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
