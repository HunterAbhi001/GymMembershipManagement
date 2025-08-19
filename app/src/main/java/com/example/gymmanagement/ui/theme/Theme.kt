package com.example.gymmanagement.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- Polished Dark Theme Palette ---
private val AppDarkBackground = Color(0xFF121212)
private val AppSurfaceDark = Color(0xFF1E1E1E)
private val AppPrimaryDark = Color(0xFFBB86FC)
private val AppOnSurfaceDark = Color.White
private val AppOnSurfaceVariantDark = Color(0xFFBDBDBD)

// --- NEW: Polished Light Theme Palette ---
private val AppLightBackground = Color(0xFFF7F7F7) // A subtle off-white
private val AppSurfaceLight = Color.White          // Pure white for cards to make them pop
private val AppPrimaryLight = Color(0xFF6200EE)    // A standard Material purple
private val AppOnSurfaceLight = Color.Black
private val AppOnSurfaceVariantLight = Color(0xFF6E6E6E)

// --- Define the custom dark color scheme ---
private val AppDarkColorScheme = darkColorScheme(
    primary = AppPrimaryDark,
    background = AppDarkBackground,
    surface = AppSurfaceDark,
    onBackground = AppOnSurfaceDark,
    onSurface = AppOnSurfaceDark,
    onPrimary = Color.Black,
    onSurfaceVariant = AppOnSurfaceVariantDark
)

// --- NEW: Define the custom light color scheme ---
private val AppLightColorScheme = lightColorScheme(
    primary = AppPrimaryLight,
    background = AppLightBackground,
    surface = AppSurfaceLight,
    onBackground = AppOnSurfaceLight,
    onSurface = AppOnSurfaceLight,
    onPrimary = Color.White,
    onSurfaceVariant = AppOnSurfaceVariantLight
)

@Composable
fun GymManagementTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // --- UPDATED: The logic now correctly chooses between our two polished themes ---
    val colorScheme = when {
        darkTheme -> AppDarkColorScheme
        else -> AppLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assumes you have a Typography.kt file
        content = content
    )
}
