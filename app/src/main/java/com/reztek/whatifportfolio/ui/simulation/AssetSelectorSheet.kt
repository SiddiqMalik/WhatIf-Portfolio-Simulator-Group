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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class AssetOption(
    val symbol: String,
    val name: String
)

val availableAssets = listOf(
    AssetOption("BTC", "Bitcoin"),
    AssetOption("ETH", "Ethereum"),
    AssetOption("AAPL", "Apple Inc."),
    AssetOption("MSFT", "Microsoft Corp."),
    AssetOption("NVDA", "NVIDIA Corp."),
    AssetOption("SPY", "S&P 500 ETF")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetSelectorSheet(
    viewModel: SimulationBuilderViewModel,
    onDismiss: () -> Unit,
    onConfirm: (List<AllocationRow>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val selectedAssets = remember { mutableStateListOf<AssetOption>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Select Assets to Include",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                items(availableAssets) { asset ->
                    val isSelected = selectedAssets.contains(asset)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) {
                                    selectedAssets.remove(asset)
                                } else {
                                    selectedAssets.add(asset)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = asset.symbol, style = MaterialTheme.typography.bodyLarge)
                            Text(text = asset.name, style = MaterialTheme.typography.bodySmall)
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) selectedAssets.add(asset) else selectedAssets.remove(asset)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val allocations = selectedAssets.map {
                        AllocationRow(
                            symbol = it.symbol,
                            name = it.name,
                            percent = "0"
                        )
                    }
                    onConfirm(allocations)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Selected Assets")
            }
        }
    }
}