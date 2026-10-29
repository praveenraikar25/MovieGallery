package com.raikar.moviegallery.di

import android.content.Context
import androidx.room3.Room
import com.raikar.moviegallery.data.local.MovieGalleryDatabase
import com.raikar.moviegallery.data.local.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // No fallbackToDestructiveMigration: a missing migration should fail loudly in
    // development rather than silently wipe users' watchlists in production.
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MovieGalleryDatabase =
        Room.databaseBuilder(context, MovieGalleryDatabase::class.java, MovieGalleryDatabase.NAME).build()

    @Provides
    fun provideWatchlistDao(database: MovieGalleryDatabase): WatchlistDao = database.watchlistDao()
}
