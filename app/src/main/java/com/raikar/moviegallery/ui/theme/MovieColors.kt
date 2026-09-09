package com.raikar.moviegallery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class MovieColors(
    val surfaceAlt: Color,
    val textMuted: Color,
    val border: Color,
    val inactiveTab: Color,
)

val LightMovieColors =
    MovieColors(
        surfaceAlt = LightSurfaceAlt,
        textMuted = LightTextMuted,
        border = LightBorder,
        inactiveTab = LightInactiveTab,
    )

val DarkMovieColors =
    MovieColors(
        surfaceAlt = DarkSurfaceAlt,
        textMuted = DarkTextMuted,
        border = DarkBorder,
        inactiveTab = DarkInactiveTab,
    )

val LocalMovieColors = staticCompositionLocalOf { LightMovieColors }

val MaterialTheme.movieColors: MovieColors
    @Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = LocalMovieColors.current
