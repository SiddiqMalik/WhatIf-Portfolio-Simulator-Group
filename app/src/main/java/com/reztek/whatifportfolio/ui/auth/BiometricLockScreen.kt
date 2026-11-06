package com.reztek.whatifportfolio.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.reztek.whatifportfolio.R
import com.reztek.whatifportfolio.ui.i18n.localized

/**
 * Compose lock surface shown while a saved Firebase session is protected by biometrics.
 *
 * @param errorMessage biometric result to show (cancelled, not recognised, lockout...).
 * @param autoPrompt open the biometric prompt automatically when the screen first appears;
 * the button stays available as the retry.
 * @param biometricAvailable false hides the biometric button when strong biometrics
 * cannot be used, leaving the password fallback as the only way in.
 * @param showPasswordFallback / [onShowPasswordFallbackChange] let the caller open the
 * password section (for example after a lockout).
 */
@Composable
fun BiometricLockScreen(
    onUnlock: () -> Unit,
    onPasswordUnlock: (String) -> Unit,
    message: String? = null,
    errorMessage: String? = null,
    passwordError: String? = null,
    passwordLoading: Boolean = false,
    autoPrompt: Boolean = false,
    biometricAvailable: Boolean = true,
    showPasswordFallback: Boolean = false,
    onShowPasswordFallbackChange: (Boolean) -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    var password by remember { mutableStateOf("") }

    if (autoPrompt && biometricAvailable) {
        LaunchedEffect(Unit) {
            onUnlock()
        }
    onShowPasswordFallbackChange: (Boolean) -> Unit = {}
) {
    // Never persisted across rotation on purpose: a typed password must not be saved.
    var password by remember { mutableStateOf("") }

    // Open the prompt without a tap; the button below is the retry.
    LaunchedEffect(autoPrompt) {
        if (autoPrompt) onUnlock()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(message ?: "Use your device biometrics to continue.", style = MaterialTheme.typography.bodyMedium)
            if (errorMessage != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(24.dp))
            if (biometricAvailable) {
                Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) {
                    Text("Unlock with biometrics")
                }
            }
            TextButton(onClick = { onShowPasswordFallbackChange(!showPasswordFallback) }) {
                Text(if (showPasswordFallback) "Hide password fallback" else "Use email password instead")
            Text(localized(message ?: stringResource(R.string.lock_message_default)), style = MaterialTheme.typography.bodyMedium)
            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(localized(errorMessage), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
            if (biometricAvailable) {
                Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.lock_unlock_biometric))
                }
                TextButton(onClick = { onShowPasswordFallbackChange(!showPasswordFallback) }) {
                    Text(
                        if (showPasswordFallback) stringResource(R.string.lock_hide_password)
                        else stringResource(R.string.lock_use_password)
                    )
                }
            }
            if (showPasswordFallback || !biometricAvailable) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.lock_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !passwordLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                if (passwordError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        passwordError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(localized(passwordError), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onPasswordUnlock(password) },
                    enabled = password.isNotBlank() && !passwordLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (passwordLoading) stringResource(R.string.lock_verifying) else stringResource(R.string.lock_unlock_password)) }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onSignOut, enabled = !passwordLoading) {
                Text("Sign out", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
