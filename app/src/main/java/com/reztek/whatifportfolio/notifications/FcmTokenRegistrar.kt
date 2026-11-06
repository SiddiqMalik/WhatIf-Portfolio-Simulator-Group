package com.reztek.whatifportfolio.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.reztek.whatifportfolio.WhatIfApplication
import com.reztek.whatifportfolio.data.preferences.LocaleHelper
import kotlinx.coroutines.tasks.await

/**
 * Registers the device FCM token on users/{uid} — PART3-DAVID.
 * The token is removed when notifications are turned off, so the server
 * cannot send to a device that opted out. The language tag is stored beside
 * the token so the server can localise the notification.
 */
object FcmTokenRegistrar {
    private const val TAG = "FcmTokenRegistrar"

    suspend fun registerCurrentToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.w(TAG, "No user — skip FCM registration")
            return
        }
        val app = WhatIfApplication.appContext
        if (!LocaleHelper.notificationsAllowed(app)) {
            Log.i(TAG, "Notifications disabled — not storing an FCM token")
            return
        }
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            val locale = LocaleHelper.storedTagOrNull(app) ?: LocaleHelper.ENGLISH
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
            Log.i(TAG, "Registered FCM token for uid=$uid locale=$locale")
        } catch (e: Exception) {
            Log.e(TAG, "FCM token registration failed", e)
        }
    }

    suspend fun syncLocale(tag: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(mapOf("appLocale" to LocaleHelper.normalize(tag)), SetOptions.merge())
                .await()
            Log.i(TAG, "Synced appLocale for uid=$uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync appLocale", e)
        }
    }

    suspend fun clearToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(
                    mapOf(
                        "fcmToken" to FieldValue.delete(),
                        "notificationsEnabled" to false
                    ),
                    SetOptions.merge()
                )
                .await()
            Log.i(TAG, "Cleared FCM token for uid=$uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear FCM token", e)
        }
    }
}
