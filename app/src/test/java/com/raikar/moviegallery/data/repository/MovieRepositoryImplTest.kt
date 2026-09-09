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
class MovieRepositoryImplTest {
    private val api = FakeTmdbApi()

    private fun repository() = MovieRepositoryImpl(api, UnconfinedTestDispatcher())

    private fun httpError(code: Int): Nothing =
        throw HttpException(
            Response.error<Unit>(code, "".toResponseBody("application/json".toMediaType())),
        )

    @Test
    fun `all four sections load in order when every endpoint succeeds`() =
        runTest {
            val result = repository().getSections()

            assertTrue(result is DataResult.Success)
            assertEquals(
                listOf("Trending Now", "Now Playing", "Top Rated", "Coming Soon"),
                (result as DataResult.Success).data.map { it.title },
            )
        }

    @Test
    fun `one failing endpoint costs only its own section`() =
        runTest {
            api.upcomingResponse = { throw IOException("offline") }

            val result = repository().getSections()

            val sections = (result as DataResult.Success).data
            assertEquals(listOf("Trending Now", "Now Playing", "Top Rated"), sections.map { it.title })
        }

    @Test
    fun `an empty section is dropped rather than rendered as a blank rail`() =
        runTest {
            api.popularResponse = { emptyList<Int>().asResponse() }

            val sections = (repository().getSections() as DataResult.Success).data

            assertTrue(sections.none { it.title == "Trending Now" })
            assertEquals(3, sections.size)
        }

    @Test
    fun `failure is only reported when every endpoint fails`() =
        runTest {
            api.popularResponse = { throw IOException("offline") }
            api.topRatedResponse = { throw IOException("offline") }
            api.nowPlayingResponse = { throw IOException("offline") }
            api.upcomingResponse = { throw IOException("offline") }

            val result = repository().getSections()

            assertEquals(DataResult.Failure(AppError.Network), result)
        }

    @Test
    fun `a total failure reports the underlying error, not a generic one`() =
        runTest {
            api.popularResponse = { httpError(401) }
            api.topRatedResponse = { httpError(401) }
            api.nowPlayingResponse = { httpError(401) }
            api.upcomingResponse = { httpError(401) }

            assertEquals(
                DataResult.Failure(AppError.Unauthorized),
                repository().getSections(),
            )
        }

    @Test
    fun `sections map through to domain movies`() =
        runTest {
            val sections = (repository().getSections() as DataResult.Success).data

            assertEquals(listOf(1), sections.first().movies.map { it.id })
            assertEquals(
                "Movie 1",
                sections
                    .first()
                    .movies
                    .first()
                    .title,
            )
        }

    @Test
    fun `search passes the query through and maps results`() =
        runTest {
            var received: String? = null
            api.searchResponse = { query ->
                received = query
                listOf(7, 8).asResponse()
            }

            val result = repository().searchMovies("blade runner")

            assertEquals("blade runner", received)
            assertEquals(listOf(7, 8), (result as DataResult.Success).data.map { it.id })
        }

    @Test
    fun `a missing movie surfaces as NotFound`() =
        runTest {
            api.detailResponse = { httpError(404) }

            assertEquals(
                DataResult.Failure(AppError.NotFound),
                repository().getMovieDetail(99),
            )
        }

    @Test
    fun `detail maps through to the domain model`() =
        runTest {
            val result = repository().getMovieDetail(1061474)

            assertEquals(1061474, (result as DataResult.Success).data.summary.id)
        }

    @Test
    fun `popular movies are exposed for the search screen's empty state`() =
        runTest {
            api.popularResponse = { listOf(11, 12).asResponse() }

            val result = repository().getPopularMovies()

            assertEquals(listOf(11, 12), (result as DataResult.Success).data.map { it.id })
        }
}
