package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Exercises the Retrofit interface against a real HTTP server, so query
 * parameters, the auth header and error propagation are all verified end to end.
 */
class TmdbApiTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private val server get() = serverRule.server

    private fun api(token: String = "test-token"): TmdbApi {
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

        return Retrofit
            .Builder()
            .baseUrl(server.url("/3/"))
            .client(client)
            .addConverterFactory(testJson.asConverterFactory("application/json; charset=utf-8".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
    }

    private fun enqueue(
        code: Int,
        body: String = "",
    ) {
        server.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .body(body)
                .build(),
        )
    }

    @Test
    fun `popular sends the bearer token and default query params`() =
        runTest {
            enqueue(200, fixture("movie_list_popular.json"))

            val response = api().popular()

            assertEquals(3, response.results.size)

            val request = server.takeRequest()
            assertEquals("Bearer test-token", request.headers["Authorization"])
            assertEquals("application/json", request.headers["Accept"])
            assertEquals("/3/movie/popular", request.url.encodedPath)
            assertEquals("en-US", request.url.queryParameter("language"))
            assertEquals("1", request.url.queryParameter("page"))
            // Auth is header-based; sending api_key too would be wrong.
            assertEquals(null, request.url.queryParameter("api_key"))
        }

    @Test
    fun `search encodes the query and excludes adult results`() =
        runTest {
            enqueue(200, fixture("movie_list_popular.json"))

            api().searchMovies(query = "space odyssey")

            val request = server.takeRequest()
            assertEquals("/3/search/movie", request.url.encodedPath)
            assertEquals("space odyssey", request.url.queryParameter("query"))
            assertEquals("false", request.url.queryParameter("include_adult"))
        }

    @Test
    fun `detail requests credits in the same call`() =
        runTest {
            enqueue(200, fixture("movie_detail_with_credits.json"))

            val detail = api().movieDetail(movieId = 1061474)

            assertEquals(3, detail.credits?.cast?.size)

            val request = server.takeRequest()
            assertEquals("/3/movie/1061474", request.url.encodedPath)
            assertEquals("credits", request.url.queryParameter("append_to_response"))
        }

    @Test
    fun `a 401 surfaces as Unauthorized through safeApiCall`() =
        runTest {
            enqueue(401, """{"status_code":7,"status_message":"Invalid API key"}""")

            val result = safeApiCall { api(token = "").popular() }

            assertEquals(DataResult.Failure(AppError.Unauthorized), result)
        }

    @Test
    fun `a 404 surfaces as NotFound through safeApiCall`() =
        runTest {
            enqueue(404, """{"status_code":34,"status_message":"The resource you requested could not be found."}""")

            val result = safeApiCall { api().movieDetail(movieId = 1) }

            assertEquals(DataResult.Failure(AppError.NotFound), result)
        }

    @Test
    fun `a malformed body surfaces as Serialization`() =
        runTest {
            enqueue(200, """{"results": "not-an-array"}""")

            val result = safeApiCall { api().popular() }

            assertEquals(DataResult.Failure(AppError.Serialization), result)
        }

    @Test
    fun `unknown response fields are ignored`() =
        runTest {
            // The `dates` object only now_playing and upcoming return.
            enqueue(200, fixture("movie_list_upcoming.json"))

            val result = safeApiCall { api().upcoming() }

            assertTrue(result is DataResult.Success)
        }
}
