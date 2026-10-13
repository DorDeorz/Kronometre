package com.dordeorz.kronometre.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 48.sp, fontFeatureSettings = "tnum"),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, fontFeatureSettings = "tnum"),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, fontFeatureSettings = "tnum"),
    bodyLarge = TextStyle(fontSize = 16.sp, fontFeatureSettings = "tnum"),
)
