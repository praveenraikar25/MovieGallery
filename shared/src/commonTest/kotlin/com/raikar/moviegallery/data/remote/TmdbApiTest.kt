package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.testutil.fakeHttpServer
import com.raikar.moviegallery.testutil.tmdbApi
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Exercises [TmdbApi] against a scripted HTTP server, so query parameters, the auth header and
 * error propagation are all verified end to end.
 */
class TmdbApiTest {
    private val server = fakeHttpServer()

    @AfterTest
    fun tearDown() = server.close()

    @Test
    fun `popular sends the bearer token and default query params`() =
        runTest {
            server.enqueue(200, fixture("movie_list_popular.json"))

            val response = tmdbApi(server).popular()

            assertEquals(3, response.results.size)

            val request = server.takeRequest()
            assertEquals("Bearer test-token", request.header("Authorization"))
            assertEquals("application/json", request.header("Accept"))
            assertEquals("/3/movie/popular", request.path)
            assertEquals("en-US", request.queryParameter("language"))
            assertEquals("1", request.queryParameter("page"))
            // Auth is header-based; sending api_key too would be wrong.
            assertNull(request.queryParameter("api_key"))
        }

    @Test
    fun `search encodes the query and excludes adult results`() =
        runTest {
            server.enqueue(200, fixture("movie_list_popular.json"))

            tmdbApi(server).searchMovies(query = "space odyssey")

            val request = server.takeRequest()
            assertEquals("/3/search/movie", request.path)
            assertEquals("space odyssey", request.queryParameter("query"))
            assertEquals("false", request.queryParameter("include_adult"))
        }

    @Test
    fun `detail requests credits in the same call`() =
        runTest {
            server.enqueue(200, fixture("movie_detail_with_credits.json"))

            val detail = tmdbApi(server).movieDetail(movieId = 1061474)

            assertEquals(3, detail.credits?.cast?.size)

            val request = server.takeRequest()
            assertEquals("/3/movie/1061474", request.path)
            assertEquals("credits", request.queryParameter("append_to_response"))
        }

    @Test
    fun `a 401 surfaces as Unauthorized through safeApiCall`() =
        runTest {
            server.enqueue(401, """{"status_code":7,"status_message":"Invalid API key"}""")

            val result = safeApiCall { tmdbApi(server, token = "").popular() }

            assertEquals(DataResult.Failure(AppError.Unauthorized), result)
        }

    @Test
    fun `a 404 surfaces as NotFound through safeApiCall`() =
        runTest {
            server.enqueue(
                404,
                """{"status_code":34,"status_message":"The resource you requested could not be found."}""",
            )

            val result = safeApiCall { tmdbApi(server).movieDetail(movieId = 1) }

            assertEquals(DataResult.Failure(AppError.NotFound), result)
        }

    @Test
    fun `a malformed body surfaces as Serialization`() =
        runTest {
            server.enqueue(200, """{"results": "not-an-array"}""")

            val result = safeApiCall { tmdbApi(server).popular() }

            assertEquals(DataResult.Failure(AppError.Serialization), result)
        }

    @Test
    fun `unknown response fields are ignored`() =
        runTest {
            // The `dates` object only now_playing and upcoming return.
            server.enqueue(200, fixture("movie_list_upcoming.json"))

            val result = safeApiCall { tmdbApi(server).upcoming() }

            assertIs<DataResult.Success<*>>(result)
        }
}
