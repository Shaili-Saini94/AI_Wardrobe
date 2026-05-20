package com.ai.wardrobe.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ClosetLightColorScheme = lightColorScheme(
    primary            = ClosetBlack,
    onPrimary          = Color.White,
    primaryContainer   = ClosetStatsBg,
    onPrimaryContainer = ClosetBlack,
    secondary          = ClosetSecondary,
    onSecondary        = Color.White,
    secondaryContainer = ClosetSecondaryBtn,
    onSecondaryContainer = ClosetBlack,
    tertiary           = ClosetGold,
    onTertiary         = Color.White,
    background         = ClosetBackground,
    onBackground       = ClosetBlack,
    surface            = ClosetBackground,
    onSurface          = ClosetBlack,
    surfaceVariant     = ClosetStatsBg,
    onSurfaceVariant   = ClosetSecondary,
    outline            = ClosetBorder,
    error              = ErrorLight,
    onError            = OnErrorLight,
)

private val ClosetDarkColorScheme = darkColorScheme(
    primary            = PrimaryDark,
    onPrimary          = ClosetBlack,
    primaryContainer   = PrimaryContainerDark,
    onPrimaryContainer = PrimaryDark,
    secondary          = SecondaryDark,
    onSecondary        = ClosetBlack,
    background         = BackgroundDark,
    onBackground       = OnBackgroundDark,
    surface            = SurfaceDark,
    onSurface          = OnSurfaceDark,
    error              = ErrorDark,
    onError            = OnErrorDark,
)

@Composable
fun AIWardrobeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ClosetDarkColorScheme else ClosetLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
