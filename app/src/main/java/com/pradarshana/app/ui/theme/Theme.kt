package com.pradarshana.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFFB45A0C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB8),
    onPrimaryContainer = Color(0xFF3A1E00),
    secondary = Color(0xFF7E6318),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5E6B8),
    onSecondaryContainer = Color(0xFF2B2100),
    tertiary = Color(0xFF7A2E2E),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8EC),
    onBackground = Color(0xFF2E1F12),
    surface = Color(0xFFFFFBF3),
    onSurface = Color(0xFF2E1F12),
    surfaceVariant = Color(0xFFF4E7D3),
    onSurfaceVariant = Color(0xFF5A4630),
    surfaceContainer = Color(0xFFFBF0DF),
    surfaceContainerHigh = Color(0xFFF7EAD6),
    outline = Color(0xFF8F7A62),
    outlineVariant = Color(0xFFE0CDB2),
    error = Color(0xFFA8321F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB86B),
    onPrimary = Color(0xFF4A2600),
    primaryContainer = Color(0xFF6A3A08),
    onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Color(0xFFE2C46F),
    onSecondary = Color(0xFF3B2F00),
    secondaryContainer = Color(0xFF54440E),
    onSecondaryContainer = Color(0xFFF5E6B8),
    tertiary = Color(0xFFFFB3A8),
    onTertiary = Color(0xFF561D14),
    background = Color(0xFF1C140D),
    onBackground = Color(0xFFF2E6D6),
    surface = Color(0xFF221810),
    onSurface = Color(0xFFF2E6D6),
    surfaceVariant = Color(0xFF3A2D20),
    onSurfaceVariant = Color(0xFFD9C6AE),
    surfaceContainer = Color(0xFF2A1F15),
    surfaceContainerHigh = Color(0xFF33271B),
    outline = Color(0xFFA6927A),
    outlineVariant = Color(0xFF4F3F2E),
    error = Color(0xFFFFB4A6),
)

@Immutable
data class TempleColors(
    val gold: Color,
    val saffron: Color,
    val completed: Color,
    val incomplete: Color,
)

private val LightTemple = TempleColors(
    gold = Color(0xFFC9A227),
    saffron = Color(0xFFE07B17),
    completed = Color(0xFF3F6B2A),
    incomplete = Color(0xFF8A5A1C),
)

private val DarkTemple = TempleColors(
    gold = Color(0xFFD9B95B),
    saffron = Color(0xFFFFA24D),
    completed = Color(0xFFA9D18E),
    incomplete = Color(0xFFE8B678),
)

val LocalTempleColors = staticCompositionLocalOf { LightTemple }

private val base = Typography()

private val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 96.sp, lineHeight = 100.sp),
    displayMedium = base.displayMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
    titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
)

val CounterNumberStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 88.sp, lineHeight = 92.sp)

@Composable
fun PradarshanaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTempleColors provides if (darkTheme) DarkTemple else LightTemple,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
