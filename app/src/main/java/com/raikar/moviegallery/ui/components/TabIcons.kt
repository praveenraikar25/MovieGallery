package com.raikar.moviegallery.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import com.raikar.moviegallery.ui.components.TabMotion.easeOutBack
import com.raikar.moviegallery.ui.components.TabMotion.phase
import com.raikar.moviegallery.ui.components.TabMotion.pulse

private const val GRID = 24f
private const val STROKE = 1.8f

/**
 * One tab's icon, drawn on a 24-unit grid. [progress] runs 0..1 over the selection animation and 1 means the
 * resting state; it is a lambda so only the draw phase reads it. [fill] is the soft tint of the selected state.
 */
@Composable
fun TabIcon(
    tab: TabItem,
    progress: () -> Float,
    fill: () -> Float,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        scale(scale = this.size.width / GRID, pivot = Offset.Zero) {
            val p = progress()
            val f = fill()
            when (tab) {
                TabItem.Home -> drawHome(p, f, color)
                TabItem.Search -> drawSearch(p, f, color)
                TabItem.Watchlist -> drawWatchlist(p, f, color)
                TabItem.Profile -> drawProfile(p, f, color)
            }
        }
    }
}

private val line = Stroke(width = STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun Path.trimmed(fraction: Float): Path {
    val measure = PathMeasure().also { it.setPath(this, false) }
    return Path().also { measure.getSegment(0f, measure.length * fraction, it, true) }
}

private fun DrawScope.drawTrimmed(
    path: Path,
    fraction: Float,
    color: Color,
) {
    if (fraction > 0f) drawPath(path.trimmed(fraction), color, style = line)
}

private val roofPath =
    Path().apply {
        moveTo(3f, 11.5f)
        lineTo(12f, 3.5f)
        lineTo(21f, 11.5f)
    }
private val wallsPath =
    Path().apply {
        moveTo(5.5f, 10.5f)
        lineTo(5.5f, 20f)
        lineTo(18.5f, 20f)
        lineTo(18.5f, 10.5f)
    }
private val houseFill =
    Path().apply {
        moveTo(5f, 10.5f)
        lineTo(12f, 4f)
        lineTo(19f, 10.5f)
        lineTo(19f, 20f)
        lineTo(5f, 20f)
        close()
    }

private fun DrawScope.drawHome(
    progress: Float,
    fill: Float,
    color: Color,
) {
    val wobble = TabMotion.wobble(progress)
    scale(scaleX = 1f + wobble, scaleY = 1f - wobble, pivot = Offset(12f, 20f)) {
        drawPath(houseFill, color.copy(alpha = fill))
        drawTrimmed(roofPath, phase(progress, TabMotion.HomeRoof), color)
        drawTrimmed(wallsPath, phase(progress, TabMotion.HomeWalls), color)
        val door = easeOutBack(phase(progress, TabMotion.HomeDoor))
        scale(scaleX = 1f, scaleY = door, pivot = Offset(12f, 20f)) {
            drawRect(color, Offset(10f, 15f), Size(4f, 5f), style = line)
        }
    }
    val smoke = phase(progress, TabMotion.HomeSmoke)
    if (smoke > 0f && smoke < 1f) {
        translate(top = -9f * smoke) {
            scale(0.5f + 0.8f * smoke, pivot = Offset(17f, 4f)) {
                drawCircle(color.copy(alpha = 0.9f * pulse(smoke)), radius = 1.2f, center = Offset(17f, 4f))
            }
        }
    }
}

private val handlePath =
    Path().apply {
        moveTo(15.2f, 15.2f)
        lineTo(20.5f, 20.5f)
    }

private fun DrawScope.drawSearch(
    progress: Float,
    fill: Float,
    color: Color,
) {
    val swing = easeOutBack(progress)
    rotate(degrees = -150f * (1f - swing), pivot = Offset(12f, 12f)) {
        scale(0.7f + 0.3f * swing, pivot = Offset(12f, 12f)) {
            val center = Offset(10.5f, 10.5f)
            drawCircle(color.copy(alpha = fill), radius = 6.2f, center = center)
            drawCircle(color, radius = 6.2f, center = center, style = line)
            drawTrimmed(handlePath, phase(progress, TabMotion.SearchHandle), color)
            val glint = phase(progress, TabMotion.SearchGlint)
            if (glint in 0.001f..0.999f) {
                drawArc(
                    color = color.copy(alpha = pulse(glint)),
                    startAngle = 200f + 100f * glint,
                    sweepAngle = 50f,
                    useCenter = false,
                    topLeft = Offset(10.5f - 4f, 10.5f - 4f),
                    size = Size(8f, 8f),
                    style = Stroke(width = 1.6f, cap = StrokeCap.Round),
                )
            }
        }
    }
}

private val ribbonPath =
    Path().apply {
        moveTo(6.5f, 3.5f)
        lineTo(17.5f, 3.5f)
        lineTo(17.5f, 20.5f)
        lineTo(12f, 16.8f)
        lineTo(6.5f, 20.5f)
        close()
    }

private val sparks =
    listOf(
        Triple(Offset(6f, 3f), Offset(4.5f, 1.5f), Offset(-9f, -8f)),
        Triple(Offset(18f, 3f), Offset(19.5f, 1.5f), Offset(9f, -8f)),
        Triple(Offset(12f, 2.5f), Offset(12f, 1f), Offset(0f, -12f)),
    )

private fun DrawScope.drawWatchlist(
    progress: Float,
    fill: Float,
    color: Color,
) {
    val drop = easeOutBack(phase(progress, TabMotion.WatchlistDrop))
    val alpha = phase(progress, TabMotion.Range(0f, 0.3f))
    translate(top = -12f * (1f - drop)) {
        val fillTo = 3.5f + 17f * phase(progress, TabMotion.WatchlistFill)
        clipRect(0f, 0f, GRID, fillTo) { drawPath(ribbonPath, color.copy(alpha = fill)) }
        drawPath(ribbonPath, color.copy(alpha = alpha), style = line)
    }
    val burst = phase(progress, TabMotion.WatchlistSparks)
    if (burst > 0f && burst < 1f) {
        sparks.forEach { (from, to, travel) ->
            translate(left = travel.x * burst, top = travel.y * burst) {
                val path =
                    Path().apply {
                        moveTo(from.x, from.y)
                        lineTo(to.x, to.y)
                    }
                drawPath(path, color.copy(alpha = pulse(burst)), style = line)
            }
        }
    }
}

private val shouldersPath =
    Path().apply {
        moveTo(5.8f, 18.6f)
        cubicTo(7f, 16f, 9.4f, 14.8f, 12f, 14.8f)
        cubicTo(14.6f, 14.8f, 17f, 16f, 18.2f, 18.6f)
    }
private val ringPath = Path().apply { addOval(Rect(Offset(12f - 9.5f, 12f - 9.5f), Size(19f, 19f))) }

private fun DrawScope.drawProfile(
    progress: Float,
    fill: Float,
    color: Color,
) {
    val center = Offset(12f, 12f)
    drawCircle(color.copy(alpha = fill), radius = 9.5f, center = center)
    drawTrimmed(ringPath, phase(progress, TabMotion.ProfileRing), color)
    val body = easeOutBack(phase(progress, TabMotion.ProfileBody))
    translate(top = 8f * (1f - body)) {
        drawPath(shouldersPath, color.copy(alpha = phase(progress, TabMotion.ProfileBody)), style = line)
    }
    val head = easeOutBack(phase(progress, TabMotion.ProfileHead))
    translate(top = 5f * (1f - head)) {
        scale(head, pivot = Offset(12f, 12.8f)) {
            drawCircle(color, radius = 3.2f, center = Offset(12f, 9.6f), style = line)
        }
    }
    val ripple = phase(progress, TabMotion.ProfileRipple)
    if (ripple > 0f && ripple < 1f) {
        scale(1f + 0.5f * ripple, pivot = center) {
            drawCircle(color.copy(alpha = 0.7f * (1f - ripple)), radius = 10f, center = center, style = Stroke(1.2f))
        }
    }
}
