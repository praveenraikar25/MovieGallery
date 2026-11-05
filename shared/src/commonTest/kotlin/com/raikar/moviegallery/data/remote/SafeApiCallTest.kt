package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.auth.MissingAuthTokenException
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.testutil.fakeHttpServer
import com.raikar.moviegallery.testutil.tmdbApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * How [safeApiCall] turns failures into [AppError]s. Cases that depend on the HTTP stack are driven
 * by real HTTP outcomes from a scripted server, not by constructing the stack's exception types, so
 * they hold whichever stack the app uses.
 */
class SafeApiCallTest {
    private val server = fakeHttpServer()

    @AfterTest
    fun tearDown() = server.close()

    private fun errorFrom(throwable: Throwable): AppError {
        val result = safeApiCall<Unit> { throw throwable }
        return (result as DataResult.Failure).error
    }

    private suspend fun errorForResponse(
        status: Int,
        body: String = "",
    ): AppError {
        server.enqueue(status, body)
        val result = safeApiCall { tmdbApi(server).popular() }
        return (result as DataResult.Failure).error
    }

    @Test
    fun `wraps a successful value`() {
        assertEquals(DataResult.Success(42), safeApiCall { 42 })
    }

    @Test
    fun `401 maps to Unauthorized`() =
        runTest {
            assertEquals(AppError.Unauthorized, errorForResponse(401))
        }

    @Test
    fun `a missing auth token maps to AuthRequired rather than Network`() {
        assertEquals(AppError.AuthRequired, errorFrom(MissingAuthTokenException()))
    }

    @Test
    fun `404 maps to NotFound`() =
        runTest {
            assertEquals(AppError.NotFound, errorForResponse(404))
        }

    @Test
    fun `500 keeps its status`() =
        runTest {
            assertEquals(AppError.Http(500), errorForResponse(500))
        }

    @Test
    fun `422 keeps its status`() =
        runTest {
            // 422 is what TMDB returns for a blank search query.
            assertEquals(AppError.Http(422), errorForResponse(422))
        }

    @Test
    fun `a dropped connection maps to Network`() =
        runTest {
            server.failConnections()

            val result = safeApiCall { tmdbApi(server).popular() }

            assertEquals(DataResult.Failure(AppError.Network), result)
        }

    @Test
    fun `a malformed body maps to Serialization`() =
        runTest {
            assertEquals(AppError.Serialization, errorForResponse(200, """{"results": "not-an-array"}"""))
        }

    @Test
    fun `a non-JSON success body maps to Serialization`() =
        runTest {
            // What a captive portal answers with.
            assertEquals(AppError.Serialization, errorForResponse(200, "<html><body>Sign in to Wi-Fi</body></html>"))
        }

    @Test
    fun `anything else is Unknown and keeps the cause`() {
        val cause = IllegalStateException("boom")

        assertEquals(AppError.Unknown(cause), errorFrom(cause))
    }

    @Test
    fun `cancellation propagates instead of becoming an error state`() {
        // Swallowing this would make normal WhileSubscribed teardown look like a network failure.
        assertFailsWith<CancellationException> {
            safeApiCall<Unit> { throw CancellationException("scope torn down") }
        }
    }
}
