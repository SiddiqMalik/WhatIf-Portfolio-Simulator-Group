package com.reztek.whatifportfolio.data.preferences

/**
 * Immutable snapshot of the user's locally persisted preferences.
 *
 * These are prototype-scope settings only (Deliverable 3, Settings screen):
 * display currency and a dark-mode toggle. Language is informational-only in
 * this build (English only — multilingual support is Final POE scope per the
 * requirements spec), and notification preferences are disabled ("soon")
 * until FCM is wired up in the POE phase, so neither is persisted yet.
 */
data class UserPreferences(
    val displayCurrency: String = "ZAR",
    val useDarkTheme: Boolean = false
)

/** Currencies offered in the Settings screen for the prototype. */
val SUPPORTED_CURRENCIES = listOf("ZAR", "USD", "EUR", "GBP")
