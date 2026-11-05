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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/** Compose lock surface shown while a saved Firebase session is protected by biometrics. */
@Composable
fun BiometricLockScreen(
    onUnlock: () -> Unit,
    onPasswordUnlock: (String) -> Unit,
    message: String? = null,
    passwordError: String? = null,
<<<<<<< Updated upstream
    passwordLoading: Boolean = false
=======
    passwordLoading: Boolean = false,
    autoPrompt: Boolean = false,
    biometricAvailable: Boolean = true,
    showPasswordFallback: Boolean = false,
    onShowPasswordFallbackChange: (Boolean) -> Unit = {},
    onSignOut: () -> Unit = {}
>>>>>>> Stashed changes
) {
    var showPasswordFallback by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("WhatIf Portfolio", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text("Your app is locked", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(message ?: "Use your device biometrics to continue.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) {
                Text("Unlock with biometrics")
            }
            TextButton(onClick = { showPasswordFallback = !showPasswordFallback }) {
                Text(if (showPasswordFallback) "Hide password fallback" else "Use email password instead")
            }
            if (showPasswordFallback) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Account password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !passwordLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                if (passwordError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(passwordError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onPasswordUnlock(password) },
                    enabled = password.isNotBlank() && !passwordLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (passwordLoading) "Verifying…" else "Unlock with password") }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onSignOut, enabled = !passwordLoading) {
                Text("Sign out", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
