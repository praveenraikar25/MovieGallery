package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Exercises the Retrofit interface against a real HTTP server, so query
 * parameters and error propagation are verified end to end.
 */
class BffApiTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private val server get() = serverRule.server

    private fun api(): BffApi =
        Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(testJson.asConverterFactory("application/json; charset=utf-8".toMediaType()))
            .build()
            .create(BffApi::class.java)

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
    fun `topTvShows sends the requested page`() =
        runTest {
            enqueue(200, fixture("tv_show_list_top.json"))

            val response = api().topTvShows(page = 2)

            assertEquals(3, response.results.size)

            val request = server.takeRequest()
            assertEquals("/tv/top", request.url.encodedPath)
            assertEquals("2", request.url.queryParameter("page"))
        }

    @Test
    fun `a non-200 response surfaces as a Failure through safeApiCall`() =
        runTest {
            enqueue(500, """{"error":"internal error"}""")

            val result = safeApiCall { api().topTvShows(page = 1) }

            assertEquals(DataResult.Failure(AppError.Http(500)), result)
        }
}
