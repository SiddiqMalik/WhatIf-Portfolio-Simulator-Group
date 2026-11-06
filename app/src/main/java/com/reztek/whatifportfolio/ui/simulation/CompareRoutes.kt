package com.reztek.whatifportfolio.ui.simulation

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reztek.whatifportfolio.ui.components.InlineErrorBanner
import com.reztek.whatifportfolio.ui.components.PrimaryActionButton
import com.reztek.whatifportfolio.ui.components.SkeletonRow
import androidx.compose.ui.res.stringResource
import com.reztek.whatifportfolio.R

/**
 * Navigation-facing wrappers for the Compare feature. They own the state
 * handling (loading / error / retry / ready) so the existing
 * CompareSelectionScreen and CompareSimulationScreen stay purely presentational.
 */

/** Loads the saved list (local + cloud) and lets the user pick exactly two. */
@Composable
fun CompareSelectionRoute(
    onCompareSelected: (firstId: String, secondId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: SavedSimulationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is SavedListUiState.Ready -> CompareSelectionScreen(
            savedItems = state.items,
            onCompareSelected = onCompareSelected,
            onBack = onBack
        )
        is SavedListUiState.Loading -> CompareStatusScaffold(title = stringResource(R.string.compare_title), onBack = onBack) {
            repeat(3) { SkeletonRow(Modifier.padding(bottom = 10.dp)) }
        }
        is SavedListUiState.Error -> CompareStatusScaffold(title = stringResource(R.string.compare_title), onBack = onBack) {
            InlineErrorBanner(state.message)
            Spacer(Modifier.height(12.dp))
            PrimaryActionButton(stringResource(R.string.action_retry), onClick = viewModel::refresh)
        }
    }
}

/** Loads the two chosen simulations and shows the comparison. */
@Composable
fun CompareRoute(
    firstId: String,
    secondId: String,
    onBack: () -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: CompareViewModel = viewModel(
        key = "compare-$firstId-$secondId",
        factory = CompareViewModel.factory(application, firstId, secondId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is CompareUiState.Ready -> CompareSimulationScreen(
            first = state.first,
            second = state.second,
            onBack = onBack
        )
        is CompareUiState.Loading -> CompareStatusScaffold(title = stringResource(R.string.compare_screen_title), onBack = onBack) {
            repeat(3) { SkeletonRow(Modifier.padding(bottom = 10.dp)) }
        }
        is CompareUiState.Error -> CompareStatusScaffold(title = stringResource(R.string.compare_screen_title), onBack = onBack) {
            InlineErrorBanner(state.message)
            if (state.canRetry) {
                Spacer(Modifier.height(12.dp))
                PrimaryActionButton(stringResource(R.string.action_retry), onClick = viewModel::retry)
            }
        }
    }
}

/** Title + Back header shared by the loading and error states of both Compare routes. */
@Composable
private fun CompareStatusScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
