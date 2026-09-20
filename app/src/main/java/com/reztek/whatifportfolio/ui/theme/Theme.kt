package com.reztek.whatifportfolio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = SurfaceLight,
    primaryContainer = TealLight,
    onPrimaryContainer = NavyDeep,
    secondary = NavyMid,
    onSecondary = SurfaceLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = BackgroundLight,
    outline = OutlineLight,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = NavyDeep,
    primaryContainer = TealDark,
    onPrimaryContainer = SurfaceLight,
    secondary = TealPrimary,
    onSecondary = NavyDeep,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = NavySurface,
    outline = OutlineDark,
    error = ErrorRed
)

/**
 * App-wide theme wrapper.
 *
 * Dynamic colour (Material You) is intentionally left OFF by default
 * ([useDynamicColor] = false): the brand relies on the teal/navy pair used in
 * the app icon and the dual-line chart, and letting the system wallpaper
 * override that would break the visual link between the icon, the charts,
 * and the chrome. It's exposed as a parameter in case a future POE iteration
 * wants to offer it as a user preference.
 */
@Composable
fun WhatIfPortfolioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
