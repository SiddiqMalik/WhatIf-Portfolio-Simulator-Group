package com.reztek.whatifportfolio.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.reztek.whatifportfolio.MainActivity
import com.reztek.whatifportfolio.R

private const val TAG = "NotificationHelpers"

const val CHANNEL_SIMULATIONS = "simulations"
const val EXTRA_SIMULATION_ID = "simulationId"
const val EXTRA_OPEN_DESTINATION = "openDestination"
const val DESTINATION_RESULTS = "simulation_results"

/**
 * Pure helper used by FCM and unit tests — PART3-DAVID.
 */
object NotificationIntentParser {
    fun simulationIdFrom(extras: Map<String, String>?): String? =
        extras?.get(EXTRA_SIMULATION_ID)?.takeIf { it.isNotBlank() }

    fun simulationIdFromIntent(intent: Intent?): String? =
        intent?.extras?.getString(EXTRA_SIMULATION_ID)?.trim()?.takeIf { it.isNotBlank() }
}

fun ensureSimulationNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val channel = NotificationChannel(
        CHANNEL_SIMULATIONS,
        context.getString(R.string.notification_channel_simulations),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.notification_channel_simulations_desc)
    }
    manager.createNotificationChannel(channel)
    Log.i(TAG, "Ensured notification channel $CHANNEL_SIMULATIONS")
}

fun showSimulationReadyNotification(
    context: Context,
    simulationId: String,
    title: String,
    body: String
) {
    ensureSimulationNotificationChannel(context)
    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_SIMULATION_ID, simulationId)
        putExtra(EXTRA_OPEN_DESTINATION, DESTINATION_RESULTS)
    }
    val pending = PendingIntent.getActivity(
        context,
        simulationId.hashCode(),
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_SIMULATIONS)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setContentIntent(pending)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    try {
        NotificationManagerCompat.from(context).notify(simulationId.hashCode(), notification)
        Log.i(TAG, "Posted simulation-ready notification for id=$simulationId")
    } catch (e: SecurityException) {
        Log.e(TAG, "Missing POST_NOTIFICATIONS permission", e)
    }
}
