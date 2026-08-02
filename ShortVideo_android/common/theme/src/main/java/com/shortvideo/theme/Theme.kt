package com.shortvideo.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Night: black shell + light foreground. */
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

/** Light: white shell + dark foreground. */
private val LightColors = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = PrimaryColor,
    onSecondary = Color.White,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF6B6B6B),
    outline = Color(0xFFD0D0D0),
    error = Color(0xFFB00020),
    onError = Color.White,
)

@Composable
fun ShortVideoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) NightColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
