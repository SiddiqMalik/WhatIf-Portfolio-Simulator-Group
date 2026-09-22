package com.reztek.whatifportfolio.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.reztek.whatifportfolio.data.engine.SimulationPoint
import com.reztek.whatifportfolio.data.model.SimulationResult
@Composable
fun PortfolioCanvasChart(
    dataPoints: List<SimulationPoint>,
    modifier: Modifier = Modifier,
    nominalColor: Color = Color(0xFF2E7D32), // Green
    realColor: Color = Color(0xFF1976D2)     // Blue
) {
    if (dataPoints.isEmpty()) return

    val maxNominal = dataPoints.maxOfOrNull { it.nominalValue } ?: 1.0
    val maxReal = dataPoints.maxOfOrNull { it.realValue } ?: 1.0
    val maxY = maxOf(maxNominal, maxReal) * 1.1 // 10% padding on top
    val minY = 0.0

    Column(modifier = modifier) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row {
                Text(text = "━ ", color = nominalColor)
                Text(text = "Nominal Growth", style = MaterialTheme.typography.labelMedium)
            }
            Row {
                Text(text = "━ ", color = realColor)
                Text(text = "Real Purchasing Power (CPI)", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Canvas Graph
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val width = size.width
            val height = size.height
            val xStep = width / (dataPoints.size - 1).coerceAtLeast(1)

            val nominalPath = Path()
            val realPath = Path()

            dataPoints.forEachIndexed { index, point ->
                val x = index * xStep
                // Invert Y coordinate since Canvas Y increases downward
                val nominalY = height - ((point.nominalValue - minY) / (maxY - minY) * height).toFloat()
                val realY = height - ((point.realValue - minY) / (maxY - minY) * height).toFloat()

                if (index == 0) {
                    nominalPath.moveTo(x, nominalY)
                    realPath.moveTo(x, realY)
                } else {
                    nominalPath.lineTo(x, nominalY)
                    realPath.lineTo(x, realY)
                }
            }

            // Draw baseline
            drawLine(
                color = Color.LightGray,
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 2f
            )

            // Draw Nominal Line
            drawPath(
                path = nominalPath,
                color = nominalColor,
                style = Stroke(width = 5f)
            )

            // Draw Real Purchasing Power Line
            drawPath(
                path = realPath,
                color = realColor,
                style = Stroke(width = 5f)
            )
        }
    }
}