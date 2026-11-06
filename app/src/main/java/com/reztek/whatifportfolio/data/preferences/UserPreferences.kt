package com.reztek.whatifportfolio.data.preferences

/**
 * Immutable snapshot of the user's locally persisted preferences.
 *
 * PART3-DAVID: notificationsEnabled and appLocale
 * ("en" = English, "af" = Afrikaans, "xh" = isiXhosa).
 */
data class UserPreferences(
    val displayCurrency: String = "ZAR",
    val useDarkTheme: Boolean = false,
    val biometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val appLocale: String = LocaleHelper.ENGLISH
)

/** Currencies offered in the Settings screen. */
val SUPPORTED_CURRENCIES = listOf("ZAR", "USD", "EUR", "GBP")

/** Locales offered in Settings — PART3-DAVID. */
val SUPPORTED_LOCALES = listOf(
    LocaleOption(tag = LocaleHelper.ENGLISH, labelKey = "language_english"),
    LocaleOption(tag = LocaleHelper.AFRIKAANS, labelKey = "language_afrikaans"),
    LocaleOption(tag = LocaleHelper.XHOSA, labelKey = "language_isixhosa")
)

data class LocaleOption(val tag: String, val labelKey: String)
