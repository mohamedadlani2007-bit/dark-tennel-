package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = DarkVoid,
    primaryContainer = CyanPrimaryGlow,
    onPrimaryContainer = CyanPrimary,
    secondary = NeonPurple,
    onSecondary = TextWhite,
    secondaryContainer = NeonPurpleGlow,
    onSecondaryContainer = NeonPurple,
    tertiary = NeonGreen,
    onTertiary = DarkVoid,
    background = DarkVoid,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = DarkCardBorder,
    error = NeonRed,
    onError = DarkVoid
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Dark Tunnel is strictly a Cyber Dark application
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
