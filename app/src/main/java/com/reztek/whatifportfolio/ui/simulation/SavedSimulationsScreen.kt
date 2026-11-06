package com.reztek.whatifportfolio.ui.simulation

import android.app.Application
import android.util.Log
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.reztek.whatifportfolio.data.local.SyncStatus
import com.reztek.whatifportfolio.data.local.toDto
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.data.sync.ConnectivityObserver
import com.reztek.whatifportfolio.data.sync.OfflineSimulationRepository
import com.reztek.whatifportfolio.data.sync.SyncOutcome
import com.reztek.whatifportfolio.data.sync.SyncScheduler
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
import com.reztek.whatifportfolio.ui.theme.WarningAmber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.reztek.whatifportfolio.ui.i18n.localize
import com.reztek.whatifportfolio.ui.i18n.localized
import com.reztek.whatifportfolio.R

sealed interface SavedListItem {
    val key: String
    val title: String
    val subtitle: String
    val valueLabel: String
    val chipLabel: String
    val chipPositive: Boolean

    data class Remote(
        val simulation: SimulationDto,
        val syncStatus: SyncStatus = SyncStatus.SYNCED
    ) : SavedListItem {
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

/**
 * PART3-ASH: offline mode with sync. Cloud simulations now come from the Room
 * cache (so the list works with no connection); rename/delete are queued and
 * pushed to the REST API by [SyncScheduler] / [OfflineSimulationRepository].
 */
class SavedSimulationsViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val appContext: Application = application
    private val offline = OfflineSimulationRepository.create(application)
    private val local = SimulationRepository(
        SimulationDatabase.getDatabase(application).simulationDao()
    )
    private val connectivity = ConnectivityObserver(application)

    private val _query = MutableStateFlow("")
    private val _loading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _message = MutableStateFlow<String?>(null)

    val isOnline: StateFlow<Boolean> = connectivity.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), connectivity.isOnlineNow())
    val unsyncedCount: StateFlow<Int> = offline.unsyncedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val message: StateFlow<String?> = _message

    val uiState: StateFlow<SavedListUiState> = combine(
        _loading,
        _error,
        offline.visibleSimulations,
        local.allSimulations,
        _query
    ) { loading, error, cachedItems, localItems, query ->
        when {
            loading && cachedItems.isEmpty() && localItems.isEmpty() -> SavedListUiState.Loading
            error != null && cachedItems.isEmpty() && localItems.isEmpty() ->
                SavedListUiState.Error(error)
            else -> {
                val merged = buildList {
                    addAll(cachedItems.map { SavedListItem.Remote(it.toDto(), it.syncStatus) })
                    addAll(localItems.map { SavedListItem.Local(it) })
                }
                SavedListUiState.Ready(merged, query)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedListUiState.Loading)

    init {
        Log.d(TAG, "SavedSimulationsViewModel created")
        refresh()
        // When the connection comes back: push queued changes, then pull the latest list.
        viewModelScope.launch {
            var wasOnline = true
            connectivity.isOnline.collect { online ->
                if (online && !wasOnline) {
                    Log.i(TAG, "Connection restored - syncing")
                    SyncScheduler.enqueue(appContext)
                    refresh()
                }
                wasOnline = online
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            // Flush queued edits first so the server list already reflects them.
            runCatching { offline.syncPending() }
            try {
                offline.refreshFromServer()
                // The list is current; fetching full results for offline use is background work.
                _loading.value = false
                runCatching { offline.prefetchDetails() }
            } catch (e: Exception) {
                Log.w(TAG, "Refresh failed, showing cached data: ${e.message}")
                // Cached simulations stay visible; only error out when there is nothing to show.
                if (!offline.hasCache()) {
                    _error.value = if (e is IOException) {
                        "You're offline and nothing is saved on this device yet. Connect once to load your simulations."
                    } else {
                        e.localizedMessage ?: "Couldn't load cloud simulations."
                    }
                }
            } finally {
                _loading.value = false
            }
        }
    }

    /** Manual "Sync now": retries failed items too, then reports the result. */
    fun syncNow() {
        viewModelScope.launch {
            offline.requeueFailed()
            val outcome = runCatching { offline.syncPending() }.getOrNull()
            _message.value = if (outcome == SyncOutcome.DONE) {
                "All changes synced"
            } else {
                "Still waiting to sync - check your connection"
            }
            refresh()
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun onQueryChanged(query: String) {
        _query.value = query
    }

    fun rename(item: SavedListItem.Remote, newName: String) {
        viewModelScope.launch {
            if (offline.renameLocally(item.simulation.id, newName)) {
                SyncScheduler.enqueue(appContext)
                if (!connectivity.isOnlineNow()) {
                    _message.value = "Saved on this device - will sync when you're back online"
                }
            }
        }
    }

    fun delete(item: SavedListItem) {
        viewModelScope.launch {
            when (item) {
                is SavedListItem.Remote -> {
                    if (offline.deleteLocally(item.simulation.id)) {
                        SyncScheduler.enqueue(appContext)
                        if (!connectivity.isOnlineNow()) {
                            _message.value = "Deleted on this device - will sync when you're back online"
                        }
                    }
                }
                is SavedListItem.Local -> {
                    local.deleteSimulationById(item.entity.id)
                }
            }
        }
    }

    private companion object {
        const val TAG = "SavedSimulationsVM"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSimulationsScreen(
    onOpenSimulation: (String) -> Unit,
    onRerun: (String) -> Unit,
    onCompare: () -> Unit,
    onBottomNavSelected: (BottomNavDestination) -> Unit,
    onCompare: () -> Unit = {},
    viewModel: SavedSimulationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // PART3-ASH: offline mode with sync.
    val isOnline by viewModel.isOnline.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var renameTarget by remember { mutableStateOf<SavedListItem.Remote?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(localize(context, it))
            viewModel.consumeMessage()
        }
    }

    renameTarget?.let { target ->
        RenameDialog(
            currentName = target.title,
            onConfirm = { newName ->
                viewModel.rename(target, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.saved_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                // Entry point to the Compare feature (pick two saved runs).
                OutlinedButton(onClick = onCompare) { Text(stringResource(R.string.action_compare)) }
            }
            Text(
                stringResource(R.string.saved_cloud_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Spacer(Modifier.height(16.dp))

            OfflineSyncBar(
                isOnline = isOnline,
                unsyncedCount = unsyncedCount,
                onSyncNow = viewModel::syncNow
            )
            if (!isOnline || unsyncedCount > 0) Spacer(Modifier.height(12.dp))

            when (val state = uiState) {
                is SavedListUiState.Loading -> {
                    repeat(3) {
                        SkeletonRow(Modifier.padding(bottom = 10.dp))
                    }
                }
                is SavedListUiState.Error -> {
                    InlineErrorBanner(state.message)
                    Spacer(Modifier.height(12.dp))
                    PrimaryActionButton(stringResource(R.string.action_retry), onClick = viewModel::refresh)
                }
                is SavedListUiState.Ready -> {
                    if (state.items.size >= 2) {
                        OutlinedButton(
                            onClick = onCompare,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Compare simulations") }
                        Spacer(Modifier.height(12.dp))
                    }
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.label_search)) },
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    val filtered = state.items.filter {
                        state.query.isBlank() ||
                            it.title.contains(state.query, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.saved_empty_title),
                            subtitle = stringResource(R.string.saved_empty_subtitle)
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
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    item.title,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (item is SavedListItem.Remote) {
                                                    IconButton(onClick = { renameTarget = item }) {
                                                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_rename))
                                                    }
                                                }
                                            }
                                            Text(
                                                if (item is SavedListItem.Local) {
                                                    stringResource(
                                                        R.string.saved_local_subtitle,
                                                        item.entity.years,
                                                        item.entity.returnRate
                                                    )
                                                } else {
                                                    item.subtitle
                                                },
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
                                                    text = localized(item.chipLabel),
                                                    color = if (item.chipPositive) SuccessGreen else ErrorRed
                                                )
                                                if (item is SavedListItem.Remote && item.syncStatus != SyncStatus.SYNCED) {
                                                    StatusChip(
                                                        text = localized(syncLabel(item.syncStatus)),
                                                        color = if (
                                                            item.syncStatus == SyncStatus.FAILED_UPDATE ||
                                                            item.syncStatus == SyncStatus.FAILED_DELETE
                                                        ) ErrorRed else WarningAmber
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
}
