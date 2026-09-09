package com.raikar.moviegallery.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors =
    lightColorScheme(
        primary = LightAccent,
        onPrimary = LightAccentText,
        secondary = LightAccent,
        onSecondary = LightAccentText,
        tertiary = LightAccent,
        onTertiary = LightAccentText,
        background = LightBg,
        onBackground = LightText,
        surface = LightSurface,
        onSurface = LightText,
        surfaceVariant = LightSurfaceAlt,
        onSurfaceVariant = LightTextMuted,
        outline = LightBorder,
        outlineVariant = LightBorder,
        error = LightError,
        onError = LightAccentText,
    )

private val DarkColors =
    darkColorScheme(
        primary = DarkAccent,
        onPrimary = DarkAccentText,
        secondary = DarkAccent,
        onSecondary = DarkAccentText,
        tertiary = DarkAccent,
        onTertiary = DarkAccentText,
        background = DarkBg,
        onBackground = DarkText,
        surface = DarkSurface,
        onSurface = DarkText,
        surfaceVariant = DarkSurfaceAlt,
        onSurfaceVariant = DarkTextMuted,
        outline = DarkBorder,
        outlineVariant = DarkBorder,
        error = DarkError,
        onError = DarkAccentText,
    )

@Composable
fun MovieGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val movieColors = if (darkTheme) DarkMovieColors else LightMovieColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalMovieColors provides movieColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MovieTypography,
            shapes = MovieShapes,
            content = content,
        )
    }
}
