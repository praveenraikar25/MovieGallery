package com.raikar.moviegallery.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.raikar.moviegallery.BuildConfig
import com.raikar.moviegallery.data.remote.BffApi
import com.raikar.moviegallery.data.remote.TmdbApi
import com.raikar.moviegallery.data.remote.createBffHttpClient
import com.raikar.moviegallery.data.remote.createTmdbHttpClient
import com.raikar.moviegallery.di.qualifier.BffClient
import com.raikar.moviegallery.di.qualifier.TmdbClient
import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val TIMEOUT_SECONDS = 15L

    /**
     * Unauthenticated client, shared by Coil, Apollo and the Ktor engines. It carries no
     * interceptors: a client carrying the TMDB bearer token would leak that token to
     * image.tmdb.org on every poster fetch. Ktor adds each backend's headers per client.
     */
    @Provides
    @Singleton
    fun provideBaseOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    /**
     * Headers and bodies in debug builds (Ktor's `ALL`, what OkHttp's `BODY` level logged), nothing in
     * release. Ktor redacts `Authorization` at every level.
     */
    private val logLevel: LogLevel get() = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE

    /**
     * Each Ktor client gets its own engine, but every engine wraps the base client, so they all share
     * its connection pool and dispatcher.
     */
    private fun engineOn(base: OkHttpClient): HttpClientEngine = OkHttp.create { preconfigured = base }

    @Provides
    @Singleton
    @TmdbClient
    fun provideTmdbHttpClient(base: OkHttpClient): HttpClient =
        createTmdbHttpClient(
            engine = engineOn(base),
            accessToken = BuildConfig.TMDB_READ_ACCESS_TOKEN,
            logLevel = logLevel,
            logger = Logger.ANDROID,
        )

    @Provides
    @Singleton
    fun provideTmdbApi(
        @TmdbClient client: HttpClient,
    ): TmdbApi = TmdbApi(client)

    /**
     * The BFF verifies the signed-in user's Firebase ID token (`Authorization: Bearer`) and rejects
     * requests without it; the client adds it, with one forced-refresh retry on a 401. The app
     * version code goes in `X-APP-VERSION`.
     */
    @Provides
    @Singleton
    @BffClient
    fun provideBffHttpClient(
        base: OkHttpClient,
        tokenProvider: AuthTokenProvider,
    ): HttpClient =
        createBffHttpClient(
            engine = engineOn(base),
            tokenProvider = tokenProvider,
            appVersion = BuildConfig.VERSION_CODE.toString(),
            logLevel = logLevel,
            logger = Logger.ANDROID,
        )

    @Provides
    @Singleton
    fun provideBffApi(
        @BffClient client: HttpClient,
    ): BffApi = BffApi(client)

    /** Uses the unauthenticated client; posters need no credentials. */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        base: OkHttpClient,
    ): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { base })) }
            .crossfade(true)
            .build()
}
