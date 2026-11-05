package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.data.remote.BffApi
import com.raikar.moviegallery.data.remote.TmdbApi
import com.raikar.moviegallery.data.remote.auth.BffAuthInterceptor
import com.raikar.moviegallery.data.remote.auth.BffTokenAuthenticator
import com.raikar.moviegallery.data.remote.testJson
import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/*
 * The only test file that knows which HTTP stack the app uses. Every API and repository test
 * builds its subject through these factories, so swapping the stack changes this file and no
 * test (share-network-layer-ktor design, decisions 1 and 2).
 */

/** A fresh scripted server; close it after the test. */
fun fakeHttpServer(): FakeHttpServer = MockWebServerHttpServer()

/** [TmdbApi] pointed at [server] under `/3/`, sending [token] the way the app does. */
fun tmdbApi(
    server: FakeHttpServer,
    token: String = "test-token",
): TmdbApi {
    val client =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain
                        .request()
                        .newBuilder()
                        .header("Authorization", "Bearer $token")
                        .header("Accept", "application/json")
                        .build(),
                )
            }.build()
    return retrofit(client, "${server.origin}/3/").create(TmdbApi::class.java)
}

/** [BffApi] pointed at [server], authenticating with [tokenProvider] and treating [bffHost] as the BFF. */
fun bffApi(
    server: FakeHttpServer,
    tokenProvider: AuthTokenProvider = FakeAuthTokenProvider(),
    bffHost: String = server.host,
): BffApi {
    val client =
        OkHttpClient
            .Builder()
            .addInterceptor(BffAuthInterceptor(tokenProvider))
            .authenticator(BffTokenAuthenticator(tokenProvider, bffHost))
            .build()
    return retrofit(client, "${server.origin}/").create(BffApi::class.java)
}

private fun retrofit(
    client: OkHttpClient,
    baseUrl: String,
): Retrofit =
    Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(testJson.asConverterFactory("application/json; charset=utf-8".toMediaType()))
        .build()
