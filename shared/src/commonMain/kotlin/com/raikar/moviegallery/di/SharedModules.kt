package com.raikar.moviegallery.di

import com.raikar.moviegallery.data.local.MovieGalleryDatabase
import com.raikar.moviegallery.data.local.WatchlistDao
import com.raikar.moviegallery.data.remote.BffApi
import com.raikar.moviegallery.data.remote.NetworkConfig
import com.raikar.moviegallery.data.remote.TmdbApi
import com.raikar.moviegallery.data.remote.createBffHttpClient
import com.raikar.moviegallery.data.remote.createTmdbHttpClient
import com.raikar.moviegallery.data.repository.CountryRepositoryImpl
import com.raikar.moviegallery.data.repository.MovieRepositoryImpl
import com.raikar.moviegallery.data.repository.TvShowRepositoryImpl
import com.raikar.moviegallery.data.repository.WatchlistRepositoryImpl
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
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock

/**
 * The TMDB and BFF Ktor clients and their APIs. Each client gets its own engine from the
 * platform's `HttpClientEngine` factory, so closing one never breaks the other.
 */
val networkModule =
    module {
        single<HttpClient>(TmdbClient) {
            val config = get<NetworkConfig>()
            createTmdbHttpClient(
                engine = get(),
                accessToken = config.tmdbAccessToken,
                logLevel = config.logLevel,
                logger = config.logger,
            )
        }
        single { TmdbApi(get(TmdbClient)) }

        // The BFF verifies the signed-in user's Firebase ID token and rejects requests without it;
        // the client adds it, with one forced-refresh retry on a 401.
        single<HttpClient>(BffClient) {
            val config = get<NetworkConfig>()
            createBffHttpClient(
                engine = get(),
                tokenProvider = get(),
                appVersion = config.appVersion,
                logLevel = config.logLevel,
                logger = config.logger,
            )
        }
        single { BffApi(get(BffClient)) }
    }

/** The shared repositories and what they run on. Repositories are app-wide singletons. */
val dataModule =
    module {
        // Injected rather than read statically so tests can control timestamps, e.g. watchlist ordering.
        factory<Clock> { Clock.System }
        factory<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }
        factory<WatchlistDao> { get<MovieGalleryDatabase>().watchlistDao() }

        single<MovieRepository> { MovieRepositoryImpl(get(), get(IoDispatcher)) }
        single<TvShowRepository> { TvShowRepositoryImpl(get(), get(IoDispatcher)) }
        single<CountryRepository> { CountryRepositoryImpl(get(), get(IoDispatcher)) }
        singleOf(::WatchlistRepositoryImpl) bind WatchlistRepository::class
    }

/** One use case per screen action; cheap wrappers, so a new one per injection. */
val domainModule =
    module {
        factoryOf(::GetCountryDetailsUseCase)
        factoryOf(::GetHomeSectionsUseCase)
        factoryOf(::GetMovieUseCase)
        factoryOf(::GetTopTvShowsUseCase)
        factoryOf(::IdentifyMoviePosterUseCase)
        factoryOf(::IsSignedInUseCase)
        factoryOf(::ObserveCurrentUserUseCase)
        factoryOf(::ObserveIsInWatchlistUseCase)
        factoryOf(::ObserveWatchlistMoviesUseCase)
        factoryOf(::SearchMoviesUseCase)
        factoryOf(::SendChatMessageUseCase)
        factoryOf(::SignInUseCase)
        factoryOf(::SignInWithGoogleUseCase)
        factoryOf(::SignOutUseCase)
        factoryOf(::SignUpUseCase)
        factoryOf(::ToggleWatchlistUseCase)
    }

/**
 * Everything the shared code wires itself. A platform loads these alongside its own modules,
 * which must supply:
 *
 * - `io.ktor.client.engine.HttpClientEngine`, as a **factory** (a new engine per client)
 * - [NetworkConfig]
 * - [MovieGalleryDatabase]
 * - `com.apollographql.apollo.ApolloClient`
 * - `AuthRepository`, `AuthTokenProvider` and `AiChatRepository`
 */
val sharedModules = listOf(networkModule, dataModule, domainModule)
