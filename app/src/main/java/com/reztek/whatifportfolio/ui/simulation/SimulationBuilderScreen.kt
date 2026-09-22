package com.reztek.whatifportfolio.ui.simulation

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reztek.whatifportfolio.data.engine.SimulationEngine
import com.reztek.whatifportfolio.data.engine.SimulationResult
import com.reztek.whatifportfolio.data.local.SavedSimulationEntity
import com.reztek.whatifportfolio.data.local.SimulationDatabase
import com.reztek.whatifportfolio.data.local.SimulationRepository
import com.reztek.whatifportfolio.ui.chart.PortfolioCanvasChart
import com.reztek.whatifportfolio.ui.components.InlineErrorBanner
import com.reztek.whatifportfolio.ui.components.PrimaryActionButton
import com.reztek.whatifportfolio.ui.components.SectionHeader
import com.reztek.whatifportfolio.ui.theme.BackgroundLight
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

enum class Currency(val symbol: String, val rateToUSD: Double) {
    USD("$", 1.0),
    ZAR("R", 18.25),
    EUR("€", 0.92),
    GBP("£", 0.78)
}

enum class SimulationMode {
    FUTURE_PROJECTION,
    HISTORICAL_BACKTEST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationBuilderScreen(
    onBack: () -> Unit = {},
    onSimulationCreated: (String) -> Unit = {},
    draftId: String? = null,
    viewModel: SimulationBuilderViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember {
        SimulationRepository(SimulationDatabase.getDatabase(context).simulationDao())
    }

    val state by viewModel.uiState.collectAsState()
    var showAssetSheet by remember { mutableStateOf(false) }

    var selectedCurrency by remember { mutableStateOf(Currency.USD) }
    var selectedMode by remember { mutableStateOf(SimulationMode.FUTURE_PROJECTION) }

    var initialInvestmentText by remember { mutableStateOf("10000") }
    var monthlyContributionText by remember { mutableStateOf("500") }
    var returnRateText by remember { mutableStateOf("7.0") }
    var inflationRateText by remember { mutableStateOf("2.5") }
    var yearsSliderPosition by remember { mutableFloatStateOf(10f) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var simulationTitle by remember { mutableStateOf("") }

    val initialInvestment = initialInvestmentText.toDoubleOrNull() ?: 0.0
    val monthlyContribution = monthlyContributionText.toDoubleOrNull() ?: 0.0
    val returnRate = returnRateText.toDoubleOrNull() ?: 0.0
    val inflationRate = inflationRateText.toDoubleOrNull() ?: 0.0
    val years = yearsSliderPosition.toInt()

    val baseResult: SimulationResult = remember(
        initialInvestment,
        monthlyContribution,
        returnRate,
        inflationRate,
        years,
        selectedMode
    ) {
        SimulationEngine.calculateSimulation(
            initialInvestment = initialInvestment,
            monthlyContribution = monthlyContribution,
            annualReturnRatePercent = if (selectedMode == SimulationMode.HISTORICAL_BACKTEST) returnRate * 0.9 else returnRate,
            annualInflationRatePercent = inflationRate,
            timeHorizonYears = years
        )
    }

    val convertedNominal = baseResult.finalNominalValue * selectedCurrency.rateToUSD
    val convertedReal = baseResult.finalRealValue * selectedCurrency.rateToUSD

    LaunchedEffect(state.createdSimulationId) {
        state.createdSimulationId?.let { id ->
            viewModel.consumeCreatedId()
            onSimulationCreated(id)
        }
    }

    // Prefill from remote draft when duplicating/rerunning via draftId
    LaunchedEffect(draftId) {
        if (!draftId.isNullOrBlank()) {
            coroutineScope.launch {
                try {
                    val repo = com.reztek.whatifportfolio.data.repository.RemoteSimulationRepository()
                    val sim = repo.getSimulation(draftId)
                    viewModel.prefillFromDraft(
                        name = "${sim.name} (copy)",
                        initialInvestment = sim.initialInvestment,
                        recurringContribution = sim.recurringContribution,
                        frequency = sim.frequency,
                        startDate = sim.startDate,
                        endDate = sim.endDate,
                        allocations = emptyList()
                    )
                } catch (_: Exception) {
                    // Keep blank draft if fetch fails
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Simulation") },
            text = {
                OutlinedTextField(
                    value = simulationTitle,
                    onValueChange = { simulationTitle = it },
                    label = { Text("Scenario Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (simulationTitle.isNotBlank()) {
                            coroutineScope.launch {
                                repository.saveSimulation(
                                    SavedSimulationEntity(
                                        title = simulationTitle.trim(),
                                        initialInvestment = initialInvestment,
                                        monthlyContribution = monthlyContribution,
                                        returnRate = returnRate,
                                        inflationRate = inflationRate,
                                        years = years,
                                        finalNominalValue = convertedNominal,
                                        finalRealValue = convertedReal
                                    )
                                )
                                Toast.makeText(context, "Simulation Saved!", Toast.LENGTH_SHORT).show()
                                showSaveDialog = false
                                simulationTitle = ""
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simulation Builder", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save")
                        }

                        PrimaryActionButton(
                            text = "Run simulation",
                            enabled = state.isValid,
                            isLoading = state.isRunning,
                            onClick = viewModel::runSimulation,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(
                title = "Scenario setup",
                subtitle = "Define parameters, dates, and multi-asset allocations."
            )

            // Display Currency Selector
            Column {
                Text(text = "Display Currency", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Currency.entries.forEachIndexed { index, currency ->
                        SegmentedButton(
                            selected = selectedCurrency == currency,
                            onClick = { selectedCurrency = currency },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = Currency.entries.size)
                        ) {
                            Text("${currency.name} (${currency.symbol})")
                        }
                    }
                }
            }

            // Engine Mode Selector
            Column {
                Text(text = "Simulation Mode", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedMode == SimulationMode.FUTURE_PROJECTION,
                        onClick = { selectedMode = SimulationMode.FUTURE_PROJECTION },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Future Projection")
                    }
                    SegmentedButton(
                        selected = selectedMode == SimulationMode.HISTORICAL_BACKTEST,
                        onClick = { selectedMode = SimulationMode.HISTORICAL_BACKTEST },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Historical Backtest")
                    }
                }
            }

            // Target Values Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Nominal Target", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = String.format(Locale.getDefault(), "%s%.2f", selectedCurrency.symbol, convertedNominal),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Real Value (Inflation Adj.)", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = String.format(Locale.getDefault(), "%s%.2f", selectedCurrency.symbol, convertedReal),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Visual Chart
            PortfolioCanvasChart(
                dataPoints = baseResult.yearlyDataPoints,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            // Simulation Name Input
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Simulation name") },
                singleLine = true,
                isError = state.fieldErrors.containsKey("name"),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            state.fieldErrors["name"]?.let {
                InlineErrorBanner(it, Modifier.padding(top = 4.dp))
            }

            // Investment Inputs
            OutlinedTextField(
                value = initialInvestmentText,
                onValueChange = {
                    initialInvestmentText = it
                    viewModel.onInitialInvestmentChanged(it)
                },
                label = { Text("Initial Investment (${selectedCurrency.symbol})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.fieldErrors.containsKey("initial"),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = monthlyContributionText,
                onValueChange = {
                    monthlyContributionText = it
                    viewModel.onRecurringContributionChanged(it)
                },
                label = { Text("Monthly Contribution (${selectedCurrency.symbol})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.fieldErrors.containsKey("contribution"),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = returnRateText,
                    onValueChange = { returnRateText = it },
                    label = { Text("Return Rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = inflationRateText,
                    onValueChange = { inflationRateText = it },
                    label = { Text("Inflation Rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Time Horizon Slider
            Column {
                Text(text = "Time Horizon: $years Years", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = yearsSliderPosition,
                    onValueChange = { yearsSliderPosition = it },
                    valueRange = 1f..30f,
                    steps = 29
                )
            }

            // Contribution Frequency
            Column {
                Text("Contribution frequency", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                val frequencies = listOf("monthly", "quarterly", "annual")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    frequencies.forEachIndexed { index, freq ->
                        SegmentedButton(
                            selected = state.frequency == freq,
                            onClick = { viewModel.onFrequencyChanged(freq) },
                            shape = SegmentedButtonDefaults.itemShape(index, frequencies.size)
                        ) {
                            Text(freq.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }

            // Dates Selection
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(
                    label = "Start date",
                    value = state.startDate,
                    modifier = Modifier.weight(1f),
                    onPick = { year, month, day ->
                        viewModel.onStartDateChanged(
                            LocalDate.of(year, month + 1, day).toString()
                        )
                    }
                )
                DateField(
                    label = "End date",
                    value = state.endDate,
                    modifier = Modifier.weight(1f),
                    onPick = { year, month, day ->
                        viewModel.onEndDateChanged(
                            LocalDate.of(year, month + 1, day).toString()
                        )
                    }
                )
            }
            state.fieldErrors["dates"]?.let {
                InlineErrorBanner(it, Modifier.padding(top = 4.dp))
            }

            // Multi-Asset Allocations Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Allocations",
                    subtitle = "Total ${"%.1f".format(state.allocationTotal)}%"
                )
                OutlinedButton(onClick = { showAssetSheet = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Add asset")
                }
            }

            state.allocationError?.let {
                InlineErrorBanner(it, Modifier.padding(bottom = 8.dp))
            }

            state.allocations.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(row.symbol.uppercase(), fontWeight = FontWeight.SemiBold)
                        Text(
                            row.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                    OutlinedTextField(
                        value = row.percent,
                        onValueChange = { viewModel.onAllocationPercentChanged(row.symbol, it) },
                        label = { Text("%") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(0.28f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(onClick = { viewModel.removeAllocation(row.symbol) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove")
                    }
                }
            }

            if (state.allocations.isEmpty()) {
                Text(
                    "No assets yet. Add equities or crypto to allocate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            state.runError?.let {
                InlineErrorBanner(it, Modifier.padding(bottom = 12.dp))
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAssetSheet) {
        AssetSelectorSheet(
            viewModel = viewModel,
            onDismiss = { showAssetSheet = false },
            onConfirm = { assets -> viewModel.addAssets(assets) }
        )
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onPick: (year: Int, month: Int, day: Int) -> Unit
) {
    val context = LocalContext.current
    val date = runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.now() }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> onPick(y, m, d) },
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth
                    ).show()
                }
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date")
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(14.dp)
    )
}