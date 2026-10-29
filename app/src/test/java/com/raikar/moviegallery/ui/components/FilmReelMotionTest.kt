package com.raikar.moviegallery.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class FilmReelMotionTest {
    @Test
    fun `there are six holes sixty degrees apart`() {
        val angles = (0 until FilmReelMotion.HOLE_COUNT).map { FilmReelMotion.holeAngleDegrees(it) }
        assertEquals(listOf(0f, 60f, 120f, 180f, 240f, 300f), angles)
    }

    @Test
    fun `every hole sits on the same orbit and inside the ring`() {
        repeat(FilmReelMotion.HOLE_COUNT) { index ->
            val hole = FilmReelMotion.holeCentre(index)
            assertEquals(FilmReelMotion.HOLE_ORBIT, hypot(hole.x, hole.y), 0.0001f)
            val outer = FilmReelMotion.HOLE_ORBIT + FilmReelMotion.HOLE_RADIUS
            assertTrue(outer < FilmReelMotion.RING_RADIUS - FilmReelMotion.RING_STROKE / 2f)
        }
    }

    @Test
    fun `holes clear the hub and each other`() {
        assertTrue(FilmReelMotion.HOLE_ORBIT - FilmReelMotion.HOLE_RADIUS > FilmReelMotion.HUB_RADIUS)
        val a = FilmReelMotion.holeCentre(0)
        val b = FilmReelMotion.holeCentre(1)
        assertTrue(hypot(a.x - b.x, a.y - b.y) > 2 * FilmReelMotion.HOLE_RADIUS)
    }

    @Test
    fun `first hole is straight up`() {
        val top = FilmReelMotion.holeCentre(0)
        assertEquals(0f, top.x, 0.0001f)
        assertEquals(-FilmReelMotion.HOLE_ORBIT, top.y, 0.0001f)
    }

    @Test
    fun `one turn takes 1600 ms`() {
        assertEquals(1600, FilmReelMotion.TURN_MS)
    }
}
