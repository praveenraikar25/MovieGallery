package com.raikar.moviegallery.testutil

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicLong

/** Advances one millisecond per read, so each save gets a distinct, increasing timestamp. */
class TickingClock(
    start: Long = 1_000L,
) : Clock() {
    private val next = AtomicLong(start)

    override fun instant(): Instant = Instant.ofEpochMilli(next.getAndIncrement())

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}
