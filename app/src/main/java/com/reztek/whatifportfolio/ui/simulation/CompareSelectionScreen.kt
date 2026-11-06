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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reztek.whatifportfolio.ui.components.EmptyState
import androidx.compose.ui.res.stringResource
import com.reztek.whatifportfolio.R

/**
 * Selection UI for choosing exactly two saved simulations for comparison.
 *
 * IDs are the stable keys already used by SavedListItem ("remote-…" / "local-…").
 * Exactly two different items must be selected before the button enables; a
 * third tap is refused with a hint (rules live in [CompareSelectionRules]).
 * Navigation is delegated to the caller (see CompareSelectionRoute).
 */
@Composable
fun CompareSelectionScreen(
    savedItems: List<SavedListItem>,
    onCompareSelected: (firstId: String, secondId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rawSelection by remember { mutableStateOf<List<String>>(emptyList()) }
    var limitHint by remember { mutableStateOf(false) }
    // Ignore selections whose item disappeared (for example, deleted meanwhile).
    val selectedIds = CompareSelectionRules.pruneTo(rawSelection, savedItems.map { it.key }.toSet())
    val toggleItem: (String) -> Unit = { key ->
        limitHint = CompareSelectionRules.isBlockedThirdPick(selectedIds, key)
        rawSelection = CompareSelectionRules.toggle(selectedIds, key)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    stringResource(R.string.compare_selected_count, selectedIds.size, CompareSelectionRules.REQUIRED_COUNT),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (limitHint) {
                    Text(
                        stringResource(R.string.compare_only_two),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (CompareSelectionRules.canCompare(selectedIds)) {
                            onCompareSelected(selectedIds[0], selectedIds[1])
                        }
                    },
                    enabled = CompareSelectionRules.canCompare(selectedIds),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.compare_title))
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
                    stringResource(R.string.compare_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.action_back))
                }
            }
            Text(
                stringResource(R.string.compare_choose_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (savedItems.size < CompareSelectionRules.REQUIRED_COUNT) {
                EmptyState(
                    title = stringResource(R.string.compare_not_enough_title),
                    subtitle = stringResource(R.string.compare_not_enough_subtitle)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedItems, key = { it.key }) { item ->
                        val isSelected = item.key in selectedIds
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { toggleItem(item.key) },
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
                                    onCheckedChange = { toggleItem(item.key) }
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
                                    if (item is SavedListItem.Local) stringResource(R.string.chip_on_device) else stringResource(R.string.chip_cloud),
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
