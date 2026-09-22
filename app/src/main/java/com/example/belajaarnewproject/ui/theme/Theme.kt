package com.example.belajaarnewproject.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RedPrimary,
    onPrimary = TextWhite,
    primaryContainer = RedContainer,
    onPrimaryContainer = TextWhite,
    secondary = RedLight,
    onSecondary = TextWhite,
    tertiary = RedDark,
    background = BlackBackground,
    onBackground = TextWhite,
    surface = BlackSurface,
    onSurface = TextWhite,
    surfaceVariant = BlackSurfaceVariant,
    onSurfaceVariant = TextGrey,
    outline = RedDark,
    error = RedLight
)

private val LightColorScheme = darkColorScheme(
    primary = RedPrimary,
    onPrimary = TextWhite,
    primaryContainer = RedContainer,
    onPrimaryContainer = TextWhite,
    secondary = RedLight,
    onSecondary = TextWhite,
    tertiary = RedDark,
    background = BlackBackground,
    onBackground = TextWhite,
    surface = BlackSurface,
    onSurface = TextWhite,
    surfaceVariant = BlackSurfaceVariant,
    onSurfaceVariant = TextGrey,
    outline = RedDark,
    error = RedLight

    /* Tema dikunci merah-hitam baik mode terang maupun gelap */
)

@Composable
fun ExternalAndroidByRegsxdTheme(
    darkTheme: Boolean = true,
    // Kunci merah-hitam: dynamic color dimatikan
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}