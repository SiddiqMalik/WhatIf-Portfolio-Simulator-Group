package com.reztek.whatifportfolio.ui.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.reztek.whatifportfolio.ui.theme.NavyDeep
import com.reztek.whatifportfolio.ui.theme.TealPrimary

data class ChartSeriesPoint(
    val date: String,
    val nominalValue: Double,
    val realValue: Double?,
    val contributed: Double = 0.0
)

@Composable
fun PortfolioCanvasChart(
    dataPoints: List<ChartSeriesPoint>,
    modifier: Modifier = Modifier,
    nominalColor: Color = TealPrimary,
    realColor: Color = NavyDeep,
    contributedColor: Color = MaterialTheme.colorScheme.outline,
    showContributed: Boolean = true,
    animate: Boolean = true
) {
    if (dataPoints.isEmpty()) return

    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(dataPoints) {
        if (animate) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(900))
        } else {
            progress.snapTo(1f)
        }
    }

    val maxNominal = dataPoints.maxOfOrNull { it.nominalValue } ?: 1.0
    val maxReal = dataPoints.maxOfOrNull { it.realValue ?: 0.0 } ?: 0.0
    val maxContributed = dataPoints.maxOfOrNull { it.contributed } ?: 0.0
    val maxY = maxOf(maxNominal, maxReal, maxContributed, 1.0) * 1.08
    val minY = 0.0

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = nominalColor, label = "Nominal")
            LegendItem(color = realColor, label = "Real (CPI)")
            if (showContributed) {
                LegendItem(color = contributedColor, label = "Contributed")
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            val width = size.width
            val height = size.height
            val visibleCount = (dataPoints.size * progress.value).toInt().coerceAtLeast(1)
            val visible = dataPoints.take(visibleCount)
            val xStep = width / (dataPoints.size - 1).coerceAtLeast(1)

            fun yFor(value: Double): Float =
                (height - ((value - minY) / (maxY - minY) * height)).toFloat()

            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 2f
            )

            fun buildPath(selector: (ChartSeriesPoint) -> Double?): Path {
                val path = Path()
                var started = false
                visible.forEachIndexed { index, point ->
                    val value = selector(point) ?: return@forEachIndexed
                    val x = index * xStep
                    val y = yFor(value)
                    if (!started) {
                        path.moveTo(x, y)
                        started = true
                    } else {
                        path.lineTo(x, y)
                    }
                }
                return path
            }

            if (showContributed) {
                drawPath(
                    path = buildPath { it.contributed },
                    color = contributedColor.copy(alpha = 0.85f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
            drawPath(
                path = buildPath { it.nominalValue },
                color = nominalColor,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
            drawPath(
                path = buildPath { it.realValue },
                color = realColor,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "━", color = color, style = MaterialTheme.typography.labelLarge)
        Text(
            text = " $label",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
    }
}
