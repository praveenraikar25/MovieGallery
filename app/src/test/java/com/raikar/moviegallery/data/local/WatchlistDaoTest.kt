package com.raikar.moviegallery.data.local

import android.app.Application
import app.cash.turbine.test
import com.raikar.moviegallery.testutil.TestDatabases
import kotlinx.coroutines.flow.first
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
class WatchlistDaoTest {
    private val database = TestDatabases.inMemory()
    private val dao = database.watchlistDao()

    @After
    fun closeDatabase() = database.close()

    private fun entity(
        movieId: Int,
        ownerUid: String = "uid-a",
        addedAt: Long = movieId.toLong(),
        genres: List<String> = listOf("Drama"),
    ) = WatchlistEntity(
        ownerUid = ownerUid,
        movieId = movieId,
        title = "Movie $movieId",
        year = 2024,
        rating = 7.0,
        genres = genres,
        synopsis = "",
        posterUrl = null,
        backdropUrl = null,
        addedAt = addedAt,
    )

    private suspend fun ids(ownerUid: String = "uid-a") = dao.observe(ownerUid).first().map { it.movieId }

    @Test
    fun `lists movies oldest-added first`() =
        runTest {
            dao.toggle(entity(3, addedAt = 30))
            dao.toggle(entity(1, addedAt = 10))
            dao.toggle(entity(2, addedAt = 20))

            assertEquals(listOf(1, 2, 3), ids())
        }

    @Test
    fun `saves in the same millisecond keep insertion order`() =
        runTest {
            dao.toggle(entity(9, addedAt = 5))
            dao.toggle(entity(4, addedAt = 5))
            dao.toggle(entity(7, addedAt = 5))

            assertEquals(listOf(9, 4, 7), ids())
        }

    @Test
    fun `toggle adds then removes`() =
        runTest {
            dao.toggle(entity(1))
            assertEquals(listOf(1), ids())

            dao.toggle(entity(1))
            assertTrue(ids().isEmpty())
        }

    @Test
    fun `re-adding a removed movie moves it to the end`() =
        runTest {
            dao.toggle(entity(1, addedAt = 10))
            dao.toggle(entity(2, addedAt = 20))
            dao.toggle(entity(1))
            dao.toggle(entity(1, addedAt = 30))

            assertEquals(listOf(2, 1), ids())
        }

    @Test
    fun `one account's rows never appear for another`() =
        runTest {
            dao.toggle(entity(1, ownerUid = "uid-a"))
            dao.toggle(entity(2, ownerUid = "uid-b"))
            // The same movie saved by both accounts is two independent rows.
            dao.toggle(entity(3, ownerUid = "uid-a"))
            dao.toggle(entity(3, ownerUid = "uid-b"))

            assertEquals(listOf(1, 3), ids("uid-a"))
            assertEquals(listOf(2, 3), ids("uid-b"))
            assertFalse(dao.observeContains("uid-b", 1).first())

            dao.toggle(entity(3, ownerUid = "uid-b"))
            assertEquals(listOf(1, 3), ids("uid-a"))
            assertEquals(listOf(2), ids("uid-b"))
        }

    @Test
    fun `observeContains emits on add and remove`() =
        runTest {
            dao.observeContains("uid-a", 1).test {
                assertFalse(awaitItem())
                dao.toggle(entity(1))
                assertTrue(awaitItem())
                dao.toggle(entity(1))
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `stored details round-trip including genres`() =
        runTest {
            val saved = entity(1, genres = listOf("Science Fiction", "Kids, Family")).copy(year = null)
            dao.toggle(saved)

            assertEquals(listOf(saved), dao.observe("uid-a").first())
        }
}
