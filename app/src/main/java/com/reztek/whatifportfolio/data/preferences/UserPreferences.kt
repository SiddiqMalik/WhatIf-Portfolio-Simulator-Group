package com.reztek.whatifportfolio.data.preferences

/**
 * Immutable snapshot of the user's locally persisted preferences.
 *
 * PART3-DAVID: notificationsEnabled and appLocale ("" = system/English,
 * "af" = Afrikaans, "xh" = isiXhosa).
 */
data class UserPreferences(
    val displayCurrency: String = "ZAR",
    val useDarkTheme: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val appLocale: String = ""
)

/** Currencies offered in the Settings screen. */
val SUPPORTED_CURRENCIES = listOf("ZAR", "USD", "EUR", "GBP")

/** Locales offered in Settings — PART3-DAVID. */
val SUPPORTED_LOCALES = listOf(
    LocaleOption(tag = "", labelKey = "language_english"),
    LocaleOption(tag = "af", labelKey = "language_afrikaans"),
    LocaleOption(tag = "xh", labelKey = "language_isixhosa")
)

data class LocaleOption(val tag: String, val labelKey: String)
