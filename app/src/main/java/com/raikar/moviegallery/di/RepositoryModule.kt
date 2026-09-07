package com.raikar.moviegallery.di

import com.raikar.moviegallery.data.repository.MovieRepositoryImpl
import com.raikar.moviegallery.data.repository.WatchlistRepositoryImpl
import com.raikar.moviegallery.domain.repository.MovieRepository
import com.raikar.moviegallery.domain.repository.WatchlistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(impl: WatchlistRepositoryImpl): WatchlistRepository
}
