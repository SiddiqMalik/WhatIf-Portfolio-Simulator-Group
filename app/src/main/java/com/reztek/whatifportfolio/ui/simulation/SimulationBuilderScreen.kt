package com.reztek.whatifportfolio.ui.simulation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reztek.whatifportfolio.data.engine.SimulationEngine
import com.reztek.whatifportfolio.data.engine.SimulationResult
import com.reztek.whatifportfolio.data.local.SavedSimulationEntity
import com.reztek.whatifportfolio.data.local.SimulationDatabase
import com.reztek.whatifportfolio.data.local.SimulationRepository
import com.reztek.whatifportfolio.ui.chart.PortfolioCanvasChart
import kotlinx.coroutines.launch
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember {
        SimulationRepository(SimulationDatabase.getDatabase(context).simulationDao())
    }

    var selectedCurrency by remember { mutableStateOf(Currency.USD) }
    var selectedMode by remember { mutableStateOf(SimulationMode.FUTURE_PROJECTION) }

    var initialInvestmentText by remember { mutableStateOf("10000") }
    var monthlyContributionText by remember { mutableStateOf("500") }
    var returnRateText by remember { mutableStateOf("7.0") }
    var inflationRateText by remember { mutableStateOf("2.5") }
    var yearsSliderPosition by remember { androidx.compose.runtime.mutableFloatStateOf(10f) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Simulation Builder",
            style = MaterialTheme.typography.headlineMedium
        )

        // Currency Selector Row
        Column {
            Text(text = "Display Currency", style = MaterialTheme.typography.labelMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Currency.values().forEachIndexed { index, currency ->
                    SegmentedButton(
                        selected = selectedCurrency == currency,
                        onClick = { selectedCurrency = currency },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Currency.values().size)
                    ) {
                        Text("${currency.name} (${currency.symbol})")
                    }
                }
            }
        }

        // Engine Mode Selector
        Column {
            Text(text = "Simulation Mode", style = MaterialTheme.typography.labelMedium)
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

        PortfolioCanvasChart(
            dataPoints = baseResult.yearlyDataPoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        OutlinedTextField(
            value = initialInvestmentText,
            onValueChange = { initialInvestmentText = it },
            label = { Text("Initial Investment (${selectedCurrency.symbol})") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = monthlyContributionText,
            onValueChange = { monthlyContributionText = it },
            label = { Text("Monthly Contribution (${selectedCurrency.symbol})") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = returnRateText,
                onValueChange = { returnRateText = it },
                label = { Text("Return Rate (%)") },
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = inflationRateText,
                onValueChange = { inflationRateText = it },
                label = { Text("Inflation Rate (%)") },
                modifier = Modifier.weight(1f)
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

        Button(
            onClick = { showSaveDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Simulation")
        }
    }
}