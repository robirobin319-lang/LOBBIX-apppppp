package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LobbixDarkColorScheme = darkColorScheme(
    primary = CrimsonRed,
    onPrimary = CrispWhite,
    primaryContainer = CrimsonDark,
    onPrimaryContainer = CrispWhite,
    secondary = NeonScarlet,
    onSecondary = MatteBlack,
    secondaryContainer = GunmetalElevated,
    onSecondaryContainer = CrispWhite,
    tertiary = GoldTrophy,
    onTertiary = MatteBlack,
    background = MatteBlack,
    onBackground = CrispWhite,
    surface = CarbonSurface,
    onSurface = CrispWhite,
    surfaceVariant = GunmetalCard,
    onSurfaceVariant = CoolSlate,
    surfaceContainerLow = CarbonSurface,
    surfaceContainer = GunmetalCard,
    surfaceContainerHigh = GunmetalElevated,
    outline = SteelBorder,
    outlineVariant = GunmetalElevated,
    error = NeonScarlet,
    onError = MatteBlack
)

val LobbixShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun LobbixTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LobbixDarkColorScheme,
        typography = LobbixTypography,
        shapes = LobbixShapes,
        content = content
    )
}
