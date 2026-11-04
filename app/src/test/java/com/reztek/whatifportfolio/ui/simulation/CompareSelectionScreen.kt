package com.reztek.whatifportfolio.ui.simulation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reztek.whatifportfolio.ui.components.EmptyState

/**
 * Selection UI for choosing exactly two saved simulations for comparison.
 *
 * IDs are the stable keys already used by SavedListItem ("remote-…" / "local-…").
 * Navigation is intentionally delegated to the caller so this screen can be
 * connected to the graph in the navigation commit.
 */
@Composable
fun CompareSelectionScreen(
    savedItems: List<SavedListItem>,
    onCompareSelected: (firstId: String, secondId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIds = remember { mutableStateListOf<String>() }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    "${selectedIds.size} of 2 selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (selectedIds.size == 2) {
                            onCompareSelected(selectedIds[0], selectedIds[1])
                        }
                    },
                    enabled = selectedIds.size == 2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Compare simulations")
                }
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Compare simulations",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onBack) {
                    Text("Back")
                }
            }
            Text(
                "Choose two saved runs. You can compare cloud runs and on-device projections.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (savedItems.isEmpty()) {
                EmptyState(
                    title = "No saved simulations",
                    subtitle = "Save or run a simulation before comparing."
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedItems, key = { it.key }) { item ->
                        val isSelected = item.key in selectedIds
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                when {
                                    isSelected -> selectedIds.remove(item.key)
                                    selectedIds.size < 2 -> selectedIds.add(item.key)
                                    else -> Unit
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        when {
                                            checked && !isSelected && selectedIds.size < 2 ->
                                                selectedIds.add(item.key)
                                            !checked && isSelected -> selectedIds.remove(item.key)
                                        }
                                    }
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        item.valueLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    if (item is SavedListItem.Local) "On device" else "Cloud",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
