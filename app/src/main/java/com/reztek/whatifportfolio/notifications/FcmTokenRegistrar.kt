package com.reztek.whatifportfolio.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Registers / refreshes the device FCM token against the signed-in user — PART3-DAVID.
 */
object FcmTokenRegistrar {
    private const val TAG = "FcmTokenRegistrar"

    suspend fun registerCurrentToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.w(TAG, "No user — skip FCM registration")
            return
        }
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .await()
            Log.i(TAG, "Registered FCM token for uid=$uid")
        } catch (e: Exception) {
            Log.e(TAG, "FCM token registration failed", e)
        }
    }
}
