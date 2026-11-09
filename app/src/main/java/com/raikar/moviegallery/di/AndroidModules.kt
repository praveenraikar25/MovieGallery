package com.raikar.moviegallery.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import com.google.firebase.auth.FirebaseAuth
import com.raikar.moviegallery.BuildConfig
import com.raikar.moviegallery.data.remote.NetworkConfig
import com.raikar.moviegallery.data.remote.auth.FirebaseAuthTokenProvider
import com.raikar.moviegallery.data.repository.AuthRepositoryImpl
import com.raikar.moviegallery.domain.repository.AuthRepository
import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

private const val TIMEOUT_SECONDS = 15L

/**
 * The Countries GraphQL backend: public and unauthenticated, so it needs no qualifier while it
 * is the app's only Apollo client. Add one (as `TmdbClient`/`BffClient` do) if a second GraphQL
 * backend arrives.
 */
const val COUNTRIES_URL = "https://countries.trevorblades.com/graphql"

/** What the shared modules need from Android (see `sharedModules`), plus Android-only services. */
val androidPlatformModule =
    module {
        /*
         * Unauthenticated client, shared by Coil, Apollo and the Ktor engines. It carries no
         * interceptors: a client carrying the TMDB bearer token would leak that token to
         * image.tmdb.org on every poster fetch. Ktor adds each backend's headers per client.
         */
        single {
            OkHttpClient
                .Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
        }

        /*
         * A factory, not a single: each Ktor client gets its own engine, so closing one never
         * breaks the other. Every engine wraps the base client, so they all share its connection
         * pool and dispatcher.
         */
        factory<HttpClientEngine> { OkHttp.create { preconfigured = get<OkHttpClient>() } }

        /*
         * Headers and bodies in debug builds (Ktor's `ALL`), nothing in release. Ktor redacts
         * `Authorization` at every level. The BFF gets the version code in `X-APP-VERSION`.
         */
        single {
            NetworkConfig(
                tmdbAccessToken = BuildConfig.TMDB_READ_ACCESS_TOKEN,
                appVersion = BuildConfig.VERSION_CODE.toString(),
                logLevel = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE,
                logger = Logger.ANDROID,
            )
        }

        // Derived from the base client so it reuses the connection pool and timeouts.
        single {
            ApolloClient
                .Builder()
                .serverUrl(COUNTRIES_URL)
                .okHttpClient(
                    get<OkHttpClient>()
                        .newBuilder()
                        .addInterceptor(
                            HttpLoggingInterceptor().apply {
                                level =
                                    if (BuildConfig.DEBUG) {
                                        HttpLoggingInterceptor.Level.BODY
                                    } else {
                                        HttpLoggingInterceptor.Level.NONE
                                    }
                            },
                        ).build(),
                ).build()
        }

        // Uses the unauthenticated base client; posters need no credentials.
        single { imageLoader(androidContext(), get()) }

        single { FirebaseAuth.getInstance() }
        singleOf(::AuthRepositoryImpl) bind AuthRepository::class
        singleOf(::FirebaseAuthTokenProvider) bind AuthTokenProvider::class
    }

private fun imageLoader(
    context: Context,
    base: OkHttpClient,
): ImageLoader =
    ImageLoader
        .Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { base })) }
        .crossfade(true)
        .build()

/** Every Android module; `startKoin` loads these together with `sharedModules` (which has the ViewModels). */
val androidModules = listOf(androidPlatformModule, aiModule)
