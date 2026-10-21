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
    onPrimary = OnGreen,
    primaryContainer = GreenContainer,
    onPrimaryContainer = OnGreenContainer,
    inversePrimary = GreenDark,
    secondary = Color(0xFF506350),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8CC),
    onSecondaryContainer = Color(0xFF0D1F0E),
    tertiary = Color(0xFF3D6470),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB9EBFA),
    onTertiaryContainer = Color(0xFF001F28),
    error = PauseRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Neutral95,
    onBackground = OnNeutral,
    surface = Neutral95,
    onSurface = OnNeutral,
    surfaceVariant = Color(0xFFE1E3DB),
    onSurfaceVariant = Color(0xFF44483F),
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2F312D),
    inverseOnSurface = Color(0xFFF0F1EB),
    surfaceDim = Color(0xFFD4D6CE),
    surfaceBright = Color(0xFFF8F8F4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFECEDE7),
    surfaceContainer = Neutral90,
    surfaceContainerHigh = Color(0xFFE0E1DB),
    surfaceContainerHighest = Color(0xFFDADCD4),
)

val DarkColors = darkColorScheme(
    primary = GreenDark,
    onPrimary = OnGreenDark,
    primaryContainer = GreenContainerDark,
    onPrimaryContainer = OnGreenContainerDark,
    inversePrimary = Green,
    secondary = Color(0xFFA6CFA8),
    onSecondary = Color(0xFF10391A),
    secondaryContainer = Color(0xFF2A4A2D),
    onSecondaryContainer = Color(0xFFCCE8CC),
    tertiary = Color(0xFF9ED0DF),
    onTertiary = Color(0xFF003641),
    tertiaryContainer = Color(0xFF1E4E5A),
    onTertiaryContainer = Color(0xFFB9EBFA),
    error = PauseRedDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Neutral10,
    onBackground = OnNeutralDark,
    surface = Neutral10,
    onSurface = OnNeutralDark,
    surfaceVariant = Color(0xFF44483F),
    onSurfaceVariant = Color(0xFFC5C8BE),
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E4DE),
    inverseOnSurface = Color(0xFF30312D),
    surfaceDim = Color(0xFF1B1C1A),
    surfaceBright = Color(0xFF3A3C37),
    surfaceContainerLowest = Color(0xFF101210),
    surfaceContainerLow = Color(0xFF1B1C1A),
    surfaceContainer = Neutral17,
    surfaceContainerHigh = Color(0xFF2C2E2A),
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
