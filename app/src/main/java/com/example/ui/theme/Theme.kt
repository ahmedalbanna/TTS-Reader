package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElegantDarkAccent,
    onPrimary = ElegantDarkOnAccent,
    primaryContainer = ElegantDarkAccentContainer,
    onPrimaryContainer = ElegantDarkText,
    secondary = ElegantDarkAccent,
    onSecondary = ElegantDarkOnAccent,
    tertiary = AccentCoral,
    background = ElegantDarkBg,
    surface = ElegantDarkSurface,
    onBackground = ElegantDarkText,
    onSurface = ElegantDarkText,
    surfaceVariant = ElegantDarkBorder,
    onSurfaceVariant = ElegantDarkTextMuted
)

private val LightColorScheme = darkColorScheme( // Enforce Elegant Dark styling for consistent branding
    primary = ElegantDarkAccent,
    onPrimary = ElegantDarkOnAccent,
    primaryContainer = ElegantDarkAccentContainer,
    onPrimaryContainer = ElegantDarkText,
    secondary = ElegantDarkAccent,
    onSecondary = ElegantDarkOnAccent,
    tertiary = AccentCoral,
    background = ElegantDarkBg,
    surface = ElegantDarkSurface,
    onBackground = ElegantDarkText,
    onSurface = ElegantDarkText,
    surfaceVariant = ElegantDarkBorder,
    onSurfaceVariant = ElegantDarkTextMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is supported on Android 12+ but we want our custom curated Teal theme to shine
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
