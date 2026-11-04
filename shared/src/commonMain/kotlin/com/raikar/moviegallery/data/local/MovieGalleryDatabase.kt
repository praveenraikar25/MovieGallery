package com.raikar.moviegallery.data.local

import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/**
 * The app's on-device database. Schemas are exported to `shared/schemas/` and there is no
 * destructive fallback, so every version bump needs a real migration — otherwise users'
 * watchlists would be wiped on upgrade.
 */
@Database(entities = [WatchlistEntity::class], version = 1, exportSchema = true)
@ColumnTypeConverters(Converters::class)
@ConstructedBy(MovieGalleryDatabaseConstructor::class)
abstract class MovieGalleryDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        const val NAME = "movie_gallery.db"
    }
}

/**
 * Room 3's multiplatform hook (same names as Room 2.7's `@ConstructedBy` /
 * `RoomDatabaseConstructor`): non-JVM targets can't instantiate the generated database by
 * reflection, so Room's compiler writes the `actual` of this object for every target.
 */
@Suppress("KotlinNoActualForExpect")
expect object MovieGalleryDatabaseConstructor : RoomDatabaseConstructor<MovieGalleryDatabase> {
    override fun initialize(): MovieGalleryDatabase
}
