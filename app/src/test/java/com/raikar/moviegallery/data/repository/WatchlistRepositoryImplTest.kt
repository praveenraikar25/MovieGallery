package com.raikar.moviegallery.data.repository

import app.cash.turbine.test
import com.raikar.moviegallery.data.local.InMemoryWatchlistDataSource
import com.raikar.moviegallery.domain.model.Movie
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchlistRepositoryImplTest {
    private val repository = WatchlistRepositoryImpl(InMemoryWatchlistDataSource())

    private fun movie(id: Int) =
        Movie(
            id = id,
            title = "Movie $id",
            year = 2024,
            rating = 7.0,
            genres = listOf("Drama"),
            synopsis = "",
            posterUrl = null,
            backdropUrl = null,
        )

    @Test
    fun `starts empty`() =
        runTest {
            assertEquals(emptyList<Movie>(), repository.watchlist.value)
        }

    @Test
    fun `toggle adds then removes the movie`() =
        runTest {
            repository.toggle(movie(1))
            assertEquals(listOf(1), repository.watchlist.value.map { it.id })

            repository.toggle(movie(1))
            assertTrue(repository.watchlist.value.isEmpty())
        }

    @Test
    fun `the full movie is retained so the watchlist needs no network`() =
        runTest {
            repository.toggle(movie(42))

            val stored = repository.watchlist.value.single()
            assertEquals("Movie 42", stored.title)
            assertEquals(listOf("Drama"), stored.genres)
        }

    @Test
    fun `insertion order is preserved for a stable grid`() =
        runTest {
            repository.toggle(movie(3))
            repository.toggle(movie(1))
            repository.toggle(movie(2))

            assertEquals(listOf(3, 1, 2), repository.watchlist.value.map { it.id })
        }

    @Test
    fun `removing from the middle keeps the rest in order`() =
        runTest {
            listOf(3, 1, 2).forEach { repository.toggle(movie(it)) }

            repository.toggle(movie(1))

            assertEquals(listOf(3, 2), repository.watchlist.value.map { it.id })
        }

    @Test
    fun `isInWatchlist emits on add and remove`() =
        runTest {
            repository.isInWatchlist(id = 1).test {
                assertFalse(awaitItem())

                repository.toggle(movie(1))
                assertTrue(awaitItem())

                repository.toggle(movie(1))
                assertFalse(awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `isInWatchlist ignores changes to other movies`() =
        runTest {
            repository.isInWatchlist(id = 1).test {
                assertFalse(awaitItem())

                // distinctUntilChanged means an unrelated toggle produces no emission.
                repository.toggle(movie(2))
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the watchlist flow emits each change`() =
        runTest {
            repository.watchlist.test {
                assertEquals(emptyList<Movie>(), awaitItem())

                repository.toggle(movie(1))
                assertEquals(listOf(1), awaitItem().map { it.id })

                repository.toggle(movie(2))
                assertEquals(listOf(1, 2), awaitItem().map { it.id })

                cancelAndIgnoreRemainingEvents()
            }
        }
}
