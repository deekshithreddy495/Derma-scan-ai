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

private val DermaScanLightColors = lightColorScheme(
    primary = ClinicalTealPrimary,
    onPrimary = Color.White,
    primaryContainer = ClinicalTealContainer,
    onPrimaryContainer = ClinicalTealOnContainer,
    secondary = ClinicalSkySecondary,
    onSecondary = Color.White,
    secondaryContainer = ClinicalSkyContainer,
    onSecondaryContainer = ClinicalSkyOnContainer,
    tertiary = ClinicalIndigoTertiary,
    onTertiary = Color.White,
    tertiaryContainer = ClinicalIndigoContainer,
    onTertiaryContainer = ClinicalIndigoOnContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = AlertCoral,
    onError = Color.White,
    errorContainer = AlertCoralContainer,
    onErrorContainer = AlertCoral,
)

private val DermaScanDarkColors = darkColorScheme(
    primary = DarkTealPrimary,
    onPrimary = DarkTealOnPrimary,
    primaryContainer = DarkTealContainer,
    onPrimaryContainer = DarkTealOnContainer,
    secondary = DarkSkySecondary,
    onSecondary = DarkSkyOnSecondary,
    secondaryContainer = DarkSkyContainer,
    onSecondaryContainer = DarkSkyOnContainer,
    tertiary = ClinicalIndigoTertiary,
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFF0369A1),
    onTertiaryContainer = Color(0xFFE0F2FE),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = AlertCoralDarkContainer,
    onErrorContainer = Color(0xFFFEE2E2),
)

val DermaScanShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DermaScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DermaScanDarkColors else DermaScanLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DermaScanTypography,
        shapes = DermaScanShapes,
        content = content,
    )
}
