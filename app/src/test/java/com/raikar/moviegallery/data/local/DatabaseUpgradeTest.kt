package com.raikar.moviegallery.data.local

import android.app.Application
import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Users updating the app must keep their watchlist: the database the app opens now has to be
 * the file earlier builds wrote.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DatabaseUpgradeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val open = mutableListOf<MovieGalleryDatabase>()

    @Before
    fun deleteDatabaseFile() {
        context.deleteDatabase(INSTALLED_NAME)
    }

    @After
    fun closeDatabases() {
        open.forEach { it.close() }
        context.deleteDatabase(INSTALLED_NAME)
    }

    private fun entity(movieId: Int) =
        WatchlistEntity(
            ownerUid = "uid-a",
            movieId = movieId,
            title = "Movie $movieId",
            year = 2024,
            rating = 7.0,
            genres = listOf("Drama"),
            synopsis = "",
            posterUrl = null,
            backdropUrl = null,
            addedAt = movieId.toLong(),
        )

    @Test
    fun `movies saved by the previous builder are read back by the current one`() =
        runTest {
            // Deliberately a verbatim copy of the call builds before the shared builder made
            // (literal name included): it pins down the file existing installs have on disk.
            val installed =
                Room
                    .databaseBuilder(context, MovieGalleryDatabase::class.java, "movie_gallery.db")
                    .build()
                    .also { open += it }
            installed.watchlistDao().toggle(entity(10))
            installed.watchlistDao().toggle(entity(20))
            installed.close()

            val updated = movieGalleryDatabaseBuilder(context).applySharedConfig().build().also { open += it }

            assertEquals(
                listOf(10, 20),
                updated
                    .watchlistDao()
                    .observe("uid-a")
                    .first()
                    .map { it.movieId },
            )
        }

    private companion object {
        const val INSTALLED_NAME = "movie_gallery.db"
    }
}
