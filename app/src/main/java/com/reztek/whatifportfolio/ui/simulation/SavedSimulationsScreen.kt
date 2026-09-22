package com.reztek.whatifportfolio.ui.simulation

import android.app.Application
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reztek.whatifportfolio.data.local.SavedSimulationEntity
import com.reztek.whatifportfolio.data.local.SimulationDatabase
import com.reztek.whatifportfolio.data.local.SimulationRepository
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

sealed interface SavedListItem {
    val key: String
    val title: String
    val subtitle: String
    val valueLabel: String
    val chipLabel: String
    val chipPositive: Boolean

    data class Remote(val simulation: SimulationDto) : SavedListItem {
        override val key = "remote-${simulation.id}"
        override val title = simulation.name
        override val subtitle = "${simulation.startDate} → ${simulation.endDate}"
        override val valueLabel: String
            get() = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).format(simulation.finalValue)
        override val chipLabel = "${"%.1f".format(simulation.percentReturn)}%"
        override val chipPositive = simulation.percentReturn >= 0
    }

    data class Local(val entity: SavedSimulationEntity) : SavedListItem {
        override val key = "local-${entity.id}"
        override val title = entity.title
        override val subtitle = "Future projection · ${entity.years}y @ ${entity.returnRate}%"
        override val valueLabel: String
            get() = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).format(entity.finalNominalValue)
        override val chipLabel = "On device"
        override val chipPositive = true
    }
}

sealed interface SavedListUiState {
    data object Loading : SavedListUiState
    data class Ready(val items: List<SavedListItem>, val query: String = "") : SavedListUiState
    data class Error(val message: String) : SavedListUiState
}

class SavedSimulationsViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val remote = RemoteSimulationRepository()
    private val local = SimulationRepository(
        SimulationDatabase.getDatabase(application).simulationDao()
    )

    private val _remoteItems = MutableStateFlow<List<SimulationDto>>(emptyList())
    private val _query = MutableStateFlow("")
    private val _loading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SavedListUiState> = combine(
        _loading,
        _error,
        _remoteItems,
        local.allSimulations,
        _query
    ) { loading, error, remoteItems, localItems, query ->
        when {
            loading && remoteItems.isEmpty() && localItems.isEmpty() -> SavedListUiState.Loading
            error != null && remoteItems.isEmpty() && localItems.isEmpty() ->
                SavedListUiState.Error(error)
            else -> {
                val merged = buildList {
                    addAll(remoteItems.map { SavedListItem.Remote(it) })
                    addAll(localItems.map { SavedListItem.Local(it) })
                }
                SavedListUiState.Ready(merged, query)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedListUiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _remoteItems.value = remote.listSimulations()
            } catch (e: Exception) {
                // Keep local projections visible even if the API is unreachable.
                if (_remoteItems.value.isEmpty()) {
                    _error.value = e.localizedMessage ?: "Couldn't load cloud simulations."
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun onQueryChanged(query: String) {
        _query.value = query
    }

    fun delete(item: SavedListItem) {
        viewModelScope.launch {
            when (item) {
                is SavedListItem.Remote -> {
                    try {
                        remote.deleteSimulation(item.simulation.id)
                        _remoteItems.value = _remoteItems.value.filterNot { it.id == item.simulation.id }
                    } catch (_: Exception) {
                        refresh()
                    }
                }
                is SavedListItem.Local -> {
                    local.deleteSimulationById(item.entity.id)
                }
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
                "Cloud historical runs and on-device future projections.",
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
                            it.title.contains(state.query, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        EmptyState(
                            title = "No saved simulations",
                            subtitle = "Run a historical backtest or save a future projection from the builder."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.key }) { item ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                viewModel.delete(item)
                                                true
                                            }
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                if (item is SavedListItem.Remote) {
                                                    onRerun(item.simulation.id)
                                                }
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
                                            contentAlignment = if (towardEnd) {
                                                Alignment.CenterEnd
                                            } else {
                                                Alignment.CenterStart
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (towardEnd) {
                                                    Icons.Filled.Delete
                                                } else {
                                                    Icons.Filled.Refresh
                                                },
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
                                            .clickable {
                                                if (item is SavedListItem.Remote) {
                                                    onOpenSimulation(item.simulation.id)
                                                }
                                            }
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text(item.title, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                item.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    item.valueLabel,
                                                    color = TealPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                StatusChip(
                                                    text = item.chipLabel,
                                                    color = if (item.chipPositive) SuccessGreen else ErrorRed
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
