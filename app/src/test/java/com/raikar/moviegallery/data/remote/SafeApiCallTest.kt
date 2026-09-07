package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SafeApiCallTest {
    private fun httpError(code: Int): HttpException =
        HttpException(
            Response.error<Unit>(code, "".toResponseBody("application/json".toMediaType())),
        )

    private fun errorFrom(throwable: Throwable): AppError {
        val result = safeApiCall<Unit> { throw throwable }
        return (result as DataResult.Failure).error
    }

    @Test
    fun `wraps a successful value`() {
        assertEquals(DataResult.Success(42), safeApiCall { 42 })
    }

    @Test
    fun `401 maps to Unauthorized`() {
        assertEquals(AppError.Unauthorized, errorFrom(httpError(401)))
    }

    @Test
    fun `404 maps to NotFound`() {
        assertEquals(AppError.NotFound, errorFrom(httpError(404)))
    }

    @Test
    fun `other http codes keep their status`() {
        assertEquals(AppError.Http(500), errorFrom(httpError(500)))
        // 422 is what TMDB returns for a blank search query.
        assertEquals(AppError.Http(422), errorFrom(httpError(422)))
    }

    @Test
    fun `IOException maps to Network`() {
        assertEquals(AppError.Network, errorFrom(IOException("offline")))
    }

    @Test
    fun `SerializationException maps to Serialization`() {
        assertEquals(AppError.Serialization, errorFrom(SerializationException("bad shape")))
    }

    @Test
    fun `anything else is Unknown and keeps the cause`() {
        val cause = IllegalStateException("boom")

        assertEquals(AppError.Unknown(cause), errorFrom(cause))
    }

    @Test
    fun `cancellation propagates instead of becoming an error state`() {
        try {
            safeApiCall<Unit> { throw CancellationException("scope torn down") }
            fail("CancellationException should be rethrown, not converted to a Failure")
        } catch (expected: CancellationException) {
            // Swallowing this would make normal WhileSubscribed teardown look like
            // a network failure.
        }
    }
}
