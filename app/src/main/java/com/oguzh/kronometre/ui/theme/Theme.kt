package com.oguzh.kronometre.ui.theme

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

private val LightColors = lightColorScheme(
    primary = StartGreen,
    error = PauseRed,
    background = Neutral95,
    surface = Neutral95,
)

private val DarkColors = darkColorScheme(
    primary = StartGreenDark,
    error = PauseRedDark,
    background = Neutral10,
    surface = Neutral10,
)

data class ActionColors(val start: Color, val pause: Color)

@Composable
fun actionColors(): ActionColors =
    if (isSystemInDarkTheme()) ActionColors(StartGreenDark, PauseRedDark) else ActionColors(StartGreen, PauseRed)

@Composable
fun KronometreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
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
