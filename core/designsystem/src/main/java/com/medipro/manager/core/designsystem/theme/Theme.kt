package com.medipro.manager.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF00695C)
private val PrimaryDark = Color(0xFF004D40)
private val Secondary = Color(0xFF00897B)
private val Error = Color(0xFFD32F2F)
private val BackgroundLight = Color(0xFFF5F7FA)
private val BackgroundDark = Color(0xFF121212)
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceDark = Color(0xFF1E1E1E)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = Secondary,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF1A1A1A),
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1A1A),
    error = Error,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Secondary,
    onPrimary = Color.Black,
    secondary = Primary,
    onSecondary = Color.White,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    error = Error,
    onError = Color.White
)

@Composable
fun MediProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MediProTypography,
        content = content
    )
}
