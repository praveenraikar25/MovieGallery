package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TvShowRepositoryImplTest {
    private val api = FakeBffApi()

    private fun repository() = TvShowRepositoryImpl(api, UnconfinedTestDispatcher())

    @Test
    fun `a successful call maps the response into a TvShowPage`() =
        runTest {
            api.topTvShowsResponse = { page -> listOf(1, 2, 3).asTvShowResponse(page = page, totalPages = 5) }

            val result = repository().topTvShows(page = 1)

            assertTrue(result is DataResult.Success)
            val page = (result as DataResult.Success).data
            assertEquals(1, page.page)
            assertEquals(5, page.totalPages)
            assertEquals(listOf(1, 2, 3), page.shows.map { it.id })
        }

    @Test
    fun `requests the given page`() =
        runTest {
            var requestedPage: Int? = null
            api.topTvShowsResponse = { page ->
                requestedPage = page
                listOf(1).asTvShowResponse(page = page)
            }

            repository().topTvShows(page = 3)

            assertEquals(3, requestedPage)
        }

    @Test
    fun `an HTTP error surfaces through safeApiCall as a Failure`() =
        runTest {
            api.topTvShowsResponse = {
                throw HttpException(Response.error<Unit>(500, "".toResponseBody("application/json".toMediaType())))
            }

            val result = repository().topTvShows(page = 1)

            assertEquals(DataResult.Failure(AppError.Http(500)), result)
        }

    @Test
    fun `a network failure surfaces as Network`() =
        runTest {
            api.topTvShowsResponse = { throw IOException("offline") }

            val result = repository().topTvShows(page = 1)

            assertEquals(DataResult.Failure(AppError.Network), result)
        }
}
