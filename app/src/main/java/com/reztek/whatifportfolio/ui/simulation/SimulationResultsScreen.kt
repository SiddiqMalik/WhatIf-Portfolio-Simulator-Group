package com.reztek.whatifportfolio.ui.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reztek.whatifportfolio.data.remote.dto.PerAssetResultDto
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.ui.chart.ChartSeriesPoint
import com.reztek.whatifportfolio.ui.chart.PortfolioCanvasChart
import com.reztek.whatifportfolio.ui.components.FullScreenLoading
import com.reztek.whatifportfolio.ui.components.InlineErrorBanner
import com.reztek.whatifportfolio.ui.components.MetricHero
import com.reztek.whatifportfolio.ui.components.PrimaryActionButton
import com.reztek.whatifportfolio.ui.components.SectionHeader
import com.reztek.whatifportfolio.ui.components.StatusChip
import com.reztek.whatifportfolio.ui.theme.BackgroundLight
import com.reztek.whatifportfolio.ui.theme.ErrorRed
import com.reztek.whatifportfolio.ui.theme.NavyDeep
import com.reztek.whatifportfolio.ui.theme.SuccessGreen
import com.reztek.whatifportfolio.ui.theme.TealPrimary
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationResultsScreen(
    simulationId: String,
    onBack: () -> Unit,
    viewModel: SimulationResultsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(simulationId) { viewModel.load(simulationId) }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = BackgroundLight
    ) { padding ->
        when (val state = uiState) {
            is SimulationDetailUiState.Loading -> FullScreenLoading(Modifier.padding(padding))
            is SimulationDetailUiState.Error -> {
                Column(Modifier.padding(padding).padding(20.dp)) {
                    InlineErrorBanner(state.message)
                    Spacer(Modifier.height(16.dp))
                    PrimaryActionButton("Try again", onClick = { viewModel.load(simulationId) })
                }
            }
            is SimulationDetailUiState.Ready -> {
                ResultsContent(
                    simulation = state.simulation,
                    window = state.window,
                    chartPoints = viewModel.chartPoints(state.simulation, state.window),
                    showSave = state.simulation.status != "saved",
                    saving = saving,
                    onWindow = viewModel::setWindow,
                    onSave = viewModel::saveSimulation,
                    contentPadding = padding,
                    animateChart = true
                )
            }
        }
    }
}

@Composable
fun ResultsContent(
    simulation: SimulationDto,
    window: ChartWindow,
    chartPoints: List<ChartSeriesPoint>,
    showSave: Boolean,
    saving: Boolean,
    onWindow: (ChartWindow) -> Unit,
    onSave: () -> Unit,
    contentPadding: PaddingValues,
    animateChart: Boolean = true,
    footer: (@Composable () -> Unit)? = null
) {
    val currency = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            simulation.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "${simulation.startDate} → ${simulation.endDate} · ${simulation.frequency}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricHero(
                label = "Final value",
                value = currency.format(simulation.finalValue),
                supporting = "${"%.1f".format(simulation.percentReturn)}% nominal",
                valueColor = TealPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricHero(
                label = "Real value",
                value = currency.format(simulation.realValue ?: 0.0),
                supporting = "${"%.1f".format(simulation.realReturnPct ?: 0.0)}% real",
                valueColor = NavyDeep,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(
                text = if (simulation.profitLoss >= 0) "+" + currency.format(simulation.profitLoss)
                else currency.format(simulation.profitLoss),
                color = if (simulation.profitLoss >= 0) SuccessGreen else ErrorRed
            )
            StatusChip(
                text = "Contributed ${currency.format(simulation.totalContributed)}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            SectionHeader(title = "Performance", subtitle = "Nominal vs CPI-adjusted value")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ChartWindow.ONE_YEAR to "1Y",
                    ChartWindow.FIVE_YEARS to "5Y",
                    ChartWindow.MAX to "Max"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = window == value,
                        onClick = { onWindow(value) },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            PortfolioCanvasChart(
                dataPoints = chartPoints,
                animate = animateChart,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionHeader(title = "Asset breakdown", subtitle = "Contribution by holding")

        simulation.perAsset.orEmpty().forEach { asset ->
            AssetBreakdownRow(asset, currency)
        }

        if (showSave) {
            PrimaryActionButton(
                text = "Save simulation",
                isLoading = saving,
                onClick = onSave
            )
        }

        footer?.invoke()
    }
}

@Composable
private fun AssetBreakdownRow(
    asset: PerAssetResultDto,
    currency: NumberFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(asset.symbol.uppercase(), fontWeight = FontWeight.SemiBold)
            Text(
                "${asset.type} · ${"%.1f".format(asset.percent)}% target",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(currency.format(asset.contributed), fontWeight = FontWeight.Medium)
            Text(
                "${"%.1f".format(asset.units)} units",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}
