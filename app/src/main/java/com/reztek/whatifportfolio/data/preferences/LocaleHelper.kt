package com.reztek.whatifportfolio.data.preferences

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Applies a persisted BCP-47 language tag via AppCompat — PART3-DAVID.
 */
object LocaleHelper {
    private const val TAG = "LocaleHelper"

    fun applyAppLocale(tag: String) {
        val locales = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        Log.i(TAG, "Applying app locales tag='$tag'")
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun displayNameFor(tag: String): String = when (tag) {
        "af" -> "Afrikaans"
        "xh" -> "isiXhosa"
        else -> "English"
    }
}
