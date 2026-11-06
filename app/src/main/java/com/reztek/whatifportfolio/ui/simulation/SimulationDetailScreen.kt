package com.reztek.whatifportfolio.ui.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reztek.whatifportfolio.ui.chart.ChartSeriesPoint
import com.reztek.whatifportfolio.ui.components.FullScreenLoading
import com.reztek.whatifportfolio.ui.components.InlineErrorBanner
import com.reztek.whatifportfolio.ui.components.PrimaryActionButton
import com.reztek.whatifportfolio.ui.theme.BackgroundLight
import com.reztek.whatifportfolio.ui.theme.ErrorRed
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.reztek.whatifportfolio.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationDetailScreen(
    simulationId: String,
    onBack: () -> Unit,
    onRerun: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: SimulationDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(simulationId) { viewModel.load(simulationId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        when (val state = uiState) {
            is SimulationDetailUiState.Loading -> FullScreenLoading(Modifier.padding(padding))
            is SimulationDetailUiState.Error -> {
                Column(Modifier.padding(padding).padding(20.dp)) {
                    InlineErrorBanner(state.message)
                    Spacer(Modifier.height(12.dp))
                    PrimaryActionButton(stringResource(R.string.action_retry), onClick = { viewModel.refresh(simulationId) })
                }
            }
            is SimulationDetailUiState.Ready -> {
                val sim = state.simulation
                val points = sim.timeSeries.orEmpty().map {
                    ChartSeriesPoint(
                        date = it.date,
                        nominalValue = it.nominalValue,
                        realValue = it.realValue,
                        contributed = it.contributed
                    )
                }
                ResultsContent(
                    simulation = sim,
                    window = ChartWindow.MAX,
                    chartPoints = points,
                    showSave = false,
                    saving = false,
                    onWindow = {},
                    onSave = {},
                    contentPadding = padding,
                    animateChart = false,
                    offlineCopy = state.fromCache,
                    footer = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            actionError?.let { InlineErrorBanner(it) }
                            Text(
                                stringResource(
                                    R.string.detail_parameters,
                                    sim.initialInvestment.toString(),
                                    sim.recurringContribution.toString(),
                                    sim.frequency
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            PrimaryActionButton(
                                text = stringResource(R.string.detail_rerun),
                                isLoading = busy,
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        actionError = null
                                        try {
                                            onRerun(viewModel.rerun(simulationId))
                                        } catch (e: Exception) {
                                            actionError = com.reztek.whatifportfolio.ui.i18n.localize(
                                                context,
                                                e.localizedMessage ?: "Rerun failed"
                                            )
                                        } finally {
                                            busy = false
                                        }
                                    }
                                }
                            )
                            OutlinedButton(
                                onClick = { onDuplicate(simulationId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.action_duplicate))
                            }
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.action_delete), color = ErrorRed)
                            }
                        }
                    }
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_simulation_title)) },
            text = { Text(stringResource(R.string.delete_simulation_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            try {
                                viewModel.delete(simulationId)
                                onDeleted()
                            } catch (e: Exception) {
                                actionError = com.reztek.whatifportfolio.ui.i18n.localize(
                                    context,
                                    e.localizedMessage ?: "Delete failed"
                                )
                            }
                        }
                    }
                ) { Text(stringResource(R.string.action_delete), color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
