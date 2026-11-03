package com.raikar.moviegallery.di

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import com.raikar.moviegallery.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

/**
 * The Countries GraphQL backend: public and unauthenticated, so it needs no qualifier while it
 * is the app's only Apollo client. Add one (as `@TmdbClient`/`@BffClient` do) if a second
 * GraphQL backend arrives.
 */
@Module
@InstallIn(SingletonComponent::class)
object GraphQlModule {
    const val COUNTRIES_URL = "https://countries.trevorblades.com/graphql"

    /** Derived from the shared base client so it reuses the connection pool and timeouts. */
    @Provides
    @Singleton
    fun provideApolloClient(base: OkHttpClient): ApolloClient =
        ApolloClient
            .Builder()
            .serverUrl(COUNTRIES_URL)
            .okHttpClient(
                base
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
