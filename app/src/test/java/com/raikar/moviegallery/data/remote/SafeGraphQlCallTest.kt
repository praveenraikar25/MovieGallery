package com.raikar.moviegallery.data.remote

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloOfflineException
import com.apollographql.apollo.exception.ApolloParseException
import com.raikar.moviegallery.data.remote.graphql.countries.CountryDetailsQuery
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SafeGraphQlCallTest {
    private val query = CountryDetailsQuery(code = "JP")
    private val data = CountryDetailsQuery.Data(country = null)

    private fun response(
        data: CountryDetailsQuery.Data? = null,
        exception: ApolloException? = null,
        errors: List<Error>? = null,
    ): ApolloResponse<CountryDetailsQuery.Data> =
        ApolloResponse
            .Builder(query, UUID.randomUUID())
            .data(data)
            .exception(exception)
            .errors(errors)
            .build()

    private fun errorFrom(response: ApolloResponse<CountryDetailsQuery.Data>): AppError =
        (safeGraphQlCall { response } as DataResult.Failure).error

    private fun httpError(code: Int) =
        ApolloHttpException(statusCode = code, headers = emptyList(), body = null, message = "")

    @Test
    fun `data is a success`() {
        assertEquals(DataResult.Success(data), safeGraphQlCall { response(data = data) })
    }

    @Test
    fun `data alongside GraphQL errors is a failure`() {
        // A failed nullable field arrives as null plus an error; it must not read as "no record".
        val error = errorFrom(response(data = data, errors = listOf(Error.Builder("country failed").build())))

        assertTrue(error is AppError.Unknown)
    }

    @Test
    fun `network and offline failures map to Network`() {
        assertEquals(AppError.Network, errorFrom(response(exception = ApolloNetworkException("down"))))
        assertEquals(AppError.Network, errorFrom(response(exception = ApolloOfflineException())))
    }

    @Test
    fun `HTTP statuses map like the REST stacks`() {
        assertEquals(AppError.Unauthorized, errorFrom(response(exception = httpError(401))))
        assertEquals(AppError.NotFound, errorFrom(response(exception = httpError(404))))
        assertEquals(AppError.Http(500), errorFrom(response(exception = httpError(500))))
    }

    @Test
    fun `an unreadable body maps to Serialization`() {
        assertEquals(AppError.Serialization, errorFrom(response(exception = ApolloParseException("bad json"))))
    }

    @Test
    fun `GraphQL errors without data map to Unknown`() {
        val error = errorFrom(response(errors = listOf(Error.Builder("Unknown field").build())))

        assertTrue(error is AppError.Unknown)
    }

    @Test
    fun `a thrown exception maps to Unknown`() {
        val error =
            (
                safeGraphQlCall<CountryDetailsQuery.Data> {
                    throw IllegalStateException("boom")
                } as DataResult.Failure
            ).error

        assertTrue(error is AppError.Unknown)
    }

    @Test
    fun `cancellation is rethrown, not reported as a failure`() {
        try {
            safeGraphQlCall<CountryDetailsQuery.Data> { throw CancellationException("scope cancelled") }
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertEquals("scope cancelled", e.message)
        }
    }
}
