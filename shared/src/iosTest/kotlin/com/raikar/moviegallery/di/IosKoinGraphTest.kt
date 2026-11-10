package com.raikar.moviegallery.di

import androidx.lifecycle.ViewModel
import androidx.room3.Room
import androidx.sqlite.driver.NativeSQLiteDriver
import com.apollographql.apollo.ApolloClient
import com.raikar.moviegallery.data.local.MovieGalleryDatabase
import com.raikar.moviegallery.data.local.applySharedConfig
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
import com.raikar.moviegallery.testutil.FakeFirebaseAiBridge
import com.raikar.moviegallery.testutil.FakeFirebaseAuthBridge
import com.raikar.moviegallery.ui.bridge.ViewModelOwner
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.dsl.onClose
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * The iOS counterpart to `:app`'s `KoinGraphResolutionTest`: builds the real iOS graph
 * (`sharedModules` plus `iosPlatformModule`) and resolves every use case and every ViewModel. On
 * iOS that is the only check there is, since Koin's `verify()` is JVM-only.
 *
 * Only the bridges are faked, plus two overrides that keep the test offline and free of files: a
 * `MockEngine` that records any request (none may happen) and an in-memory database. Main is a
 * never-advanced test dispatcher, so ViewModels' `init` loads never run.
 */
@OptIn(ExperimentalCoroutinesApi::class, KoinInternalApi::class)
class IosKoinGraphTest {
    private val requests = mutableListOf<String>()
    private var app: KoinApplication? = null

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() {
        app?.close()
        Dispatchers.resetMain()
        assertEquals(emptyList(), requests, "the graph made a network request")
    }

    private fun graphApp(): KoinApplication =
        koinApplication {
            allowOverride(true)
            modules(
                iosModules(CONFIG, FakeFirebaseAuthBridge(), FakeFirebaseAiBridge()) +
                    module {
                        factory<HttpClientEngine> {
                            MockEngine { request ->
                                requests += request.url.toString()
                                respondError(HttpStatusCode.ServiceUnavailable)
                            }
                        }
                        single<MovieGalleryDatabase> {
                            Room
                                .inMemoryDatabaseBuilder<MovieGalleryDatabase>()
                                .setDriver(NativeSQLiteDriver())
                                .setQueryCoroutineContext(Dispatchers.IO)
                                .applySharedConfig()
                                .build()
                        } onClose { it?.close() }
                    },
            )
        }.also { app = it }

    @Test
    fun `every use case resolves against the real graph`() {
        val koin = graphApp().koin

        useCases.forEach { type -> koin.get<Any>(type) }
    }

    @Test
    fun `every ViewModel resolves through the graph and clears through its owner`() {
        val graph = MovieGalleryGraph(graphApp().koin)
        val factories: List<() -> ViewModel> =
            listOf(
                graph::appSessionViewModel,
                graph::aiChatViewModel,
                graph::homeViewModel,
                graph::loginViewModel,
                { graph.movieDetailViewModel(movieId = 550) },
                graph::profileViewModel,
                graph::searchViewModel,
                graph::topTvShowsViewModel,
                graph::watchlistViewModel,
            )

        val resolved =
            factories.map { create ->
                val owner = ViewModelOwner(create)
                val viewModel = owner.viewModel
                assertSame(viewModel, owner.viewModel)
                owner.clear()
                viewModel::class
            }

        // A ViewModel added to viewModelModule without a MovieGalleryGraph function fails here.
        val registered = viewModelModule.mappings.values.map { it.beanDefinition.primaryType }
        assertEquals(registered.size, resolved.size)
        assertEquals(registered.toSet(), resolved.toSet())
    }

    @Test
    fun `the TMDB and BFF clients each get their own engine and close with the graph`() {
        val koin = graphApp().koin
        val tmdb = koin.get<HttpClient>(TmdbClient)
        val bff = koin.get<HttpClient>(BffClient)

        assertNotSame(tmdb.engine, bff.engine)

        app?.close()
        app = null

        assertFalse(tmdb.isActive, "TMDB client still open")
        assertFalse(bff.isActive, "BFF client still open")
    }

    @Test
    fun `the Apollo client resolves`() {
        graphApp().koin.get<ApolloClient>()
    }

    @Test
    fun `the unmodified engine factory gives a new Darwin engine each time`() {
        val koin =
            koinApplication { modules(iosModules(CONFIG, FakeFirebaseAuthBridge(), FakeFirebaseAiBridge())) }
                .also { app = it }
                .koin

        val first = koin.get<HttpClientEngine>()
        val second = koin.get<HttpClientEngine>()

        assertNotSame(first, second)
        assertIs<DarwinClientEngineConfig>(first.config)
        assertIs<DarwinClientEngineConfig>(second.config)
        first.close()
        second.close()
    }

    @Test
    fun `the config never prints the TMDB token`() {
        assertFalse("tmdb-test-token" in CONFIG.toString())
    }

    private val useCases: List<KClass<*>> =
        listOf(
            GetCountryDetailsUseCase::class,
            GetHomeSectionsUseCase::class,
            GetMovieUseCase::class,
            GetTopTvShowsUseCase::class,
            IdentifyMoviePosterUseCase::class,
            IsSignedInUseCase::class,
            ObserveCurrentUserUseCase::class,
            ObserveIsInWatchlistUseCase::class,
            ObserveWatchlistMoviesUseCase::class,
            SearchMoviesUseCase::class,
            SendChatMessageUseCase::class,
            SignInUseCase::class,
            SignInWithGoogleUseCase::class,
            SignOutUseCase::class,
            SignUpUseCase::class,
            ToggleWatchlistUseCase::class,
        )

    private companion object {
        val CONFIG = IosAppConfig(tmdbAccessToken = "tmdb-test-token", appVersion = "1", isDebug = false)
    }
}
