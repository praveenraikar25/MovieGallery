package com.raikar.moviegallery.ui.components

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/** Pure timing and easing maths for the tab bar animations; no Android or Compose imports. */
object TabMotion {
    const val TAB_COUNT = 4

    // Total run time per tab, in milliseconds. Sub-ranges below are fractions of this.
    const val HOME_MS = 700
    const val SEARCH_MS = 750
    const val WATCHLIST_MS = 650
    const val PROFILE_MS = 650

    /** A phase that starts at [start] and ends at [end] of a tab's overall progress. */
    data class Range(
        val start: Float,
        val end: Float,
    )

    val HomeRoof = Range(0f, 0.55f)
    val HomeWalls = Range(0.2f, 0.75f)
    val HomeDoor = Range(0.43f, 1f)
    val HomeSmoke = Range(0.5f, 1f)

    val SearchHandle = Range(0.27f, 0.67f)
    val SearchGlint = Range(0.4f, 1f)

    val WatchlistDrop = Range(0f, 0.85f)
    val WatchlistFill = Range(0.23f, 0.85f)
    val WatchlistSparks = Range(0.38f, 1f)

    val ProfileRing = Range(0f, 0.77f)
    val ProfileBody = Range(0.12f, 0.7f)
    val ProfileHead = Range(0.18f, 0.8f)
    val ProfileRipple = Range(0.46f, 1f)

    val allRanges =
        listOf(
            HomeRoof,
            HomeWalls,
            HomeDoor,
            HomeSmoke,
            SearchHandle,
            SearchGlint,
            WatchlistDrop,
            WatchlistFill,
            WatchlistSparks,
            ProfileRing,
            ProfileBody,
            ProfileHead,
            ProfileRipple,
        )

    /** Motion is skipped when the system animator duration scale is 0. */
    fun animationsEnabled(animatorScale: Float): Boolean = animatorScale > 0f

    /** Left edge of the selection pill, in the same unit as [tabWidth]. */
    fun pillOffset(
        index: Int,
        tabWidth: Float,
    ): Float = index.coerceIn(0, TAB_COUNT - 1) * tabWidth

    /** Maps [progress] to 0..1 within [range]; 0 before it starts and 1 after it ends. */
    fun phase(
        progress: Float,
        range: Range,
    ): Float = ((progress - range.start) / (range.end - range.start)).coerceIn(0f, 1f)

    /** Ease-out with a small overshoot past 1 before settling; 0 at 0 and 1 at 1. */
    fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val x = t.coerceIn(0f, 1f) - 1f
        return 1f + c3 * x.pow(3) + c1 * x.pow(2)
    }

    /** Squash-and-stretch amount: 0 at the start and end, swinging either side in between. */
    fun wobble(t: Float): Float = 0.14f * sin(2f * PI.toFloat() * t) * (1f - t)

    /** Rises and falls back to 0 over 0..1, for puffs and sparks that fade in and out. */
    fun pulse(t: Float): Float = sin(PI.toFloat() * t.coerceIn(0f, 1f))
}
