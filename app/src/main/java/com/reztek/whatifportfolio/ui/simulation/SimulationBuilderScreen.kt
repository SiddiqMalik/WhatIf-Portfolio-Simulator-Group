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
import com.reztek.whatifportfolio.ui.chart.ChartSeriesPoint
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
    onLocalSaved: () -> Unit = {},
    draftId: String? = null,
    viewModel: SimulationBuilderViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val localRepository = remember {
        SimulationRepository(SimulationDatabase.getDatabase(context).simulationDao())
    }

    val state by viewModel.uiState.collectAsState()
    var showAssetSheet by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf(Currency.USD) }
    var selectedMode by remember { mutableStateOf(SimulationMode.HISTORICAL_BACKTEST) }
    var returnRateText by remember { mutableStateOf("7.0") }
    var inflationRateText by remember { mutableStateOf("2.5") }
    var yearsSliderPosition by remember { mutableFloatStateOf(10f) }
    var isSavingLocal by remember { mutableStateOf(false) }

    val initialInvestment = state.initialInvestment.toDoubleOrNull() ?: 0.0
    val monthlyContribution = state.recurringContribution.toDoubleOrNull() ?: 0.0
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
            annualReturnRatePercent = if (selectedMode == SimulationMode.HISTORICAL_BACKTEST) {
                returnRate * 0.9
            } else {
                returnRate
            },
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

    LaunchedEffect(draftId) {
        if (!draftId.isNullOrBlank()) {
            runCatching {
                val repo = com.reztek.whatifportfolio.data.repository.RemoteSimulationRepository()
                val sim = repo.getSimulation(draftId)
                viewModel.prefillFromDraft(
                    name = "${sim.name} (copy)",
                    initialInvestment = sim.initialInvestment,
                    recurringContribution = sim.recurringContribution,
                    frequency = sim.frequency,
                    startDate = sim.startDate,
                    endDate = sim.endDate,
                    allocations = sim.allocations.orEmpty()
                )
                selectedMode = SimulationMode.HISTORICAL_BACKTEST
            }
        }
    }

    fun saveLocalProjection(andGoSaved: Boolean) {
        if (!state.isProjectionValid) {
            Toast.makeText(
                context,
                "Enter a scenario name and a positive initial investment.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        coroutineScope.launch {
            isSavingLocal = true
            try {
                localRepository.saveSimulation(
                    SavedSimulationEntity(
                        title = state.name.trim(),
                        initialInvestment = initialInvestment,
                        monthlyContribution = monthlyContribution,
                        returnRate = returnRate,
                        inflationRate = inflationRate,
                        years = years,
                        finalNominalValue = convertedNominal,
                        finalRealValue = convertedReal
                    )
                )
                Toast.makeText(context, "Projection saved on this device", Toast.LENGTH_SHORT).show()
                if (andGoSaved) onLocalSaved()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    e.localizedMessage ?: "Couldn't save projection",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isSavingLocal = false
            }
        }
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
                            onClick = {
                                when (selectedMode) {
                                    SimulationMode.FUTURE_PROJECTION -> saveLocalProjection(andGoSaved = false)
                                    SimulationMode.HISTORICAL_BACKTEST ->
                                        viewModel.runSimulation(alsoSave = true)
                                }
                            },
                            enabled = !state.isRunning && !isSavingLocal,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save")
                        }

                        PrimaryActionButton(
                            text = if (selectedMode == SimulationMode.FUTURE_PROJECTION) {
                                "Save & view"
                            } else {
                                "Run simulation"
                            },
                            enabled = !isSavingLocal,
                            isLoading = state.isRunning || isSavingLocal,
                            onClick = {
                                when (selectedMode) {
                                    SimulationMode.FUTURE_PROJECTION ->
                                        saveLocalProjection(andGoSaved = true)
                                    SimulationMode.HISTORICAL_BACKTEST ->
                                        viewModel.runSimulation(alsoSave = false)
                                }
                            },
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
                subtitle = when (selectedMode) {
                    SimulationMode.HISTORICAL_BACKTEST ->
                        "Backtest real market history with multi-asset DCA."
                    SimulationMode.FUTURE_PROJECTION ->
                        "Project forward with assumed return and inflation rates."
                }
            )

            Column {
                Text(text = "Simulation Mode", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedMode == SimulationMode.HISTORICAL_BACKTEST,
                        onClick = { selectedMode = SimulationMode.HISTORICAL_BACKTEST },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Historical")
                    }
                    SegmentedButton(
                        selected = selectedMode == SimulationMode.FUTURE_PROJECTION,
                        onClick = { selectedMode = SimulationMode.FUTURE_PROJECTION },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Future")
                    }
                }
            }

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

            Column {
                Text(text = "Display Currency", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Currency.entries.forEachIndexed { index, currency ->
                        SegmentedButton(
                            selected = selectedCurrency == currency,
                            onClick = { selectedCurrency = currency },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = Currency.entries.size
                            )
                        ) {
                            Text(currency.name)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.initialInvestment,
                onValueChange = viewModel::onInitialInvestmentChanged,
                label = { Text("Initial Investment (${selectedCurrency.symbol})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.fieldErrors.containsKey("initial"),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = state.recurringContribution,
                onValueChange = viewModel::onRecurringContributionChanged,
                label = {
                    Text(
                        if (selectedMode == SimulationMode.FUTURE_PROJECTION) {
                            "Monthly Contribution (${selectedCurrency.symbol})"
                        } else {
                            "Recurring Contribution (${selectedCurrency.symbol})"
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.fieldErrors.containsKey("contribution"),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            if (selectedMode == SimulationMode.FUTURE_PROJECTION) {
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
                        label = { Text("Inflation (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Column {
                    Text(text = "Time Horizon: $years Years", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = yearsSliderPosition,
                        onValueChange = { yearsSliderPosition = it },
                        valueRange = 1f..30f,
                        steps = 29
                    )
                }

                Text(
                    text = String.format(
                        Locale.getDefault(),
                        "Projected nominal: %s%.2f  ·  Real: %s%.2f",
                        selectedCurrency.symbol,
                        convertedNominal,
                        selectedCurrency.symbol,
                        convertedReal
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                PortfolioCanvasChart(
                    dataPoints = baseResult.yearlyDataPoints.map { point ->
                        ChartSeriesPoint(
                            date = point.year.toString(),
                            nominalValue = point.nominalValue,
                            realValue = point.realValue,
                            contributed = point.totalContributions
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            } else {
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
                            modifier = Modifier.width(88.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        IconButton(onClick = { viewModel.removeAllocation(row.symbol) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                }

                if (state.allocations.isEmpty()) {
                    Text(
                        "Add equities or crypto and set percentages to 100% before running.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
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
