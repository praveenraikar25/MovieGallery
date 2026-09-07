package com.raikar.moviegallery.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.raikar.moviegallery.BuildConfig
import com.raikar.moviegallery.data.remote.TmdbApi
import com.raikar.moviegallery.di.qualifier.TmdbClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val TIMEOUT_SECONDS = 15L

    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            // TMDB adds fields over time and returns endpoint-specific extras such as
            // the `dates` object on now_playing/upcoming.
            ignoreUnknownKeys = true
            // Turns an explicit JSON null into the property's declared default.
            coerceInputValues = true
        }

    /**
     * Unauthenticated client, safe to share with Coil. Interceptors apply to every
     * request regardless of host, so a client carrying the TMDB bearer token would
     * leak that token to image.tmdb.org on every poster fetch.
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
     * Derived with [OkHttpClient.newBuilder] so it shares the base client's
     * connection pool and dispatcher.
     */
    @Provides
    @Singleton
    @TmdbClient
    fun provideTmdbOkHttpClient(base: OkHttpClient): OkHttpClient =
        base
            .newBuilder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain
                        .request()
                        .newBuilder()
                        .header("Authorization", "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}")
                        .header("Accept", "application/json")
                        .build(),
                )
            }.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level =
                        if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BODY
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
                    // Never print the token, even at BODY level.
                    redactHeader("Authorization")
                },
            ).build()

    @Provides
    @Singleton
    fun provideRetrofit(
        @TmdbClient client: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(TmdbApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideTmdbApi(retrofit: Retrofit): TmdbApi = retrofit.create(TmdbApi::class.java)

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
