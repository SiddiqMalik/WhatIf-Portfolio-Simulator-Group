package com.reztek.whatifportfolio.ui.simulation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.data.repository.RemoteSimulationRepository
import com.reztek.whatifportfolio.navigation.BottomNavDestination
import com.reztek.whatifportfolio.ui.components.EmptyState
import com.reztek.whatifportfolio.ui.components.InlineErrorBanner
import com.reztek.whatifportfolio.ui.components.PrimaryActionButton
import com.reztek.whatifportfolio.ui.components.SkeletonRow
import com.reztek.whatifportfolio.ui.components.StatusChip
import com.reztek.whatifportfolio.ui.components.WhatIfBottomNavBar
import com.reztek.whatifportfolio.ui.theme.BackgroundLight
import com.reztek.whatifportfolio.ui.theme.ErrorRed
import com.reztek.whatifportfolio.ui.theme.SuccessGreen
import com.reztek.whatifportfolio.ui.theme.TealPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

sealed interface SavedListUiState {
    data object Loading : SavedListUiState
    data class Ready(val items: List<SimulationDto>, val query: String = "") : SavedListUiState
    data class Error(val message: String) : SavedListUiState
}

class SavedSimulationsViewModel(
    private val repository: RemoteSimulationRepository = RemoteSimulationRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<SavedListUiState>(SavedListUiState.Loading)
    val uiState: StateFlow<SavedListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = SavedListUiState.Loading
            try {
                _uiState.value = SavedListUiState.Ready(repository.listSimulations())
            } catch (e: Exception) {
                _uiState.value = SavedListUiState.Error(
                    e.localizedMessage ?: "Couldn't load saved simulations."
                )
            }
        }
    }

    fun onQueryChanged(query: String) {
        val current = _uiState.value
        if (current is SavedListUiState.Ready) {
            _uiState.value = current.copy(query = query)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteSimulation(id)
                val current = _uiState.value
                if (current is SavedListUiState.Ready) {
                    _uiState.value = current.copy(items = current.items.filterNot { it.id == id })
                }
            } catch (_: Exception) {
                refresh()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSimulationsScreen(
    onOpenSimulation: (String) -> Unit,
    onRerun: (String) -> Unit,
    onBottomNavSelected: (BottomNavDestination) -> Unit,
    viewModel: SavedSimulationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currency = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    Scaffold(
        bottomBar = {
            WhatIfBottomNavBar(
                currentDestination = BottomNavDestination.SAVED,
                onDestinationSelected = onBottomNavSelected
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Saved simulations",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Search, reopen, rerun, or delete your scenarios.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Spacer(Modifier.height(16.dp))

            when (val state = uiState) {
                is SavedListUiState.Loading -> {
                    repeat(3) {
                        SkeletonRow(Modifier.padding(bottom = 10.dp))
                    }
                }
                is SavedListUiState.Error -> {
                    InlineErrorBanner(state.message)
                    Spacer(Modifier.height(12.dp))
                    PrimaryActionButton("Retry", onClick = viewModel::refresh)
                }
                is SavedListUiState.Ready -> {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    val filtered = state.items.filter {
                        state.query.isBlank() ||
                            it.name.contains(state.query, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        EmptyState(
                            title = "No saved simulations",
                            subtitle = "Run a historical scenario from Home to see it here."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { sim ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                viewModel.delete(sim.id)
                                                true
                                            }
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                onRerun(sim.id)
                                                false
                                            }
                                            else -> false
                                        }
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val towardEnd =
                                            dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            contentAlignment = if (towardEnd) Alignment.CenterEnd else Alignment.CenterStart
                                        ) {
                                            Icon(
                                                imageVector = if (towardEnd) Icons.Filled.Delete else Icons.Filled.Refresh,
                                                contentDescription = null,
                                                tint = if (towardEnd) ErrorRed else TealPrimary
                                            )
                                        }
                                    }
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = CardDefaults.cardElevation(0.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenSimulation(sim.id) }
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text(sim.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "${sim.startDate} → ${sim.endDate}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    currency.format(sim.finalValue),
                                                    color = TealPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                StatusChip(
                                                    text = "${"%.1f".format(sim.percentReturn)}%",
                                                    color = if (sim.percentReturn >= 0) SuccessGreen else ErrorRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
