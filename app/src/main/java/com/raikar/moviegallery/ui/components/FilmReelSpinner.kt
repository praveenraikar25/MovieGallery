package com.raikar.moviegallery.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.raikar.moviegallery.ui.theme.Sizes

/** A ring with six holes around a hub that turns steadily; still when system animations are off. */
@Composable
fun FilmReelSpinner(
    modifier: Modifier = Modifier,
    size: Dp = Sizes.ReelSpinner,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val resolver = LocalContext.current.contentResolver
    val animate =
        remember {
            TabMotion.animationsEnabled(
                Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f),
            )
        }
    val rotation =
        if (animate) {
            val transition = rememberInfiniteTransition(label = "filmReel")
            val turn by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = FilmReelMotion.TURN_MS, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "filmReelTurn",
            )
            turn
        } else {
            0f
        }

    Canvas(
        modifier =
            modifier
                .size(size)
                .semantics { contentDescription = "Loading" },
    ) {
        val radius = this.size.minDimension / 2f
        rotate(degrees = rotation) {
            drawCircle(
                color = color,
                radius = radius * FilmReelMotion.RING_RADIUS,
                style = Stroke(width = radius * FilmReelMotion.RING_STROKE),
            )
            drawCircle(color = color, radius = radius * FilmReelMotion.HUB_RADIUS)
            repeat(FilmReelMotion.HOLE_COUNT) { index ->
                val hole = FilmReelMotion.holeCentre(index)
                drawCircle(
                    color = color,
                    radius = radius * FilmReelMotion.HOLE_RADIUS,
                    center = Offset(center.x + hole.x * radius, center.y + hole.y * radius),
                )
            }
        }
    }
}
