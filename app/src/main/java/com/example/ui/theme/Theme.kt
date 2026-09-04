package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Mode Only ColorScheme tailored for Glassmorphism
private val LightGlassmorphicColorScheme = lightColorScheme(
    primary = SavioEmerald,
    onPrimary = Color.White,
    primaryContainer = SavioEmeraldContainer,
    onPrimaryContainer = SavioEmerald,
    secondary = SavioTransferIndigo,
    onSecondary = Color.White,
    secondaryContainer = SavioTransferIndigoBg,
    onSecondaryContainer = SavioTransferIndigo,
    tertiary = SavioSpendRose,
    onTertiary = Color.White,
    tertiaryContainer = SavioSpendRoseBg,
    onTertiaryContainer = SavioSpendRose,
    background = GlassBackground,
    onBackground = SavioSlateDark,
    surface = GlassSurface,
    onSurface = SavioSlateDark,
    surfaceVariant = GlassCardBg,
    onSurfaceVariant = SavioSlateBody,
    outline = GlassCardBorder,
    outlineVariant = GlassCardBorderSubtle
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Forced Light Mode Only
    dynamicColor: Boolean = false, // Intentional bespoke glassmorphism palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightGlassmorphicColorScheme,
        typography = Typography,
        content = content
    )
}
