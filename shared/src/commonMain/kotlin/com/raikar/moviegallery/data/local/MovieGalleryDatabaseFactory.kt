package com.raikar.moviegallery.data.local

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration

/**
 * Every schema migration, in version order. Both platforms open the same schema, so a new
 * [Migration] is added here once rather than in each platform's builder. Empty while the
 * database is at version 1.
 */
internal val MIGRATIONS: Array<Migration> = emptyArray()

/**
 * Configuration that must be the same on every platform, applied on top of a platform's
 * builder (`movieGalleryDatabaseBuilder` in `androidMain` / `iosMain`). Public only so
 * `:app`'s tests can build the database the way production does; the app itself gets it
 * from the shared Koin `databaseModule`.
 *
 * There is deliberately no destructive fallback: a missing migration must fail loudly in
 * development rather than silently wipe users' watchlists in production.
 */
fun RoomDatabase.Builder<MovieGalleryDatabase>.applySharedConfig(): RoomDatabase.Builder<MovieGalleryDatabase> =
    addMigrations(*MIGRATIONS)
