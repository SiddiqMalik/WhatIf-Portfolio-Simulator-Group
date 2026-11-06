package com.reztek.whatifportfolio.data.preferences

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.reztek.whatifportfolio.notifications.ensureSimulationNotificationChannel

/**
 * Persists and applies English, Afrikaans, or isiXhosa — PART3-DAVID.
 *
 * AppCompat stores the per-app locale itself, which is what actually updates
 * resources on API 26 through 34. A small SharedPreferences mirror is written
 * synchronously so the next cold start can apply the language before the first
 * frame, without waiting for DataStore.
 */
object LocaleHelper {
    private const val TAG = "LocaleHelper"
    private const val PREFS = "whatif_locale"
    private const val KEY_TAG = "tag"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    private const val KEY_PERMISSION_ASKED = "notification_permission_asked"

    const val ENGLISH = "en"
    const val AFRIKAANS = "af"
    const val XHOSA = "xh"

    fun normalize(tag: String): String = when (tag) {
        AFRIKAANS -> AFRIKAANS
        XHOSA -> XHOSA
        else -> ENGLISH
    }

    fun storedTagOrNull(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TAG, null)
            ?.let(::normalize)

    /**
     * Applies the last language chosen, before any activity is created.
     * No-op until the user (or DataStore migration) has stored a tag.
     */
    fun applyStoredLocale(context: Context) {
        val tag = storedTagOrNull(context) ?: return
        applyAppLocale(context, tag, persist = false)
    }

    fun applyAppLocale(context: Context, tag: String, persist: Boolean = true) {
        val normalized = normalize(tag)
        if (persist) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TAG, normalized)
                .commit()
        }
        Log.i(TAG, "Applying app locale '$normalized'")
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalized))
        ensureSimulationNotificationChannel(context)
    }

    fun notificationsAllowed(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotificationsAllowed(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATIONS, enabled)
            .apply()
    }

    fun notificationPermissionAsked(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERMISSION_ASKED, false)

    fun markNotificationPermissionAsked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PERMISSION_ASKED, true)
            .apply()
    }

    fun displayNameFor(tag: String): String = when (normalize(tag)) {
        AFRIKAANS -> "Afrikaans"
        XHOSA -> "isiXhosa"
        else -> "English"
    }
}
