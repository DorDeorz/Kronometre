package com.dordeorz.kronometre.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LightColors = lightColorScheme(
    primary = Green,
    primaryContainer = GreenContainer,
    onPrimaryContainer = Color(0xFF0B2E0E),
    error = PauseRed,
    background = Neutral95,
    surface = Neutral95,
    surfaceContainer = Neutral90,
    surfaceContainerHighest = Color(0xFFDCDDD7),
)

val DarkColors = darkColorScheme(
    primary = GreenDark,
    primaryContainer = GreenContainerDark,
    onPrimaryContainer = GreenContainer,
    error = PauseRedDark,
    background = Neutral10,
    surface = Neutral10,
    surfaceContainer = Neutral17,
    surfaceContainerHighest = Color(0xFF33352F),
)

@Composable
fun KronometreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
