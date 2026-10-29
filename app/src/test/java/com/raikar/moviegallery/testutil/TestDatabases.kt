package com.raikar.moviegallery.testutil

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.raikar.moviegallery.data.local.MovieGalleryDatabase

/**
 * Real Room databases for Robolectric tests. Run those tests with
 * `@Config(application = Application::class)` so the app's own Application (Firebase,
 * App Check) is not started.
 */
object TestDatabases {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    fun inMemory(): MovieGalleryDatabase =
        Room.inMemoryDatabaseBuilder(context, MovieGalleryDatabase::class.java).build()

    /**
     * A file-backed database: closing it and calling this again with the same [name] opens
     * the same data, which is how tests simulate the app process being killed and relaunched.
     */
    fun onDisk(name: String = "watchlist-test.db"): MovieGalleryDatabase =
        Room.databaseBuilder(context, MovieGalleryDatabase::class.java, name).build()
}
