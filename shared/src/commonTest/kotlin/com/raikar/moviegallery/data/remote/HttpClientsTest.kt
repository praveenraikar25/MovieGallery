package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.testutil.FakeAuthTokenProvider
import com.raikar.moviegallery.testutil.asMovieListJson
import com.raikar.moviegallery.testutil.asTvShowListJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Behaviour of the client factories that needs no real server: log redaction and timeout mapping. */
class HttpClientsTest {
    private class CapturingLogger : Logger {
        val lines = mutableListOf<String>()

        override fun log(message: String) {
            lines += message
        }

        val text: String get() = lines.joinToString("\n")
    }

    private fun answering(body: String) = MockEngine { respond(body, HttpStatusCode.OK) }

    private fun failingWith(failure: Throwable) = MockEngine { throw failure }

    @Test
    fun `the TMDB token never reaches the log even when headers and bodies are logged`() =
        runTest {
            val logger = CapturingLogger()
            val client =
                createTmdbHttpClient(
                    engine = answering(listOf(1).asMovieListJson()),
                    accessToken = "tmdb-secret-token",
                    logLevel = LogLevel.ALL,
                    logger = logger,
                )

            TmdbApi(client).popular()

            assertTrue(logger.text.contains("Authorization", ignoreCase = true), "headers were not logged")
            assertFalse(logger.text.contains("tmdb-secret-token"), "the token was logged")
        }

    @Test
    fun `the BFF ID token never reaches the log even when headers and bodies are logged`() =
        runTest {
            val logger = CapturingLogger()
            val client =
                createBffHttpClient(
                    engine = answering(listOf(1).asTvShowListJson()),
                    tokenProvider = FakeAuthTokenProvider(cached = "bff-secret-id-token"),
                    appVersion = "1",
                    logLevel = LogLevel.ALL,
                    logger = logger,
                )

            BffApi(client).topTvShows()

            assertTrue(logger.text.contains("Authorization", ignoreCase = true), "headers were not logged")
            assertFalse(logger.text.contains("bff-secret-id-token"), "the token was logged")
        }

    @Test
    fun `each Ktor timeout maps to Network`() =
        runTest {
            val timeouts =
                listOf(
                    HttpRequestTimeoutException("https://api.themoviedb.org/3/movie/popular", 15_000L),
                    ConnectTimeoutException("connect timed out", null),
                    SocketTimeoutException("read timed out", null),
                )

            for (timeout in timeouts) {
                val api = TmdbApi(createTmdbHttpClient(engine = failingWith(timeout), accessToken = "t"))

                assertEquals(
                    DataResult.Failure(AppError.Network),
                    safeApiCall { api.popular() },
                    "for ${timeout::class.simpleName}",
                )
            }
        }
}
