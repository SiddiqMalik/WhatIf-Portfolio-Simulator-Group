package com.reztek.whatifportfolio.ui.auth

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reztek.whatifportfolio.ui.theme.NavyDeep
import com.reztek.whatifportfolio.ui.theme.TealPrimary

private const val TAG = "SignInScreen"

/**
 * Login screen (Deliverable 3, Screen 1). Entry point for unauthenticated
 * users — establishes a Firebase session before Dashboard, Settings or any
 * other protected screen becomes reachable.
 *
 * The actual Google credential prompt is triggered by [onSignInClicked],
 * which the hosting Activity wires up to Android's Credential Manager (see
 * MainActivity). This screen only renders state and reports the resulting
 * ID token back to the ViewModel — kept this way so the composable itself
 * has no Activity dependency and previews cleanly.
 */
@Composable
fun SignInScreen(
    onSignInClicked: () -> Unit,
    onSignedIn: () -> Unit,
    viewModel: SignInViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Lifecycle/state-transition logging, as required by the technical
    // deliverables ("functional logging ... to demonstrate a clear
    // programmatic understanding of your application's ... state transitions").
    LaunchedEffect(uiState) {
        Log.d(TAG, "SignInUiState -> $uiState")
        if (uiState is SignInUiState.Success) {
            onSignedIn()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDeep, MaterialTheme.colorScheme.background)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandMark()

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "What-If Portfolio Simulator",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "See what a regular investment strategy would have " +
                    "been worth — and how it stacks up against inflation.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            GoogleSignInButton(
                isLoading = uiState is SignInUiState.Loading,
                onClick = {
                    Log.i(TAG, "Sign-in CTA tapped")
                    onSignInClicked()
                }
            )

            if (uiState is SignInUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as SignInUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "By continuing you agree to the Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(TealPrimary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ShowChart,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun GoogleSignInButton(isLoading: Boolean, onClick: () -> Unit) {
    // Styled as a light "on dark" surface button so it reads as the Google
    // sign-in convention while still fitting the app's teal/navy language.
    androidx.compose.material3.Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = NavyDeep
                )
            } else {
                Text(
                    text = "Sign in with Google",
                    color = NavyDeep,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
