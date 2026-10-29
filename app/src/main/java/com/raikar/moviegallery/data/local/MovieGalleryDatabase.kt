package com.raikar.moviegallery.data.local

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase

/**
 * The app's on-device database. Schemas are exported to `app/schemas/` and there is no
 * destructive fallback, so every version bump needs a real migration — otherwise users'
 * watchlists would be wiped on upgrade.
 */
@Database(entities = [WatchlistEntity::class], version = 1, exportSchema = true)
@ColumnTypeConverters(Converters::class)
abstract class MovieGalleryDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        const val NAME = "movie_gallery.db"
    }
}
