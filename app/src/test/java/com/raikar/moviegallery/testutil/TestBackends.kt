package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.data.remote.BffApi
import com.raikar.moviegallery.data.remote.TmdbApi
import com.raikar.moviegallery.data.remote.createBffHttpClient
import com.raikar.moviegallery.data.remote.createTmdbHttpClient
import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import io.ktor.client.engine.okhttp.OkHttp

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
): TmdbApi =
    TmdbApi(
        createTmdbHttpClient(
            engine = OkHttp.create(),
            accessToken = token,
            baseUrl = "${server.origin}/3/",
        ),
    )

/** [BffApi] pointed at [server], authenticating with [tokenProvider] and treating [bffHost] as the BFF. */
fun bffApi(
    server: FakeHttpServer,
    tokenProvider: AuthTokenProvider = FakeAuthTokenProvider(),
    bffHost: String = server.host,
): BffApi =
    BffApi(
        createBffHttpClient(
            engine = OkHttp.create(),
            tokenProvider = tokenProvider,
            appVersion = "1",
            baseUrl = "${server.origin}/",
            bffHost = bffHost,
        ),
    )
