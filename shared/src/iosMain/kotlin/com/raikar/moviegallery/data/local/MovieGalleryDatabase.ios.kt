package com.raikar.moviegallery.data.local

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.NativeSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * The iOS database builder: the file at [path] on the system SQLite library (iOS binaries
 * link it with `-lsqlite3`, see `shared/build.gradle.kts`), with queries on [Dispatchers.IO].
 */
fun movieGalleryDatabaseBuilder(path: String = defaultDatabasePath()): RoomDatabase.Builder<MovieGalleryDatabase> =
    Room
        .databaseBuilder<MovieGalleryDatabase>(name = path)
        .setDriver(NativeSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)

/**
 * `<Application Support>/movie_gallery.db`, creating the directory on first launch.
 * Application Support rather than Documents, which holds user-facing files and shows up in
 * the Files app once file sharing is on. Throws if the directory can't be resolved: the app
 * can't run without its database, and falling back to an in-memory one would silently lose
 * the watchlist.
 */
@OptIn(ExperimentalForeignApi::class)
private fun defaultDatabasePath(): String {
    val directory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
    val path = checkNotNull(directory?.path) { "Application Support directory is unavailable" }
    return "$path/${MovieGalleryDatabase.NAME}"
}
