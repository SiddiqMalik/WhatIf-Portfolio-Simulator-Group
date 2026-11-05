package com.reztek.whatifportfolio.ui.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val chartPoints: List<ChartSeriesPoint>,
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
                ComparisonHighlights.summary(first, second)?.let { summary ->
                    SummaryLine(summaryText(summary, first, second, currency))
                    Spacer(Modifier.height(8.dp))
                }
                MetricComparisonRow(
                    "Total contributed", first, second,
                    firstValue = currency(first.totalContributed).withDerivedMark(first.contributedIsDerived),
                    secondValue = currency(second.totalContributed).withDerivedMark(second.contributedIsDerived),
                    winner = ComparisonHighlights.winnerFor(ComparisonMetric.TOTAL_CONTRIBUTED, first, second),
                    differenceText = differenceText(
                        ComparisonHighlights.differenceFor(ComparisonMetric.TOTAL_CONTRIBUTED, first, second),
                        currency
                    )
                )
                Spacer(Modifier.height(8.dp))
                MetricComparisonRow(
                    "Final value", first, second, currency(first.finalValue), currency(second.finalValue),
                    winner = ComparisonHighlights.winnerFor(ComparisonMetric.FINAL_VALUE, first, second),
                    differenceText = differenceText(
                        ComparisonHighlights.differenceFor(ComparisonMetric.FINAL_VALUE, first, second),
                        currency
                    )
                )
                Spacer(Modifier.height(8.dp))
                MetricComparisonRow(
                    "Real value", first, second,
                    first.realValue?.let(currency) ?: NOT_AVAILABLE,
                    second.realValue?.let(currency) ?: NOT_AVAILABLE,
                    winner = ComparisonHighlights.winnerFor(ComparisonMetric.REAL_VALUE, first, second),
                    differenceText = differenceText(
                        ComparisonHighlights.differenceFor(ComparisonMetric.REAL_VALUE, first, second),
                        currency
                    )
                )
                Spacer(Modifier.height(8.dp))
                MetricComparisonRow(
                    "Return", first, second,
                    first.percentReturn?.let(::formatPercent) ?: NOT_AVAILABLE,
                    second.percentReturn?.let(::formatPercent) ?: NOT_AVAILABLE,
                    winner = ComparisonHighlights.winnerFor(ComparisonMetric.RETURN, first, second),
                    differenceText = differenceText(
                        ComparisonHighlights.differenceFor(ComparisonMetric.RETURN, first, second),
                        { formatPercent(it) },
                        unit = " pts"
                    )
                )
                if (first.contributedIsDerived || second.contributedIsDerived) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$DERIVED_MARK Calculated from the saved inputs (initial investment + monthly contribution " +
                            "x 12 x years). It is not stored with the simulation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Text("Projection timelines", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Cloud simulations use their stored timeline. Local simulations are recalculated from their saved inputs. A chart is omitted when no timeline is available.",
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
private fun SummaryLine(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun MetricComparisonRow(
    metric: String,
    first: ComparisonSimulation,
    second: ComparisonSimulation,
    firstValue: String,
    secondValue: String,
    winner: ComparisonWinner = ComparisonWinner.NONE,
    differenceText: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(metric, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricColumn(first.title, firstValue, winner == ComparisonWinner.FIRST, Modifier.weight(1f))
                MetricColumn(second.title, secondValue, winner == ComparisonWinner.SECOND, Modifier.weight(1f))
            }
            if (differenceText != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Difference (second vs first): $differenceText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    simulationTitle: String,
    value: String,
    isBetter: Boolean,
    modifier: Modifier = Modifier
) {
    val highlight = if (isBetter) {
        Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
    } else {
        Modifier
    }
    Column(modifier.then(highlight).padding(8.dp)) {
        Text(
            simulationTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBetter) FontWeight.Bold else FontWeight.Medium,
            color = if (isBetter) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified
        )
        if (isBetter) {
            Text(
                "Better",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
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

/** Same locale as the Saved Simulations list, so amounts read identically across screens. */
private val AppLocale: Locale = Locale("en", "ZA")

private const val NOT_AVAILABLE = "Not available"
private const val DERIVED_MARK = "*"

private fun String.withDerivedMark(derived: Boolean): String = if (derived) "$this $DERIVED_MARK" else this

private fun formatPercent(value: Double): String = String.format(AppLocale, "%.2f%%", value)

private fun differenceText(
    difference: Double?,
    format: (Double) -> String,
    unit: String = ""
): String? {
    if (difference == null) return null
    if (kotlin.math.abs(difference) < ComparisonHighlights.EQUAL_TOLERANCE) return "None"
    val sign = if (difference > 0) "+" else "-"
    val magnitude = format(kotlin.math.abs(difference)).removeSuffix("%")
    return if (unit.isEmpty()) "$sign$magnitude" else "$sign$magnitude$unit"
}

private fun summaryText(
    summary: ComparisonSummary,
    first: ComparisonSimulation,
    second: ComparisonSimulation,
    currency: (Double) -> String
): String {
    val basis = if (summary.basis == ComparisonMetric.REAL_VALUE) "real value" else "final value"
    return when (summary.winner) {
        ComparisonWinner.TIE -> "Both simulations ended with the same $basis."
        ComparisonWinner.FIRST -> "${first.title} ended ${currency(summary.amount)} higher in $basis than ${second.title}."
        ComparisonWinner.SECOND -> "${second.title} ended ${currency(summary.amount)} higher in $basis than ${first.title}."
        ComparisonWinner.NONE -> ""
    }
}

@Composable
private fun rememberCurrencyFormatter(): (Double) -> String {
    val formatter = NumberFormat.getCurrencyInstance()
    return { amount -> formatter.format(amount) }
}
