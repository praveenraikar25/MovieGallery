package com.raikar.moviegallery.data.local

import androidx.room3.Room
import androidx.sqlite.driver.NativeSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves the iOS database path: Room on the system SQLite driver, both in memory and on disk.
 * The DAO's logic is covered in depth by `:app`'s Robolectric `WatchlistDaoTest`; this suite
 * is intentionally smaller.
 */
class WatchlistDaoIosTest {
    private val open = mutableListOf<MovieGalleryDatabase>()
    private val diskPath = "${NSTemporaryDirectory().trimEnd('/')}/watchlist-ios-test.db"

    @AfterTest
    fun closeAndDelete() {
        open.forEach { it.close() }
        deleteDatabaseFiles()
    }

    private fun inMemory(): MovieGalleryDatabase =
        Room
            .inMemoryDatabaseBuilder<MovieGalleryDatabase>()
            .setDriver(NativeSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .applySharedConfig()
            .build()
            .also { open += it }

    private fun onDisk(): MovieGalleryDatabase =
        movieGalleryDatabaseBuilder(diskPath).applySharedConfig().build().also { open += it }

    /** The database file plus SQLite's write-ahead log and shared-memory side files. */
    @OptIn(ExperimentalForeignApi::class)
    private fun deleteDatabaseFiles() {
        listOf("", "-wal", "-shm").forEach { NSFileManager.defaultManager.removeItemAtPath(diskPath + it, null) }
    }

    private fun entity(
        movieId: Int,
        ownerUid: String = "uid-a",
    ) = WatchlistEntity(
        ownerUid = ownerUid,
        movieId = movieId,
        title = "Movie $movieId",
        year = 2024,
        rating = 7.0,
        genres = listOf("Drama", "Comedy"),
        synopsis = "",
        posterUrl = null,
        backdropUrl = null,
        addedAt = movieId.toLong(),
    )

    private suspend fun WatchlistDao.ids(ownerUid: String = "uid-a") = observe(ownerUid).first().map { it.movieId }

    @Test
    fun `saved movies are listed oldest-added first`() =
        runTest {
            val dao = inMemory().watchlistDao()
            dao.toggle(entity(3))
            dao.toggle(entity(1))
            dao.toggle(entity(2))

            assertEquals(listOf(1, 2, 3), dao.ids())
        }

    @Test
    fun `toggle adds then removes`() =
        runTest {
            val dao = inMemory().watchlistDao()

            dao.toggle(entity(1))
            assertTrue(dao.observeContains("uid-a", 1).first())

            dao.toggle(entity(1))
            assertFalse(dao.observeContains("uid-a", 1).first())
            assertEquals(emptyList(), dao.ids())
        }

    @Test
    fun `one account's rows never appear for another`() =
        runTest {
            val dao = inMemory().watchlistDao()
            dao.toggle(entity(1, ownerUid = "uid-a"))
            dao.toggle(entity(2, ownerUid = "uid-b"))

            assertEquals(listOf(1), dao.ids("uid-a"))
            assertEquals(listOf(2), dao.ids("uid-b"))
        }

    @Test
    fun `an on-disk database keeps its rows after being closed and reopened`() =
        runTest {
            deleteDatabaseFiles()
            val firstRun = onDisk()
            firstRun.watchlistDao().toggle(entity(10))
            firstRun.watchlistDao().toggle(entity(20))
            firstRun.close()

            val reopened = onDisk().watchlistDao().observe("uid-a").first()

            assertEquals(listOf(10, 20), reopened.map { it.movieId })
            assertEquals(listOf("Drama", "Comedy"), reopened.first().genres)
        }
}
