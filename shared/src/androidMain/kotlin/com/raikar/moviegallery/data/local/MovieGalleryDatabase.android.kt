package com.raikar.moviegallery.data.local

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

/**
 * The Android database builder: the database file [name] under the app's databases directory,
 * on Room's default driver (the platform SQLite) and Room's own query executors.
 *
 * This is the exact call the app has always made, so existing installs keep opening the same
 * file. Don't switch to the Context-less `Room.databaseBuilder<T>(name)` overload: it treats
 * [name] as a path and would open a different, empty database, and users' watchlists would
 * silently disappear on upgrade.
 *
 * Public only so `:app`'s tests can build the database the way production does; the app
 * itself gets it from the shared Koin `databaseModule`.
 */
fun movieGalleryDatabaseBuilder(
    context: Context,
    name: String = MovieGalleryDatabase.NAME,
): RoomDatabase.Builder<MovieGalleryDatabase> = Room.databaseBuilder(context, MovieGalleryDatabase::class.java, name)
