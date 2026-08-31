package com.incarail.checkinbimodal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta alineada al prototipo web presentado al equipo (verde-azulado "petróleo" de marca).
val AccentLight = Color(0xFF0B5A5E)
val AccentDark = Color(0xFF49C9A8)

val GoodLight = Color(0xFF1F7A44)
val GoodDark = Color(0xFF49C98A)
val CriticalLight = Color(0xFFB3261E)
val CriticalDark = Color(0xFFF0837A)
val WarnLight = Color(0xFF9A6B00)
val WarnDark = Color(0xFFE0B04A)

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color.White,
    background = Color(0xFFF5F3EC),
    surface = Color.White,
    surfaceVariant = Color(0xFFEFECE2),
    error = CriticalLight,
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF06211C),
    background = Color(0xFF0C1512),
    surface = Color(0xFF16211C),
    surfaceVariant = Color(0xFF1C2822),
    error = CriticalDark,
)

@Composable
fun CheckinBimodalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}

// Colores de estado (semánticos), independientes del tema claro/oscuro activo.
object StatusColors {
    val good: Color @Composable get() = if (isSystemInDarkTheme()) GoodDark else GoodLight
    val critical: Color @Composable get() = if (isSystemInDarkTheme()) CriticalDark else CriticalLight
    val warn: Color @Composable get() = if (isSystemInDarkTheme()) WarnDark else WarnLight
}
