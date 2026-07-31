package com.shortvideo.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = PrimaryColor,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = PrimaryColor,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
)

@Composable
fun ShortVideoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
