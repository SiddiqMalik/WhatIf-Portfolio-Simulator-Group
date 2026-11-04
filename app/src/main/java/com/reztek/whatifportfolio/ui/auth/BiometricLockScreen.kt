package com.reztek.whatifportfolio.ui.auth

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Compose lock surface shown while a saved Firebase session is protected by biometrics. */
@Composable
fun BiometricLockScreen(
    onUnlock: () -> Unit,
    message: String? = null
) {
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
            Text(
                message ?: "Use your device biometrics to continue.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUnlock) { Text("Unlock with biometrics") }
        }
    }
}
