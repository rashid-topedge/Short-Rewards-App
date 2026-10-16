package com.dramflix.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandVioletLight,
    onPrimary = Color(0xFF0F0B1E),
    primaryContainer = BrandVioletDark,
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = BrandRose,
    onSecondary = Color.White,
    tertiary = CoinGold,
    onTertiary = Color(0xFF1E1500),
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder,
    outlineVariant = DarkBorderHighlight,
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun DramaFlixTheme(
    darkTheme: Boolean = true, // Default to stunning dark theme for streaming
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
