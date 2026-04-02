package com.downloader.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme = darkColorScheme(
    primary = MonochromeContent,
    onPrimary = MonochromeBackground,
    primaryContainer = MonochromeContent,
    onPrimaryContainer = MonochromeBackground,
    secondary = MonochromeContent,
    onSecondary = MonochromeBackground,
    background = MonochromeBackground,
    onBackground = MonochromeContent,
    surface = MonochromeBackground, // Using same background as requested
    onSurface = MonochromeContent,
    surfaceVariant = MonochromeBackground,
    onSurfaceVariant = MonochromeContent,
    error = MonochromeContent, // We just use the 2 colors
    onError = MonochromeBackground
)

@Composable
fun DownloaderTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = MonochromeBackground.toArgb()
            window.navigationBarColor = MonochromeBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
