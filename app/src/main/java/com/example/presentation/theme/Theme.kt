package com.example.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemePackOption(val title: String) {
    HIJAU_SEGAR("Hijau Segar (Default)"),
    BIRU_LANGIT("Biru Langit"),
    KUNING_HANGAT("Kuning Hangat")
}

private val LightColorsEmerald = lightColorScheme(
    primary = Emerald600,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald700,
    secondary = Amber600,
    onSecondary = Color.White,
    secondaryContainer = Amber100,
    onSecondaryContainer = Amber600,
    tertiary = Sky600,
    onTertiary = Color.White,
    background = SurfaceLight,
    onBackground = TextDark,
    surface = CardLight,
    onSurface = TextDark,
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorsEmerald = darkColorScheme(
    primary = Emerald500,
    onPrimary = Color.Black,
    primaryContainer = Emerald700,
    onPrimaryContainer = Emerald100,
    secondary = Amber500,
    onSecondary = Color.Black,
    secondaryContainer = Amber600,
    onSecondaryContainer = Amber100,
    tertiary = Sky600,
    onTertiary = Color.White,
    background = SurfaceDark,
    onBackground = TextLight,
    surface = CardDark,
    onSurface = TextLight,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorsSky = lightColorScheme(
    primary = Sky600,
    onPrimary = Color.White,
    primaryContainer = Sky100,
    onPrimaryContainer = Sky600,
    secondary = Emerald600,
    onSecondary = Color.White,
    background = SurfaceLight,
    surface = CardLight
)

private val LightColorsAmber = lightColorScheme(
    primary = Amber600,
    onPrimary = Color.White,
    primaryContainer = Amber100,
    onPrimaryContainer = Amber600,
    secondary = Emerald600,
    onSecondary = Color.White,
    background = SurfaceLight,
    surface = CardLight
)

@Composable
fun OmahErpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePack: ThemePackOption = ThemePackOption.HIJAU_SEGAR,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorsEmerald
        themePack == ThemePackOption.BIRU_LANGIT -> LightColorsSky
        themePack == ThemePackOption.KUNING_HANGAT -> LightColorsAmber
        else -> LightColorsEmerald
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
