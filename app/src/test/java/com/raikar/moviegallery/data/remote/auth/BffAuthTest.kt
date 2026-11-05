package com.raikar.moviegallery.data.remote.auth

import com.raikar.moviegallery.data.remote.safeApiCall
import com.raikar.moviegallery.data.repository.TvShowRepositoryImpl
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.testutil.FakeAuthTokenProvider
import com.raikar.moviegallery.testutil.asTvShowListJson
import com.raikar.moviegallery.testutil.bffApi
import com.raikar.moviegallery.testutil.fakeHttpServer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The BFF token handling, observed from outside: what reaches the server and what the caller gets
 * back. Backs the tv-shows requirements on the bearer token, the no-token case and the single
 * refresh retry.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BffAuthTest {
    private val server = fakeHttpServer()

    @AfterTest
    fun tearDown() = server.close()

    private fun enqueueShows() = server.enqueue(200, listOf(1).asTvShowListJson())

    private suspend fun call(
        provider: FakeAuthTokenProvider,
        bffHost: String = server.host,
    ) = safeApiCall { bffApi(server, provider, bffHost).topTvShows(page = 1) }

    @Test
    fun `sends the token as a bearer Authorization header`() =
        runTest {
            enqueueShows()

            call(FakeAuthTokenProvider())

            assertEquals("Bearer cached", server.takeRequest().header("Authorization"))
        }

    @Test
    fun `sends no request when there is no token`() =
        runTest {
            val result = call(FakeAuthTokenProvider(cached = null))

            assertEquals(DataResult.Failure(AppError.AuthRequired), result)
            assertEquals(0, server.requestCount)
        }

    @Test
    fun `a 401 is retried once with a refreshed token`() =
        runTest {
            server.enqueue(401)
            enqueueShows()
            val provider = FakeAuthTokenProvider()

            val result = call(provider)

            assertIs<DataResult.Success<*>>(result)
            assertEquals("Bearer cached", server.takeRequest().header("Authorization"))
            assertEquals("Bearer refreshed", server.takeRequest().header("Authorization"))
            assertEquals(listOf(false, true), provider.forceRefreshCalls)
        }

    @Test
    fun `a persistent 401 is surfaced after a single retry`() =
        runTest {
            server.enqueue(401)
            server.enqueue(401)
            enqueueShows()

            val result = call(FakeAuthTokenProvider())

            assertEquals(DataResult.Failure(AppError.Unauthorized), result)
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `a 401 is surfaced without a retry when no refreshed token exists`() =
        runTest {
            server.enqueue(401)

            val result = call(FakeAuthTokenProvider(refreshed = null))

            assertEquals(DataResult.Failure(AppError.Unauthorized), result)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `a 401 from another host is not retried`() =
        runTest {
            server.enqueue(401)
            enqueueShows()
            val provider = FakeAuthTokenProvider()

            val result = call(provider, bffHost = "bff.example")

            assertEquals(DataResult.Failure(AppError.Unauthorized), result)
            assertEquals(1, server.requestCount)
            assertEquals(listOf(false), provider.forceRefreshCalls)
        }

    @Test
    fun `Retry after a failure looks up the token again`() =
        runTest {
            server.enqueue(500)
            enqueueShows()
            val provider = FakeAuthTokenProvider()
            val repository = TvShowRepositoryImpl(bffApi(server, provider), UnconfinedTestDispatcher())

            repository.topTvShows(page = 1)
            repository.topTvShows(page = 1)

            assertEquals(listOf(false, false), provider.forceRefreshCalls)
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `a missing token sends no request and surfaces as AuthRequired`() =
        runTest {
            val repository =
                TvShowRepositoryImpl(bffApi(server, FakeAuthTokenProvider(cached = null)), UnconfinedTestDispatcher())

            val result = repository.topTvShows(page = 1)

            assertEquals(DataResult.Failure(AppError.AuthRequired), result)
            assertEquals(0, server.requestCount)
        }
}
