package com.raikar.moviegallery.ui.screens.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeaderCollapseTest {
    private val expanded = 1000f
    private val collapsed = 200f

    @Test
    fun `at the top the header is fully expanded`() {
        assertEquals(0f, collapseFraction(0, 0, expanded, collapsed), 0f)
    }

    @Test
    fun `scrolling down collapses in proportion to the offset`() {
        assertEquals(0.5f, collapseFraction(0, 400, expanded, collapsed), 0.0001f)
    }

    @Test
    fun `scrolling past the collapse distance stays fully collapsed`() {
        assertEquals(1f, collapseFraction(0, 800, expanded, collapsed), 0f)
        assertEquals(1f, collapseFraction(0, 950, expanded, collapsed), 0f)
    }

    @Test
    fun `first item scrolled out of view is fully collapsed`() {
        assertEquals(1f, collapseFraction(1, 0, expanded, collapsed), 0f)
        assertEquals(1f, collapseFraction(3, 120, expanded, collapsed), 0f)
    }

    @Test
    fun `scrolling back up expands again`() {
        val down = collapseFraction(0, 600, expanded, collapsed)
        val up = collapseFraction(0, 100, expanded, collapsed)
        assertTrue(up < down)
        assertEquals(0.125f, up, 0.0001f)
    }

    @Test
    fun `landscape collapses over a shorter distance than portrait`() {
        val portrait = collapseFraction(0, 300, expandedHeightPx = 1000f, collapsedHeightPx = 200f)
        val landscape = collapseFraction(0, 300, expandedHeightPx = 600f, collapsedHeightPx = 200f)
        assertTrue(landscape > portrait)
    }

    @Test
    fun `nothing to collapse counts as collapsed`() {
        assertEquals(1f, collapseFraction(0, 0, expandedHeightPx = 200f, collapsedHeightPx = 200f), 0f)
    }

    @Test
    fun `content already long enough needs no extra padding`() {
        assertEquals(0, extraBottomPaddingPx(viewportHeightPx = 2000, contentHeightPx = 3000, collapseDistancePx = 800))
        assertEquals(0, extraBottomPaddingPx(viewportHeightPx = 2000, contentHeightPx = 2800, collapseDistancePx = 800))
    }

    @Test
    fun `short content gets exactly the shortfall`() {
        assertEquals(
            500,
            extraBottomPaddingPx(viewportHeightPx = 2000, contentHeightPx = 2300, collapseDistancePx = 800),
        )
        assertEquals(2800, extraBottomPaddingPx(viewportHeightPx = 2000, contentHeightPx = 0, collapseDistancePx = 800))
    }
}
