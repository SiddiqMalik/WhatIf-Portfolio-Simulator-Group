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
import java.util.Locale
import com.reztek.whatifportfolio.data.model.SimulationPoint
import kotlinx.coroutines.launch

@Composable
fun SimulationBuilderScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember {
        SimulationRepository(SimulationDatabase.getDatabase(context).simulationDao())
    }

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

    val simulationResult: SimulationResult = remember(
        initialInvestment,
        monthlyContribution,
        returnRate,
        inflationRate,
        years
    ) {
        SimulationEngine.calculateSimulation(
            initialInvestment = initialInvestment,
            monthlyContribution = monthlyContribution,
            annualReturnRatePercent = returnRate,
            annualInflationRatePercent = inflationRate,
            timeHorizonYears = years
        )
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
                                        finalNominalValue = simulationResult.finalNominalValue,
                                        finalRealValue = simulationResult.finalRealValue
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
                        text = String.format(Locale.getDefault(), "$%.2f", simulationResult.finalNominalValue),
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
                        text = String.format(Locale.getDefault(), "$%.2f", simulationResult.finalRealValue),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        PortfolioCanvasChart(
            dataPoints = simulationResult.yearlyDataPoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        OutlinedTextField(
            value = initialInvestmentText,
            onValueChange = { initialInvestmentText = it },
            label = { Text("Initial Investment ($)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = monthlyContributionText,
            onValueChange = { monthlyContributionText = it },
            label = { Text("Monthly Contribution ($)") },
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