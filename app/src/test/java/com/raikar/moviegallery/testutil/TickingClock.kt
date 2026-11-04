package com.raikar.moviegallery.testutil

import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Clock
import kotlin.time.Instant

/** Advances one millisecond per read, so each save gets a distinct, increasing timestamp. */
class TickingClock(
    start: Long = 1_000L,
) : Clock {
    private val next = AtomicLong(start)

    override fun now(): Instant = Instant.fromEpochMilliseconds(next.getAndIncrement())
}
