package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.testutil.bffApi
import com.raikar.moviegallery.testutil.fakeHttpServer
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BffApiTest {
    private val server = fakeHttpServer()

    @AfterTest
    fun tearDown() = server.close()

    @Test
    fun `topTvShows sends the requested page`() =
        runTest {
            // A page that is neither the requested one nor the DTO default, so decoding is what's checked.
            server.enqueue(200, fixture("tv_show_list_top.json").replace("\"page\": 1", "\"page\": 3"))

            val response = bffApi(server).topTvShows(page = 2)

            assertEquals(3, response.results.size)
            assertEquals(3, response.page)

            val request = server.takeRequest()
            assertEquals("/tv/top", request.path)
            assertEquals("2", request.queryParameter("page"))
        }

    @Test
    fun `a non-200 response surfaces as a Failure through safeApiCall`() =
        runTest {
            server.enqueue(500, """{"error":"internal error"}""")

            val result = safeApiCall { bffApi(server).topTvShows(page = 1) }

            assertEquals(DataResult.Failure(AppError.Http(500)), result)
        }
}
