package com.raikar.moviegallery.di

import com.raikar.moviegallery.data.remote.auth.FirebaseAuthTokenProvider
import com.raikar.moviegallery.data.repository.AiChatRepositoryImpl
import com.raikar.moviegallery.data.repository.AuthRepositoryImpl
import com.raikar.moviegallery.domain.repository.AiChatRepository
import com.raikar.moviegallery.domain.repository.AuthRepository
import com.raikar.moviegallery.domain.repository.AuthTokenProvider
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
    abstract fun bindAiChatRepository(impl: AiChatRepositoryImpl): AiChatRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(impl: FirebaseAuthTokenProvider): AuthTokenProvider
}
