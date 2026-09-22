package com.reztek.whatifportfolio.ui.simulation

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reztek.whatifportfolio.ui.components.InlineErrorBanner
import com.reztek.whatifportfolio.ui.components.PrimaryActionButton
import com.reztek.whatifportfolio.ui.components.SectionHeader
import com.reztek.whatifportfolio.ui.theme.BackgroundLight
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationBuilderScreen(
    onBack: () -> Unit,
    onSimulationCreated: (String) -> Unit,
    draftId: String? = null,
    viewModel: SimulationBuilderViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAssetSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(state.createdSimulationId) {
        state.createdSimulationId?.let { id ->
            viewModel.consumeCreatedId()
            onSimulationCreated(id)
        }
    }

    // Prefill from remote draft when duplicating/rerunning via draftId
    LaunchedEffect(draftId) {
        if (!draftId.isNullOrBlank()) {
            try {
                val repo = com.reztek.whatifportfolio.data.repository.RemoteSimulationRepository()
                val sim = repo.getSimulation(draftId)
                viewModel.prefillFromDraft(
                    name = "${sim.name} (copy)",
                    initialInvestment = sim.initialInvestment,
                    recurringContribution = sim.recurringContribution,
                    frequency = sim.frequency,
                    startDate = sim.startDate,
                    endDate = sim.endDate,
                    allocations = sim.allocations.orEmpty()
                )
            } catch (_: Exception) {
                // Keep blank draft if fetch fails
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New simulation", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            SectionHeader(
                title = "Scenario setup",
                subtitle = "Define contributions, dates, and multi-asset allocations."
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Simulation name") },
                singleLine = true,
                isError = state.fieldErrors.containsKey("name"),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            state.fieldErrors["name"]?.let {
                InlineErrorBanner(it, Modifier.padding(top = 8.dp))
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.initialInvestment,
                onValueChange = viewModel::onInitialInvestmentChanged,
                label = { Text("Initial investment") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.fieldErrors.containsKey("initial"),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            state.fieldErrors["initial"]?.let {
                InlineErrorBanner(it, Modifier.padding(top = 8.dp))
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.recurringContribution,
                onValueChange = viewModel::onRecurringContributionChanged,
                label = { Text("Recurring contribution") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.fieldErrors.containsKey("contribution"),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            state.fieldErrors["contribution"]?.let {
                InlineErrorBanner(it, Modifier.padding(top = 8.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text("Contribution frequency", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            val frequencies = listOf("monthly", "quarterly", "annual")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                frequencies.forEachIndexed { index, freq ->
                    SegmentedButton(
                        selected = state.frequency == freq,
                        onClick = { viewModel.onFrequencyChanged(freq) },
                        shape = SegmentedButtonDefaults.itemShape(index, frequencies.size)
                    ) {
                        Text(freq.replaceFirstChar { it.uppercase() })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(
                    label = "Start date",
                    value = state.startDate,
                    modifier = Modifier.weight(1f),
                    onPick = { year, month, day ->
                        viewModel.onStartDateChanged(
                            LocalDate.of(year, month + 1, day).toString()
                        )
                    }
                )
                DateField(
                    label = "End date",
                    value = state.endDate,
                    modifier = Modifier.weight(1f),
                    onPick = { year, month, day ->
                        viewModel.onEndDateChanged(
                            LocalDate.of(year, month + 1, day).toString()
                        )
                    }
                )
            }
            state.fieldErrors["dates"]?.let {
                InlineErrorBanner(it, Modifier.padding(top = 8.dp))
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Allocations",
                    subtitle = "Total ${"%.1f".format(state.allocationTotal)}%"
                )
                OutlinedButton(onClick = { showAssetSheet = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Add asset")
                }
            }

            Spacer(Modifier.height(8.dp))
            state.allocationError?.let {
                InlineErrorBanner(it, Modifier.padding(bottom = 8.dp))
            }

            state.allocations.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(row.symbol.uppercase(), fontWeight = FontWeight.SemiBold)
                        Text(
                            row.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                    OutlinedTextField(
                        value = row.percent,
                        onValueChange = { viewModel.onAllocationPercentChanged(row.symbol, it) },
                        label = { Text("%") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(0.28f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(onClick = { viewModel.removeAllocation(row.symbol) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove")
                    }
                }
            }

            if (state.allocations.isEmpty()) {
                Text(
                    "No assets yet. Add equities or crypto to allocate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            state.runError?.let {
                InlineErrorBanner(it, Modifier.padding(bottom = 12.dp))
            }

            PrimaryActionButton(
                text = "Run simulation",
                enabled = state.isValid,
                isLoading = state.isRunning,
                onClick = viewModel::runSimulation
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAssetSheet) {
        AssetSelectorSheet(
            viewModel = viewModel,
            onDismiss = { showAssetSheet = false },
            onConfirm = { assets -> viewModel.addAssets(assets) }
        )
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onPick: (year: Int, month: Int, day: Int) -> Unit
) {
    val context = LocalContext.current
    val date = runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.now() }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> onPick(y, m, d) },
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth
                    ).show()
                }
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date")
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(14.dp)
    )
}
