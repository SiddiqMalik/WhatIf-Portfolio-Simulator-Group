package com.reztek.whatifportfolio.ui.simulation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reztek.whatifportfolio.data.local.SavedSimulationEntity
import com.reztek.whatifportfolio.data.local.SimulationDatabase
import com.reztek.whatifportfolio.data.local.SimulationRepository
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSimulationsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember {
        SimulationRepository(SimulationDatabase.getDatabase(context).simulationDao())
    }

    val simulations by repository.allSimulations.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Saved Simulations",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (simulations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved simulations yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = simulations,
                    key = { it.id }
                ) { simulation ->
                    SavedSimulationCard(
                        simulation = simulation,
                        onDelete = {
                            coroutineScope.launch {
                                repository.deleteSimulation(simulation)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedSimulationCard(
    simulation: SavedSimulationEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = simulation.title,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Simulation",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Nominal: ${String.format(Locale.getDefault(), "$%.2f", simulation.finalNominalValue)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Real: ${String.format(Locale.getDefault(), "$%.2f", simulation.finalRealValue)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Initial: $${simulation.initialInvestment} | Monthly: $${simulation.monthlyContribution} | ${simulation.years} Yrs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}