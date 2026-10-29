package com.raikar.moviegallery.ui.components

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Pure geometry and timing for the film reel spinner; no Android or Compose imports. */
object FilmReelMotion {
    const val TURN_MS = 1600
    const val HOLE_COUNT = 6

    // All radii are fractions of the reel's overall radius.
    const val RING_RADIUS = 0.92f
    const val RING_STROKE = 0.12f
    const val HOLE_ORBIT = 0.56f
    const val HOLE_RADIUS = 0.17f
    const val HUB_RADIUS = 0.16f

    /** Position of a hole centre relative to the reel centre, as fractions of the reel radius. */
    data class Point(
        val x: Float,
        val y: Float,
    )

    /** Angle of hole [index] in degrees, measured clockwise from straight up. */
    fun holeAngleDegrees(index: Int): Float = index * (360f / HOLE_COUNT)

    fun holeCentre(index: Int): Point {
        val radians = holeAngleDegrees(index) * PI.toFloat() / 180f
        return Point(x = HOLE_ORBIT * sin(radians), y = -HOLE_ORBIT * cos(radians))
    }
}
