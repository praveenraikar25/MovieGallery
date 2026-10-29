package com.raikar.moviegallery.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabMotionTest {
    @Test
    fun `animations are disabled when the system animator scale is zero`() {
        assertFalse(TabMotion.animationsEnabled(0f))
    }

    @Test
    fun `animations are enabled for any positive animator scale`() {
        assertTrue(TabMotion.animationsEnabled(1f))
        assertTrue(TabMotion.animationsEnabled(0.5f))
    }

    @Test
    fun `pill sits behind each of the four tabs`() {
        val offsets = (0 until TabMotion.TAB_COUNT).map { TabMotion.pillOffset(it, tabWidth = 90f) }
        assertEquals(listOf(0f, 90f, 180f, 270f), offsets)
    }

    @Test
    fun `pill offset stays within the bar for out of range indexes`() {
        assertEquals(0f, TabMotion.pillOffset(-1, 90f), 0f)
        assertEquals(270f, TabMotion.pillOffset(9, 90f), 0f)
    }

    @Test
    fun `every phase range is ordered and within 0 to 1`() {
        TabMotion.allRanges.forEach {
            assertTrue(it.start >= 0f && it.end <= 1f && it.start < it.end)
        }
    }

    @Test
    fun `phase is clamped before and after its range`() {
        val range = TabMotion.Range(0.2f, 0.6f)
        assertEquals(0f, TabMotion.phase(0f, range), 0f)
        assertEquals(0.5f, TabMotion.phase(0.4f, range), 1e-6f)
        assertEquals(1f, TabMotion.phase(1f, range), 0f)
    }

    @Test
    fun `every icon ends fully drawn at the end of its animation`() {
        TabMotion.allRanges.forEach { assertEquals(1f, TabMotion.phase(1f, it), 0f) }
        assertEquals(0f, TabMotion.wobble(1f), 1e-6f)
        assertEquals(0f, TabMotion.pulse(1f), 1e-6f)
        assertEquals(1f, TabMotion.easeOutBack(1f), 1e-6f)
    }

    @Test
    fun `ease out back overshoots before settling`() {
        assertEquals(0f, TabMotion.easeOutBack(0f), 1e-6f)
        assertTrue(TabMotion.easeOutBack(0.7f) > 1f)
    }
}
