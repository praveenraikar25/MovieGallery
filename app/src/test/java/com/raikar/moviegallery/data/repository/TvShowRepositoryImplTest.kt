package com.raikar.moviegallery.data.repository

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

@OptIn(ExperimentalCoroutinesApi::class)
class TvShowRepositoryImplTest {
    private val server = fakeHttpServer()

    private fun repository(tokenProvider: FakeAuthTokenProvider = FakeAuthTokenProvider()) =
        TvShowRepositoryImpl(bffApi(server, tokenProvider), UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = server.close()

    @Test
    fun `a successful call maps the response into a TvShowPage`() =
        runTest {
            server.enqueue(200, listOf(1, 2, 3).asTvShowListJson(page = 1, totalPages = 5))

            val result = repository().topTvShows(page = 1)

            assertIs<DataResult.Success<*>>(result)
            val page = (result as DataResult.Success).data
            assertEquals(1, page.page)
            assertEquals(5, page.totalPages)
            assertEquals(listOf(1, 2, 3), page.shows.map { it.id })
        }

    @Test
    fun `requests the given page`() =
        runTest {
            server.enqueue(200, listOf(1).asTvShowListJson(page = 3))

            repository().topTvShows(page = 3)

            assertEquals("3", server.takeRequest().queryParameter("page"))
        }

    @Test
    fun `an HTTP error surfaces through safeApiCall as a Failure`() =
        runTest {
            server.enqueue(500)

            val result = repository().topTvShows(page = 1)

            assertEquals(DataResult.Failure(AppError.Http(500)), result)
        }

    @Test
    fun `a network failure surfaces as Network`() =
        runTest {
            server.failConnections()

            val result = repository().topTvShows(page = 1)

            assertEquals(DataResult.Failure(AppError.Network), result)
        }

    @Test
    fun `a rejected token surfaces as AuthRequired`() =
        runTest {
            // Rejected, then rejected again after the one refresh retry.
            server.enqueue(401)
            server.enqueue(401)

            val result = repository().topTvShows(page = 1)

            assertEquals(DataResult.Failure(AppError.AuthRequired), result)
        }

    @Test
    fun `a missing token surfaces as AuthRequired`() =
        runTest {
            val result = repository(FakeAuthTokenProvider(cached = null)).topTvShows(page = 1)

            assertEquals(DataResult.Failure(AppError.AuthRequired), result)
        }
}
