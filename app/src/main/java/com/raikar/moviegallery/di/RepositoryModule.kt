package com.raikar.moviegallery.di

import com.raikar.moviegallery.data.remote.auth.FirebaseAuthTokenProvider
import com.raikar.moviegallery.data.repository.AiChatRepositoryImpl
import com.raikar.moviegallery.data.repository.AuthRepositoryImpl
import com.raikar.moviegallery.data.repository.MovieRepositoryImpl
import com.raikar.moviegallery.data.repository.TvShowRepositoryImpl
import com.raikar.moviegallery.data.repository.WatchlistRepositoryImpl
import com.raikar.moviegallery.domain.repository.AiChatRepository
import com.raikar.moviegallery.domain.repository.AuthRepository
import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import com.raikar.moviegallery.domain.repository.MovieRepository
import com.raikar.moviegallery.domain.repository.TvShowRepository
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

    @Binds
    @Singleton
    abstract fun bindAiChatRepository(impl: AiChatRepositoryImpl): AiChatRepository

    @Binds
    @Singleton
    abstract fun bindTvShowRepository(impl: TvShowRepositoryImpl): TvShowRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(impl: FirebaseAuthTokenProvider): AuthTokenProvider
}
