package com.reztek.whatifportfolio.ui.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reztek.whatifportfolio.ui.chart.ChartSeriesPoint
import com.reztek.whatifportfolio.ui.chart.PortfolioCanvasChart
import java.text.NumberFormat
import java.util.Locale

/**
 * UI-ready data for comparing two saved simulations.
 * Values and points must be supplied from the actual saved simulation records.
 * Missing chart points remain missing; this screen does not synthesize a timeline.
 */
data class ComparisonSimulation(
    val id: String,
    val title: String,
    val dateRange: String,
    val totalContributed: Double,
    val finalValue: Double,
    val realValue: Double?,
    val percentReturn: Double?,
    val chartPoints: List<ChartSeriesPoint>
)

@Composable
fun CompareSimulationScreen(
    first: ComparisonSimulation,
    second: ComparisonSimulation,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = rememberCurrencyFormatter()
    Scaffold(modifier = modifier) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Simulation comparison",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onBack) { Text("Back") }
                }
                Text(
                    "Compare the saved outcomes and available projection timelines.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SimulationSummaryCard(first, currency, Modifier.weight(1f))
                    SimulationSummaryCard(second, currency, Modifier.weight(1f))
                }
            }
            item {
                Text("Key metrics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                MetricComparisonRow("Total contributed", currency(first.totalContributed), currency(second.totalContributed))
                MetricComparisonRow("Final value", currency(first.finalValue), currency(second.finalValue))
                MetricComparisonRow("Real value", first.realValue?.let(currency) ?: "Not available", second.realValue?.let(currency) ?: "Not available")
                MetricComparisonRow("Return", first.percentReturn?.let { String.format(Locale.getDefault(), "%.2f%%", it) } ?: "Not available", second.percentReturn?.let { String.format(Locale.getDefault(), "%.2f%%", it) } ?: "Not available")
            }
            item {
                Text("Projection timelines", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Each chart uses the timeline stored with that simulation. A chart is omitted when no timeline is available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ChartSection(first, currency)
            }
            item {
                ChartSection(second, currency)
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun SimulationSummaryCard(
    simulation: ComparisonSimulation,
    currency: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(simulation.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(simulation.dateRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(currency(simulation.finalValue), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("Final value", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MetricComparisonRow(label: String, firstValue: String, secondValue: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(firstValue, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            Column(Modifier.weight(1f)) {
                Text("Simulation 2", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(secondValue, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ChartSection(
    simulation: ComparisonSimulation,
    currency: (Double) -> String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(simulation.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (simulation.chartPoints.isEmpty()) {
                Text(
                    "No saved timeline is available for this simulation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Final: ${currency(simulation.finalValue)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PortfolioCanvasChart(
                    dataPoints = simulation.chartPoints,
                    modifier = Modifier.fillMaxWidth(),
                    showContributed = true,
                    animate = false
                )
            }
        }
    }
}

@Composable
private fun rememberCurrencyFormatter(): (Double) -> String {
    val formatter = NumberFormat.getCurrencyInstance()
    return { amount -> formatter.format(amount) }
}
