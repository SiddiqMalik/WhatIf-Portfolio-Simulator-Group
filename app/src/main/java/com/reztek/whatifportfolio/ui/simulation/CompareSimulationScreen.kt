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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reztek.whatifportfolio.ui.chart.ChartSeriesPoint
import com.reztek.whatifportfolio.ui.chart.PortfolioCanvasChart
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.reztek.whatifportfolio.R

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
    val chartPoints: List<ChartSeriesPoint>,
    /** True when [totalContributed] is calculated from saved inputs rather than stored. */
    val contributedIsDerived: Boolean = false
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
                        stringResource(R.string.compare_screen_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                }
                Text(
                    stringResource(R.string.compare_screen_hint),
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
                Text(stringResource(R.string.compare_key_metrics), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                MetricComparisonRow(
                    stringResource(R.string.compare_total_contributed), first, second,
                    firstValue = currency(first.totalContributed).withDerivedMark(first.contributedIsDerived),
                    secondValue = currency(second.totalContributed).withDerivedMark(second.contributedIsDerived)
                )
                Spacer(Modifier.height(8.dp))
                MetricComparisonRow(stringResource(R.string.compare_final_value), first, second, currency(first.finalValue), currency(second.finalValue))
                Spacer(Modifier.height(8.dp))
                MetricComparisonRow(
                    stringResource(R.string.compare_real_value), first, second,
                    first.realValue?.let(currency) ?: stringResource(R.string.not_available),
                    second.realValue?.let(currency) ?: stringResource(R.string.not_available)
                )
                Spacer(Modifier.height(8.dp))
                MetricComparisonRow(
                    stringResource(R.string.compare_return), first, second,
                    first.percentReturn?.let(::formatPercent) ?: stringResource(R.string.not_available),
                    second.percentReturn?.let(::formatPercent) ?: stringResource(R.string.not_available)
                )
                if (first.contributedIsDerived || second.contributedIsDerived) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$DERIVED_MARK ${stringResource(R.string.compare_derived_note)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Text(stringResource(R.string.compare_timelines), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.compare_timelines_hint),
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
            Text(stringResource(R.string.compare_final_value), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MetricComparisonRow(
    metric: String,
    first: ComparisonSimulation,
    second: ComparisonSimulation,
    firstValue: String,
    secondValue: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            // The metric name appears once; each column is labelled with its own simulation.
            Text(metric, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricColumn(first.title, firstValue, Modifier.weight(1f))
                MetricColumn(second.title, secondValue, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricColumn(simulationTitle: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            simulationTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
                    stringResource(R.string.compare_no_timeline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(R.string.compare_final_short, currency(simulation.finalValue)),
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

/** Same locale as the Saved Simulations list, so amounts read identically across screens. */
private val AppLocale: Locale = Locale("en", "ZA")

private const val DERIVED_MARK = "*"

private fun String.withDerivedMark(derived: Boolean): String = if (derived) "$this $DERIVED_MARK" else this

private fun formatPercent(value: Double): String = String.format(AppLocale, "%.2f%%", value)

@Composable
private fun rememberCurrencyFormatter(): (Double) -> String {
    val formatter = remember { NumberFormat.getCurrencyInstance(AppLocale) }
    return { amount -> formatter.format(amount) }
}
