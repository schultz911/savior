package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BentoLavenderCard,
    onPrimary = BentoPurpleDark,
    primaryContainer = BentoPurplePrimary,
    onPrimaryContainer = BentoLavenderContainer,
    secondary = BentoLavenderLight,
    onSecondary = BentoPurpleDark,
    secondaryContainer = BentoSurfaceVariantDark,
    onSecondaryContainer = BentoLavenderContainer,
    tertiary = BentoSpendPlum,
    onTertiary = Color.White,
    background = BentoBackgroundDark,
    onBackground = BentoTextDarkPrimary,
    surface = BentoSurfaceDark,
    onSurface = BentoTextDarkPrimary,
    surfaceVariant = BentoSurfaceVariantDark,
    onSurfaceVariant = BentoTextDarkSecondary,
    outline = BentoCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = BentoPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = BentoLavenderContainer,
    onPrimaryContainer = BentoPurpleDark,
    secondary = BentoPurplePrimary,
    onSecondary = Color.White,
    secondaryContainer = BentoLavenderLight,
    onSecondaryContainer = BentoPurpleDark,
    tertiary = BentoSpendPlum,
    onTertiary = Color.White,
    background = BentoBackgroundLight,
    onBackground = BentoTextPrimary,
    surface = BentoSurfaceLight,
    onSurface = BentoTextPrimary,
    surfaceVariant = BentoCardBg,
    onSurfaceVariant = BentoTextSecondary,
    outline = BentoBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use intentional bespoke palette by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
