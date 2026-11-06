package com.raikar.moviegallery.di

import com.raikar.moviegallery.data.remote.BffApi
import com.raikar.moviegallery.data.remote.NetworkConfig
import com.raikar.moviegallery.data.remote.TmdbApi
import com.raikar.moviegallery.data.remote.fixture
import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import com.raikar.moviegallery.testutil.FakeAuthTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Checks that each API gets the client behind its own qualifier. Koin's `verify()` sees only
 * types, so a swapped `TmdbClient`/`BffClient` would pass it but would send the TMDB token to the
 * BFF and BFF calls without the ID token (replace-hilt-with-koin design, decision 7).
 */
class SharedModulesTest {
    private class SeenRequest(
        val host: String,
        val headers: Map<String, List<String>>,
    )

    private val seen = mutableListOf<SeenRequest>()

    private val engine =
        MockEngine { request ->
            seen += SeenRequest(request.url.host, request.headers.entries().associate { it.key to it.value })
            val isTmdb = request.url.host == TMDB_HOST
            val body = fixture(if (isTmdb) "movie_list_popular.json" else "tv_show_list_top.json")
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

    // An isolated application, not startKoin: no global Koin state survives the test.
    private val koin: KoinApplication =
        koinApplication {
            modules(
                networkModule,
                module {
                    factory<HttpClientEngine> { engine }
                    single<AuthTokenProvider> { FakeAuthTokenProvider(cached = ID_TOKEN) }
                    single { NetworkConfig(tmdbAccessToken = TMDB_TOKEN, appVersion = "7") }
                },
            )
        }

    @AfterTest
    fun tearDown() = koin.close()

    @Test
    fun `TmdbApi sends the TMDB token to the TMDB host`() =
        runTest {
            koin.koin.get<TmdbApi>().popular()

            val request = seen.single()
            assertEquals(TMDB_HOST, request.host)
            assertEquals(listOf("Bearer $TMDB_TOKEN"), request.headers[HttpHeaders.Authorization])
        }

    @Test
    fun `BffApi sends the ID token to the BFF host and never the TMDB token`() =
        runTest {
            koin.koin.get<BffApi>().topTvShows()

            val request = seen.single()
            assertEquals(BFF_HOST, request.host)
            assertEquals(listOf("Bearer $ID_TOKEN"), request.headers[HttpHeaders.Authorization])
            assertFalse(
                request.headers.values
                    .flatten()
                    .any { TMDB_TOKEN in it },
                "TMDB token leaked to the BFF",
            )
        }

    @Test
    fun `closing the graph closes both clients but leaves the shared engine open`() {
        val tmdbClient = koin.koin.get<HttpClient>(TmdbClient)
        val bffClient = koin.koin.get<HttpClient>(BffClient)

        koin.close()

        assertFalse(tmdbClient.isActive, "TMDB client still open")
        assertFalse(bffClient.isActive, "BFF client still open")
        // On Android the engines wrap the OkHttpClient that Coil and Apollo share.
        assertTrue(engine.isActive, "engine was closed with its client")
    }

    private companion object {
        const val TMDB_TOKEN = "tmdb-test-token"
        const val ID_TOKEN = "firebase-id-token"
        val TMDB_HOST = Url(TmdbApi.BASE_URL).host
        val BFF_HOST = Url(BffApi.BASE_URL).host
    }
}
