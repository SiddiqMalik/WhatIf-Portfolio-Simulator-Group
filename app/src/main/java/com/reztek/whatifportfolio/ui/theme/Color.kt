package com.reztek.whatifportfolio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette for the What-If Portfolio Simulator.
 *
 * Derived from the app icon concept documented in Deliverable 2 (Application
 * Overview): a line chart + question-mark motif in teal and navy, chosen so
 * small icons and chart lines stay legible at mobile sizes.
 *
 * Two lines appear throughout the app's charts (nominal vs CPI-adjusted real
 * value) — TealPrimary and NavyDeep are reserved for exactly that distinction
 * so the colour language stays consistent between the icon, the chart, and
 * the UI chrome.
 */

// Core brand colours
val TealPrimary = Color(0xFF0FA3A3)      // Nominal value line / primary actions
val TealDark = Color(0xFF0B7A7A)
val TealLight = Color(0xFF6FD8D8)

val NavyDeep = Color(0xFF0B1F3A)         // Real (inflation-adjusted) value line / headers
val NavyMid = Color(0xFF17335C)
val NavySurface = Color(0xFF1F3A63)

// Semantic colours
val SuccessGreen = Color(0xFF2FAE6B)     // Positive returns
val ErrorRed = Color(0xFFD64545)         // Negative returns / validation errors
val WarningAmber = Color(0xFFE0A72A)

// Neutrals — light theme
val BackgroundLight = Color(0xFFF7F9FB)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF10192B)
val OutlineLight = Color(0xFFD9E1EA)

// Neutrals — dark theme
val BackgroundDark = Color(0xFF0A1220)
val SurfaceDark = Color(0xFF121C30)
val OnSurfaceDark = Color(0xFFE7ECF3)
val OutlineDark = Color(0xFF2A3A56)
