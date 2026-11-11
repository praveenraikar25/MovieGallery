package com.raikar.moviegallery.ui.common

/**
 * How far the detail header has collapsed, from 0 (fully expanded) to 1 (a pinned strip).
 *
 * The list's first item is a spacer as tall as the expanded header, so while that item is
 * still on screen its scroll offset *is* how far the header has shrunk. Once it has scrolled
 * out of view the header has been collapsed for a while. Shared so both apps' detail headers
 * collapse alike; iOS, which has no list items, passes index 0 and its scroll offset in points.
 */
fun collapseFraction(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    expandedHeightPx: Float,
    collapsedHeightPx: Float,
): Float {
    val distance = expandedHeightPx - collapsedHeightPx
    return when {
        distance <= 0f -> 1f
        firstVisibleItemIndex > 0 -> 1f
        else -> (firstVisibleItemScrollOffset / distance).coerceIn(0f, 1f)
    }
}

/**
 * Extra space needed after the last item so the list can always scroll the whole
 * [collapseDistancePx]. Without it, a short page can't scroll far enough to finish
 * collapsing and the header would sit half-way with nothing left to scroll.
 */
fun extraBottomPaddingPx(
    viewportHeightPx: Int,
    contentHeightPx: Int,
    collapseDistancePx: Int,
): Int = (viewportHeightPx + collapseDistancePx - contentHeightPx).coerceAtLeast(0)
