package com.reztek.whatifportfolio.ui.simulation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reztek.whatifportfolio.data.remote.dto.AssetSearchResultDto
import com.reztek.whatifportfolio.ui.components.InlineErrorBanner
import com.reztek.whatifportfolio.ui.components.PrimaryActionButton
import com.reztek.whatifportfolio.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetSelectorSheet(
    viewModel: SimulationBuilderViewModel,
    onDismiss: () -> Unit,
    onConfirm: (List<AssetSearchResultDto>) -> Unit
) {
    val state by viewModel.assetSelector.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Add assets",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Search equities and crypto for your scenario.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search") },
                placeholder = { Text("e.g. AAPL, bitcoin") },
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", "equity" to "Stocks", "crypto" to "Crypto").forEach { (value, label) ->
                    FilterChip(
                        selected = state.filter == value,
                        onClick = { viewModel.onFilterChanged(value) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.error != null) {
                InlineErrorBanner(message = state.error!!, modifier = Modifier.padding(bottom = 8.dp))
            }

            if (state.isSearching) {
                Text(
                    "Searching…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.results, key = { "${it.type}:${it.symbol}" }) { asset ->
                    val selected = asset.symbol in state.selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleAssetSelection(asset.symbol) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(asset.symbol.uppercase(), fontWeight = FontWeight.SemiBold)
                            Text(
                                asset.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                asset.type.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = TealPrimary
                            )
                        }
                        IconButton(onClick = { viewModel.toggleAssetSelection(asset.symbol) }) {
                            Icon(
                                imageVector = if (selected) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = if (selected) "Selected" else "Add",
                                tint = if (selected) TealPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            PrimaryActionButton(
                text = if (state.selected.isEmpty()) {
                    "Add selected assets"
                } else {
                    "Add ${state.selected.size} selected asset${if (state.selected.size == 1) "" else "s"}"
                },
                enabled = state.selected.isNotEmpty(),
                onClick = {
                    onConfirm(viewModel.selectedAssets())
                    viewModel.clearAssetSelection()
                    onDismiss()
                }
            )
        }
    }
}