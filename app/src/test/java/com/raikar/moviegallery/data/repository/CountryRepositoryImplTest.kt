package com.raikar.moviegallery.data.repository

import com.apollographql.apollo.ApolloClient
import com.raikar.moviegallery.data.remote.fixture
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Runs the real Apollo client against a local server, so the query, parsing and mapping are all covered. */
@OptIn(ExperimentalCoroutinesApi::class)
class CountryRepositoryImplTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private val server get() = serverRule.server

    private fun repository(url: String = server.url("/graphql").toString()) =
        CountryRepositoryImpl(
            apollo = ApolloClient.Builder().serverUrl(url).build(),
            ioDispatcher = UnconfinedTestDispatcher(),
        )

    private fun enqueue(
        body: String,
        code: Int = 200,
    ) {
        server.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build(),
        )
    }

    @Test
    fun `maps a known country`() =
        runTest {
            enqueue(fixture("country_jp.json"))

            val details = (repository().getCountryDetails("JP") as DataResult.Success).data!!

            assertEquals("Japan", details.name)
            assertEquals("日本", details.nativeName)
            assertEquals("Tokyo", details.capital)
            assertEquals("Asia", details.continent)
            assertEquals(listOf("Japanese"), details.languages)
            assertEquals(listOf("JPY"), details.currencies)
            assertTrue(
                server
                    .takeRequest()
                    .body!!
                    .utf8()
                    .contains("\"code\":\"JP\""),
            )
        }

    @Test
    fun `an unknown code is a success with no details`() =
        runTest {
            enqueue(fixture("country_null.json"))

            assertEquals(DataResult.Success(null), repository().getCountryDetails("SU"))
        }

    @Test
    fun `a second lookup of the same code is served from memory`() =
        runTest {
            enqueue(fixture("country_jp.json"))
            val repository = repository()

            repository.getCountryDetails("JP")
            val second = repository.getCountryDetails("jp")

            assertEquals("Japan", (second as DataResult.Success).data?.name)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `an unknown code is cached too`() =
        runTest {
            enqueue(fixture("country_null.json"))
            val repository = repository()

            repository.getCountryDetails("SU")
            assertNull((repository.getCountryDetails("SU") as DataResult.Success).data)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `a failure is not cached`() =
        runTest {
            enqueue("", code = 500)
            enqueue(fixture("country_jp.json"))
            val repository = repository()

            assertEquals(DataResult.Failure(AppError.Http(500)), repository.getCountryDetails("JP"))
            assertTrue(repository.getCountryDetails("JP") is DataResult.Success)
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `an unreachable server maps to Network`() =
        runTest {
            // Port 1 refuses connections on any normal machine.
            val result = repository(url = "http://127.0.0.1:1/graphql").getCountryDetails("JP")

            assertEquals(DataResult.Failure(AppError.Network), result)
        }
}
