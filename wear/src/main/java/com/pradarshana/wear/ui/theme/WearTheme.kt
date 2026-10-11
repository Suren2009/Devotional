package com.pradarshana.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val Saffron = Color(0xFFFFA24D)
val Gold = Color(0xFFD9B95B)
val Cream = Color(0xFFF2E6D6)

private val WearColors = Colors(
    primary = Saffron,
    primaryVariant = Color(0xFFE07B17),
    secondary = Gold,
    secondaryVariant = Color(0xFFB8952E),
    background = Color.Black,
    surface = Color(0xFF2A1F15),
    error = Color(0xFFFFB4A6),
    onPrimary = Color(0xFF3A1E00),
    onSecondary = Color(0xFF2B2100),
    onBackground = Cream,
    onSurface = Cream,
    onSurfaceVariant = Color(0xFFD9C6AE),
    onError = Color(0xFF561D14),
)

/** Watches are always dark; OLED-friendly black background with warm accents. */
@Composable
fun WearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = WearColors, content = content)
}
