package com.dramflix.android.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BrandViolet = Color(0xFF8B5CF6)
val BrandVioletLight = Color(0xFFA78BFA)
val BrandVioletDark = Color(0xFF6D28D9)

val BrandRose = Color(0xFFEC4899)
val BrandRoseLight = Color(0xFFF472B6)

val CoinGold = Color(0xFFFFB800)
val CoinGoldLight = Color(0xFFFFD54F)
val CoinGoldDark = Color(0xFFFF8F00)

val DarkBackground = Color(0xFF0B0E14)
val DarkSurface = Color(0xFF131822)
val DarkSurfaceVariant = Color(0xFF1A2232)
val DarkSurfaceCard = Color(0xFF161D2B)
val DarkBorder = Color(0x24FFFFFF)
val DarkBorderHighlight = Color(0x408B5CF6)

val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

val SuccessGreen = Color(0xFF10B981)
val ErrorRed = Color(0xFFEF4444)

val BrandGradient = Brush.horizontalGradient(
    colors = listOf(BrandViolet, BrandRose)
)

val BrandGradientVertical = Brush.verticalGradient(
    colors = listOf(BrandViolet, BrandRose)
)

val GoldGradient = Brush.horizontalGradient(
    colors = listOf(CoinGoldLight, CoinGoldDark)
)

val GoldPillGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF2A200A), Color(0xFF3B2D0E))
)

val CardGlassGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1F293D).copy(alpha = 0.85f),
        Color(0xFF121824).copy(alpha = 0.95f)
    )
)
