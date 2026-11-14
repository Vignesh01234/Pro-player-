package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkSportsColorScheme = darkColorScheme(
    primary = CourtGreenLight,
    onPrimary = Color(0xFF002114),
    primaryContainer = CourtGreenPrimary,
    onPrimaryContainer = Color(0xFFD7FBE8),
    secondary = ShuttleGoldAccent,
    onSecondary = Color(0xFF2B1700),
    secondaryContainer = Color(0xFF523500),
    onSecondaryContainer = ShuttleGoldLight,
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF00263A),
    background = ArenaBackgroundDark,
    onBackground = Color(0xFFE5F2EC),
    surface = ArenaSurfaceDark,
    onSurface = Color(0xFFE5F2EC),
    surfaceVariant = ArenaSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFB4CCC1),
    error = StatusPendingRed,
    onError = Color.White
)

private val LightSportsColorScheme = lightColorScheme(
    primary = CourtGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = CourtGreenContainerLight,
    onPrimaryContainer = OnCourtGreenContainerLight,
    secondary = ShuttleGoldDark,
    onSecondary = Color.White,
    secondaryContainer = ShuttleGoldContainerLight,
    onSecondaryContainer = OnShuttleGoldContainerLight,
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = ArenaBackgroundLight,
    onBackground = Color(0xFF0F211B),
    surface = ArenaSurfaceLight,
    onSurface = Color(0xFF0F211B),
    surfaceVariant = ArenaSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF3D564C),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ShuttleProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkSportsColorScheme else LightSportsColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
