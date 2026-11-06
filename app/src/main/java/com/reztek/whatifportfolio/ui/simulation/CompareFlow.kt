package com.reztek.whatifportfolio.ui.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reztek.whatifportfolio.ui.components.InlineErrorBanner
import com.reztek.whatifportfolio.ui.components.PrimaryActionButton

@Composable
fun CompareSelectionRoute(
    onCompareSelected: (firstId: String, secondId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: SavedSimulationsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    when (val current = state) {
        is SavedListUiState.Loading -> CenteredProgress()
        is SavedListUiState.Error -> CenteredMessage(
            message = current.message,
            onRetry = viewModel::refresh,
            onBack = onBack
        )
        is SavedListUiState.Ready -> CompareSelectionScreen(
            savedItems = current.items,
            onCompareSelected = onCompareSelected,
            onBack = onBack
        )
    }
}

@Composable
fun CompareResultRoute(
    firstKey: String,
    secondKey: String,
    onBack: () -> Unit,
    viewModel: CompareViewModel = viewModel()
) {
    LaunchedEffect(firstKey, secondKey) { viewModel.load(firstKey, secondKey) }
    val state by viewModel.uiState.collectAsState()
    when (val current = state) {
        is CompareUiState.Loading -> CenteredProgress()
        is CompareUiState.Error -> CenteredMessage(
            message = current.message,
            onRetry = viewModel::retry,
            onBack = onBack
        )
        is CompareUiState.Ready -> CompareSimulationScreen(
            first = current.first,
            second = current.second,
            onBack = onBack
        )
    }
}

@Composable
private fun CenteredProgress() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        InlineErrorBanner(message)
        PrimaryActionButton("Retry", onClick = onRetry, modifier = Modifier.padding(top = 12.dp))
        TextButton(onClick = onBack) { Text("Back") }
    }
}
