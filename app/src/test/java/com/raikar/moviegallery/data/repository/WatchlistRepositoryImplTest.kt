package com.raikar.moviegallery.data.repository

import android.app.Application
import app.cash.turbine.test
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.testutil.FakeAuthRepository
import com.raikar.moviegallery.testutil.TestDatabases
import com.raikar.moviegallery.testutil.TickingClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WatchlistRepositoryImplTest {
    private val database = TestDatabases.inMemory()
    private val auth = FakeAuthRepository().apply { currentUserId.value = USER_A }
    private val repository = WatchlistRepositoryImpl(database.watchlistDao(), auth, TickingClock())

    @After
    fun closeDatabase() = database.close()

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

    private suspend fun ids() = repository.watchlist.first().map { it.id }

    private fun signInAs(uid: String?) {
        auth.currentUserId.value = uid
    }

    @Test
    fun `starts empty`() =
        runTest {
            assertEquals(emptyList<Movie>(), repository.watchlist.first())
        }

    @Test
    fun `toggle adds then removes the movie`() =
        runTest {
            repository.toggle(movie(1))
            assertEquals(listOf(1), ids())

            repository.toggle(movie(1))
            assertTrue(ids().isEmpty())
        }

    @Test
    fun `the full movie is retained so the watchlist needs no network`() =
        runTest {
            repository.toggle(movie(42))

            assertEquals(movie(42), repository.watchlist.first().single())
        }

    @Test
    fun `insertion order is preserved for a stable grid`() =
        runTest {
            repository.toggle(movie(3))
            repository.toggle(movie(1))
            repository.toggle(movie(2))

            assertEquals(listOf(3, 1, 2), ids())
        }

    @Test
    fun `removing from the middle keeps the rest in order`() =
        runTest {
            listOf(3, 1, 2).forEach { repository.toggle(movie(it)) }

            repository.toggle(movie(1))

            assertEquals(listOf(3, 2), ids())
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

                // Room re-runs the query on any table change; distinctUntilChanged drops the repeat.
                repository.toggle(movie(2))
                assertEquals(listOf(2), ids())
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

    @Test
    fun `rapid repeated taps never save a movie twice`() =
        runTest {
            suspend fun tap(times: Int) =
                coroutineScope {
                    repeat(times) { launch(Dispatchers.Default) { repository.toggle(movie(1)) } }
                }

            // An odd number of taps leaves the movie saved exactly once.
            tap(times = 7)
            assertEquals(listOf(1), ids())

            // An even number of further taps leaves it as it was.
            tap(times = 6)
            assertEquals(listOf(1), ids())

            tap(times = 1)
            assertTrue(ids().isEmpty())
        }

    @Test
    fun `another account on the same device sees an empty watchlist`() =
        runTest {
            repository.toggle(movie(1))

            signInAs(null)
            signInAs(USER_B)

            assertTrue(ids().isEmpty())
            assertFalse(repository.isInWatchlist(id = 1).first())
        }

    @Test
    fun `changes stay with the account that made them`() =
        runTest {
            repository.toggle(movie(1))

            signInAs(USER_B)
            repository.toggle(movie(2))
            // B un-saving a movie only A saved must not touch A's row: it saves it for B instead.
            repository.toggle(movie(1))
            assertEquals(listOf(2, 1), ids())

            signInAs(USER_A)
            assertEquals(listOf(1), ids())
        }

    @Test
    fun `sign out and back in keeps the watchlist`() =
        runTest {
            repository.toggle(movie(1))

            signInAs(null)
            assertTrue(ids().isEmpty())

            signInAs(USER_A)
            assertEquals(listOf(1), ids())
        }

    @Test
    fun `account A gets exactly its list back after account B used the device`() =
        runTest {
            listOf(3, 1).forEach { repository.toggle(movie(it)) }
            signInAs(null)

            signInAs(USER_B)
            repository.toggle(movie(2))
            signInAs(null)

            signInAs(USER_A)
            assertEquals(listOf(3, 1), ids())
        }

    @Test
    fun `open screens follow an account switch`() =
        runTest {
            repository.toggle(movie(1))

            repository.watchlist.test {
                assertEquals(listOf(1), awaitItem().map { it.id })

                signInAs(USER_B)
                assertEquals(emptyList<Movie>(), awaitItem())

                signInAs(USER_A)
                assertEquals(listOf(1), awaitItem().map { it.id })

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `toggling with no signed-in account writes nothing`() =
        runTest {
            signInAs(null)
            repository.toggle(movie(1))

            assertTrue(ids().isEmpty())
            signInAs(USER_A)
            assertTrue(ids().isEmpty())
            signInAs(USER_B)
            assertTrue(ids().isEmpty())
        }

    private companion object {
        const val USER_A = "uid-a"
        const val USER_B = "uid-b"
    }
}
