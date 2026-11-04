package com.raikar.moviegallery.di

import com.apollographql.apollo.ApolloClient
import com.raikar.moviegallery.data.local.WatchlistDao
import com.raikar.moviegallery.data.repository.CountryRepositoryImpl
import com.raikar.moviegallery.data.repository.WatchlistRepositoryImpl
import com.raikar.moviegallery.di.qualifier.IoDispatcher
import com.raikar.moviegallery.domain.repository.AiChatRepository
import com.raikar.moviegallery.domain.repository.AuthRepository
import com.raikar.moviegallery.domain.repository.CountryRepository
import com.raikar.moviegallery.domain.repository.MovieRepository
import com.raikar.moviegallery.domain.repository.TvShowRepository
import com.raikar.moviegallery.domain.repository.WatchlistRepository
import com.raikar.moviegallery.domain.usecase.GetCountryDetailsUseCase
import com.raikar.moviegallery.domain.usecase.GetHomeSectionsUseCase
import com.raikar.moviegallery.domain.usecase.GetMovieUseCase
import com.raikar.moviegallery.domain.usecase.GetTopTvShowsUseCase
import com.raikar.moviegallery.domain.usecase.IdentifyMoviePosterUseCase
import com.raikar.moviegallery.domain.usecase.IsSignedInUseCase
import com.raikar.moviegallery.domain.usecase.ObserveCurrentUserUseCase
import com.raikar.moviegallery.domain.usecase.ObserveIsInWatchlistUseCase
import com.raikar.moviegallery.domain.usecase.ObserveWatchlistMoviesUseCase
import com.raikar.moviegallery.domain.usecase.SearchMoviesUseCase
import com.raikar.moviegallery.domain.usecase.SendChatMessageUseCase
import com.raikar.moviegallery.domain.usecase.SignInUseCase
import com.raikar.moviegallery.domain.usecase.SignInWithGoogleUseCase
import com.raikar.moviegallery.domain.usecase.SignOutUseCase
import com.raikar.moviegallery.domain.usecase.SignUpUseCase
import com.raikar.moviegallery.domain.usecase.ToggleWatchlistUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Wires the classes that live in :shared. Shared code compiles for iOS too, so it can't
 * use javax.inject's `@Inject`, and Dagger can't add it to classes it doesn't compile —
 * every shared class Hilt hands out is provided here instead. Goes away once DI moves
 * to a multiplatform library.
 */
@Module
@InstallIn(SingletonComponent::class)
object SharedModule {
    @Provides
    @Singleton
    fun provideCountryRepository(
        apollo: ApolloClient,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): CountryRepository = CountryRepositoryImpl(apollo, ioDispatcher)

    @Provides
    @Singleton
    fun provideWatchlistRepository(
        dao: WatchlistDao,
        authRepository: AuthRepository,
        clock: Clock,
    ): WatchlistRepository = WatchlistRepositoryImpl(dao, authRepository, clock)

    @Provides
    fun provideGetCountryDetailsUseCase(repository: CountryRepository) = GetCountryDetailsUseCase(repository)

    @Provides
    fun provideGetHomeSectionsUseCase(repository: MovieRepository) = GetHomeSectionsUseCase(repository)

    @Provides
    fun provideGetMovieUseCase(repository: MovieRepository) = GetMovieUseCase(repository)

    @Provides
    fun provideGetTopTvShowsUseCase(repository: TvShowRepository) = GetTopTvShowsUseCase(repository)

    @Provides
    fun provideIdentifyMoviePosterUseCase(repository: AiChatRepository) = IdentifyMoviePosterUseCase(repository)

    @Provides
    fun provideIsSignedInUseCase(repository: AuthRepository) = IsSignedInUseCase(repository)

    @Provides
    fun provideObserveCurrentUserUseCase(repository: AuthRepository) = ObserveCurrentUserUseCase(repository)

    @Provides
    fun provideObserveIsInWatchlistUseCase(repository: WatchlistRepository) = ObserveIsInWatchlistUseCase(repository)

    @Provides
    fun provideObserveWatchlistMoviesUseCase(repository: WatchlistRepository) =
        ObserveWatchlistMoviesUseCase(repository)

    @Provides
    fun provideSearchMoviesUseCase(repository: MovieRepository) = SearchMoviesUseCase(repository)

    @Provides
    fun provideSendChatMessageUseCase(repository: AiChatRepository) = SendChatMessageUseCase(repository)

    @Provides
    fun provideSignInUseCase(repository: AuthRepository) = SignInUseCase(repository)

    @Provides
    fun provideSignInWithGoogleUseCase(repository: AuthRepository) = SignInWithGoogleUseCase(repository)

    @Provides
    fun provideSignOutUseCase(repository: AuthRepository) = SignOutUseCase(repository)

    @Provides
    fun provideSignUpUseCase(repository: AuthRepository) = SignUpUseCase(repository)

    @Provides
    fun provideToggleWatchlistUseCase(repository: WatchlistRepository) = ToggleWatchlistUseCase(repository)
}
