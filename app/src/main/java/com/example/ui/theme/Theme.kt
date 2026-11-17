package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Strict Light-Only ColorScheme — never follows Android system dark mode
private val AzadLightColorScheme = lightColorScheme(
    primary = TailorIndigo,
    onPrimary = Color.White,
    primaryContainer = TailorIndigoContainer,
    onPrimaryContainer = TailorIndigoDark,
    secondary = BrassGold,
    onSecondary = Color.White,
    secondaryContainer = BrassGoldContainer,
    onSecondaryContainer = BrassGoldDark,
    tertiary = CraftSage,
    onTertiary = Color.White,
    tertiaryContainer = CraftSageContainer,
    onTertiaryContainer = CraftSageDark,
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = AlertCrimsonDark,
    background = AtelierBackground,
    onBackground = TextPrimary,
    surface = AtelierSurface,
    onSurface = TextPrimary,
    surfaceVariant = AtelierSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = AtelierSurface,
    surfaceContainerLow = AtelierBackground,
    surfaceContainer = AtelierSurfaceContainer,
    surfaceContainerHigh = AtelierSurfaceVariant,
    outline = OutlineWarm,
    outlineVariant = OutlineSubtle
)

private val AzadShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AzadTaillorsTheme(
    isSindhi: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AzadLightColorScheme,
        typography = createAppTypography(isSindhi = isSindhi),
        shapes = AzadShapes,
        content = content
    )
}
