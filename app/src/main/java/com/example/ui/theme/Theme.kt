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

private val StyleoraDarkColorScheme = darkColorScheme(
    primary = ChampagneGoldLight,
    onPrimary = MatteBlack,
    primaryContainer = Color(0xFF3E3118),
    onPrimaryContainer = ChampagneGoldLight,
    secondary = ChampagneGold,
    onSecondary = MatteBlack,
    secondaryContainer = EspressoSurface,
    onSecondaryContainer = SoftBeige,
    tertiary = WarmSand,
    onTertiary = MatteBlack,
    background = MatteBlack,
    onBackground = WarmWhite,
    surface = ObsidianCharcoal,
    onSurface = WarmWhite,
    surfaceVariant = EspressoSurface,
    onSurfaceVariant = Color(0xFFC8BFB2),
    outline = DarkOutline,
    outlineVariant = Color(0xFF2D2823),
    error = Color(0xFFEF5350),
    onError = MatteBlack
)

private val StyleoraLightColorScheme = lightColorScheme(
    primary = MatteBlack,
    onPrimary = ChampagneGoldLight,
    primaryContainer = SoftBeige,
    onPrimaryContainer = MatteBlack,
    secondary = ChampagneGoldMuted,
    onSecondary = WarmWhite,
    secondaryContainer = WarmSand,
    onSecondaryContainer = DeepBronzeInk,
    tertiary = ChampagneGold,
    onTertiary = MatteBlack,
    background = WarmWhite,
    onBackground = MatteBlack,
    surface = SoftBeige,
    onSurface = MatteBlack,
    surfaceVariant = WarmSand,
    onSurfaceVariant = MutedTaupe,
    outline = HairlineWarm,
    outlineVariant = Color(0xFFE9E2D6),
    error = StatusBookedRed,
    onError = WarmWhite
)

val StyleoraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun StyleoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StyleoraDarkColorScheme else StyleoraLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = StyleoraTypography,
        shapes = StyleoraShapes,
        content = content
    )
}
