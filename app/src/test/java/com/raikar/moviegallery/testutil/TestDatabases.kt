package com.raikar.moviegallery.testutil

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.raikar.moviegallery.data.local.MovieGalleryDatabase
import com.raikar.moviegallery.data.local.applySharedConfig
import com.raikar.moviegallery.data.local.movieGalleryDatabaseBuilder

/**
 * Real Room databases for Robolectric tests, with the same shared configuration production
 * applies. Run those tests with `@Config(application = Application::class)` so the app's own
 * Application (Firebase, App Check) is not started.
 */
object TestDatabases {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    fun inMemory(): MovieGalleryDatabase =
        Room
            .inMemoryDatabaseBuilder(context, MovieGalleryDatabase::class.java)
            .applySharedConfig()
            .build()

    /**
     * A file-backed database opened with the production builder: closing it and calling this
     * again with the same [name] opens the same data, which is how tests simulate the app
     * process being killed and relaunched.
     */
    fun onDisk(name: String = "watchlist-test.db"): MovieGalleryDatabase =
        movieGalleryDatabaseBuilder(context, name).applySharedConfig().build()
}
