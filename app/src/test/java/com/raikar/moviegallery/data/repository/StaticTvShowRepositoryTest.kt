package com.raikar.moviegallery.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticTvShowRepositoryTest {
    @Test
    fun `exposes ten shows with distinct ids and a rating`() =
        runTest {
            val shows = StaticTvShowRepository().topTvShows()

            assertEquals(10, shows.size)
            assertEquals(10, shows.map { it.id }.distinct().size)
            assertTrue(shows.all { it.hasRating })
            assertTrue(shows.all { it.title.isNotBlank() })
        }

    /** The repository promises best-first, and the screen derives its rank from the order. */
    @Test
    fun `orders shows by non-increasing rating`() =
        runTest {
            val ratings = StaticTvShowRepository().topTvShows().map { it.rating }

            assertEquals(ratings.sortedDescending(), ratings)
        }
}
