package com.shortvideo.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * App chrome is always dark (black background + light foreground).
 * "Light" mode only lifts surfaces slightly; it never switches to a white shell.
 */
private val NightColors = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = PrimaryColor,
    onSecondary = Color.White,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = SubTextColor,
    outline = Color(0xFF3A3A3A),
    error = Color(0xFFFF5252),
    onError = Color.White,
)

private val LightColors = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = PrimaryColor,
    onSecondary = Color.White,
    background = Color(0xFF0A0A0A),
    onBackground = White,
    surface = SurfaceElevated,
    onSurface = White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = SubTextColor,
    outline = Color(0xFF4A4A4A),
    error = Color(0xFFFF5252),
    onError = Color.White,
)

@Composable
fun ShortVideoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NightColors else LightColors,
        content = content,
    )
}
