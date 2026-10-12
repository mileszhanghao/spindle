package com.mileszhang.spindle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Amber = Color(0xFFF5A524)
val Vinyl = Color(0xFF121212)
val Groove = Color(0xFF1E1E1E)

private val colors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF1A1200),
    secondary = Color(0xFF8AB4F8),
    background = Vinyl,
    onBackground = Color(0xFFEDEDED),
    surface = Vinyl,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Groove,
    onSurfaceVariant = Color(0xFFB3B3B3),
    surfaceContainer = Color(0xFF181818),
)

private val base = Typography()
private val typography = base.copy(
    displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
)

/** One app theme. The course wrapped every screen in its own MaterialTheme(darkColors()). */
@Composable
fun SpindleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
