package com.raikar.moviegallery.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.raikar.moviegallery.data.local.MovieGalleryDatabase
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.testutil.FakeAuthRepository
import com.raikar.moviegallery.testutil.TestDatabases
import com.raikar.moviegallery.testutil.TickingClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Simulates the app process being killed and relaunched: the database file is closed
 * and a brand-new database, auth session and repository are built over the same file.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WatchlistPersistenceTest {
    private val open = mutableListOf<MovieGalleryDatabase>()

    @Before
    fun deleteDatabaseFile() {
        ApplicationProvider.getApplicationContext<Application>().deleteDatabase(DB_NAME)
    }

    @After
    fun closeDatabases() = open.forEach { it.close() }

    /** One "process": a fresh database instance and repository, signed in as [uid]. */
    private fun launchApp(uid: String = "uid-a"): Pair<MovieGalleryDatabase, WatchlistRepositoryImpl> {
        val database = TestDatabases.onDisk(DB_NAME).also { open += it }
        val auth = FakeAuthRepository().apply { currentUserId.value = uid }
        return database to WatchlistRepositoryImpl(database.watchlistDao(), auth, TickingClock())
    }

    private fun movie(
        id: Int,
        year: Int? = 2024,
    ) = Movie(
        id = id,
        title = "Movie $id",
        year = year,
        rating = 6.5,
        genres = listOf("Action", "Comedy"),
        synopsis = "Synopsis $id",
        posterUrl = "https://image.tmdb.org/t/p/w500/poster$id.jpg",
        backdropUrl = null,
    )

    @Test
    fun `saved movies survive a restart in the order they were added`() =
        runTest {
            val (firstRun, before) = launchApp()
            before.toggle(movie(10))
            before.toggle(movie(20))
            firstRun.close()

            val (_, after) = launchApp()

            assertEquals(listOf(10, 20), after.watchlist.first().map { it.id })
        }

    @Test
    fun `a removal survives a restart`() =
        runTest {
            val (firstRun, before) = launchApp()
            before.toggle(movie(10))
            before.toggle(movie(20))
            before.toggle(movie(10))
            firstRun.close()

            val (_, after) = launchApp()

            assertEquals(listOf(20), after.watchlist.first().map { it.id })
            assertFalse(after.isInWatchlist(id = 10).first())
        }

    @Test
    fun `saved details are returned after a restart without any network`() =
        runTest {
            val saved = listOf(movie(10, year = null), movie(20))
            val (firstRun, before) = launchApp()
            saved.forEach { before.toggle(it) }
            firstRun.close()

            // The repository has no network dependency at all; everything comes from the file.
            val (_, after) = launchApp()

            assertEquals(saved, after.watchlist.first())
        }

    private companion object {
        const val DB_NAME = "watchlist-persistence-test.db"
    }
}
