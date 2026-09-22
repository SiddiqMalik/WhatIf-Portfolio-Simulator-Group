package com.reztek.whatifportfolio.ui.dashboard

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reztek.whatifportfolio.navigation.BottomNavDestination
import com.reztek.whatifportfolio.ui.components.EmptyState
import com.reztek.whatifportfolio.ui.components.StatusChip
import com.reztek.whatifportfolio.ui.components.WhatIfBottomNavBar
import com.reztek.whatifportfolio.ui.theme.BackgroundLight
import com.reztek.whatifportfolio.ui.theme.ErrorRed
import com.reztek.whatifportfolio.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

private const val TAG = "DashboardScreen"

/**
 * Home/Dashboard screen (Deliverable 3, Screen 2). Landing screen after
 * authentication — snapshot of recent activity plus the two primary
 * actions: start a new simulation, or browse everything saved.
 */
@Composable
fun DashboardScreen(
    onNewSimulation: () -> Unit,
    onOpenSimulation: (String) -> Unit,
    onViewAllSaved: () -> Unit,
    onBottomNavSelected: (BottomNavDestination) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        Log.d(TAG, "DashboardUiState -> ${uiState::class.simpleName}")
    }

    Scaffold(
        containerColor = BackgroundLight,
        bottomBar = {
            WhatIfBottomNavBar(
                currentDestination = BottomNavDestination.HOME,
                onDestinationSelected = onBottomNavSelected
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("New Simulation", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = {
                    Log.i(TAG, "New Simulation FAB tapped")
                    onNewSimulation()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                Column(Modifier.padding(padding).padding(20.dp)) {
                    com.reztek.whatifportfolio.ui.components.SkeletonRow()
                    Spacer(Modifier.height(10.dp))
                    com.reztek.whatifportfolio.ui.components.SkeletonRow()
                    Spacer(Modifier.height(10.dp))
                    com.reztek.whatifportfolio.ui.components.SkeletonRow()
                }
            }
            is DashboardUiState.Error -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }

            is DashboardUiState.Loaded -> DashboardContent(
                state = state,
                contentPadding = padding,
                onOpenSimulation = onOpenSimulation,
                onViewAllSaved = onViewAllSaved
            )
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Loaded,
    contentPadding: PaddingValues,
    onOpenSimulation: (String) -> Unit,
    onViewAllSaved: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = contentPadding.calculateTopPadding() + 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Welcome back, ${state.displayName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Here's what your recent scenarios looked like.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent simulations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onViewAllSaved) {
                    Text("View all")
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
            }
        }

        if (state.recentSimulations.isEmpty()) {
            item {
                EmptyState(
                    title = "Run your first simulation",
                    subtitle = "Tap \u201CNew Simulation\u201D to see what a recurring " +
                        "investment strategy would have been worth historically."
                )
            }
        } else {
            items(state.recentSimulations, key = { it.id }) { summary ->
                SimulationSummaryCard(summary = summary, onClick = { onOpenSimulation(summary.id) })
            }
        }
    }
}

@Composable
private fun SimulationSummaryCard(summary: SimulationSummary, onClick: () -> Unit) {
    val currencyFormat = remember(summary.finalValue) {
        NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
    }
    val isPositive = summary.percentReturn >= 0.0
    val returnColor = if (isPositive) SuccessGreen else ErrorRed

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(returnColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    contentDescription = null,
                    tint = returnColor
                )
            }

            Spacer(modifier = Modifier.size(width = 12.dp, height = 0.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(summary.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    currencyFormat.format(summary.finalValue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            StatusChip(
                text = "${if (isPositive) "+" else ""}${"%.1f".format(summary.percentReturn)}%",
                color = returnColor
            )
        }
    }
}
