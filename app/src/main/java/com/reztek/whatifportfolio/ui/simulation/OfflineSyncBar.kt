package com.reztek.whatifportfolio.ui.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reztek.whatifportfolio.data.local.SyncStatus
import com.reztek.whatifportfolio.ui.theme.TealPrimary
import com.reztek.whatifportfolio.ui.theme.WarningAmber

/*
 * PART3-ASH: offline mode with sync - small UI pieces used by the Saved screen.
 */

/** Short label for the sync chip on a saved-simulation card. */
fun syncLabel(status: SyncStatus): String = when (status) {
    SyncStatus.SYNCED -> "Synced"
    SyncStatus.PENDING_UPDATE -> "Pending sync"
    SyncStatus.PENDING_DELETE -> "Deleting..."
    SyncStatus.FAILED_UPDATE, SyncStatus.FAILED_DELETE -> "Sync failed"
}

/** Shown on the results screens when the data is the on-device copy because there is no connection. */
@Composable
fun OfflineCopyBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WarningAmber.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Offline - showing the copy saved on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Banner shown while the device is offline or changes are waiting to sync.
 * Hidden when online with nothing queued.
 */
@Composable
fun OfflineSyncBar(
    isOnline: Boolean,
    unsyncedCount: Int,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isOnline && unsyncedCount == 0) return

    val waiting = if (unsyncedCount == 1) "1 change waiting to sync" else "$unsyncedCount changes waiting to sync"
    val message = when {
        !isOnline && unsyncedCount > 0 -> "Offline - $waiting. They sync automatically when you reconnect."
        !isOnline -> "Offline - showing saved simulations. Changes sync when you reconnect."
        else -> waiting
    }
    val color = if (isOnline) TealPrimary else WarningAmber

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (isOnline && unsyncedCount > 0) {
            TextButton(onClick = onSyncNow) { Text("Sync now") }
        }
    }
}

/** Dialog used to rename a saved simulation (works offline; the change is queued). */
@Composable
fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename simulation") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Name") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
