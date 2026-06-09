package com.example.unisync.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UniSyncColorScheme = lightColorScheme(
    primary = LavenderPrimary,
    onPrimary = Color.White,
    primaryContainer = LavenderLight,
    onPrimaryContainer = LavenderDark,
    secondary = PinkAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8E8FF),
    onSecondaryContainer = PurpleAccent,
    tertiary = PurpleAccent,
    onTertiary = Color.White,
    background = LavenderBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = LavenderSurface,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = LostRed,
    onError = Color.White,
)

@Composable
fun UniSyncTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = UniSyncColorScheme,
        typography = UniSyncTypography,
        content = content
    )
}
