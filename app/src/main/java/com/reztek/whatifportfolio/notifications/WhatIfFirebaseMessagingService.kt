package com.reztek.whatifportfolio.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reztek.whatifportfolio.R
import com.reztek.whatifportfolio.data.preferences.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Handles FCM token refresh and foreground messages — PART3-DAVID.
 */
class WhatIfFirebaseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Log.i(TAG, "FCM onNewToken length=${token.length}")
        scope.launch { persistToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.i(TAG, "FCM message from=${message.from} data=${message.data}")
        if (!LocaleHelper.notificationsAllowed(this)) {
            Log.i(TAG, "Notifications disabled — not posting")
            return
        }
        val simulationId = NotificationIntentParser.simulationIdFrom(message.data) ?: return
        val title = message.notification?.title
            ?: getString(R.string.notification_simulation_ready_title)
        val body = message.notification?.body
            ?: message.data["body"]?.takeIf { it.isNotBlank() }
            ?: getString(R.string.notification_simulation_ready_body)
        showSimulationReadyNotification(this, simulationId, title, body)
    }

    private suspend fun persistToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "Skipping FCM token write — no signed-in user")
            return
        }
        if (!LocaleHelper.notificationsAllowed(this)) {
            Log.i(TAG, "Notifications disabled — dropping refreshed token")
            return
        }
        try {
            val locale = LocaleHelper.storedTagOrNull(this) ?: LocaleHelper.ENGLISH
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(
                    mapOf(
                        "fcmToken" to token,
                        "appLocale" to locale,
                        "notificationsEnabled" to true
                    ),
                    SetOptions.merge()
                )
                .await()
            Log.i(TAG, "Saved FCM token to users/$uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token", e)
        }
    }

    companion object {
        private const val TAG = "WhatIfFcmService"
    }
}
