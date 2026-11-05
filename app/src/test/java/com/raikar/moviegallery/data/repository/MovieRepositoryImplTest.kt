package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.testutil.asMovieListJson
import com.raikar.moviegallery.testutil.fakeHttpServer
import com.raikar.moviegallery.testutil.movieDetailJson
import com.raikar.moviegallery.testutil.tmdbApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MovieRepositoryImplTest {
    private val server = fakeHttpServer()

    private fun repository() = MovieRepositoryImpl(tmdbApi(server), UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = server.close()

    /** Answers the four Home rails, each with one movie: popular 1, top rated 2, now playing 3, upcoming 4. */
    private fun enqueueAllSections(except: String? = null) {
        SECTION_PATHS.forEachIndexed { index, path ->
            if (path != except) server.enqueue(200, listOf(index + 1).asMovieListJson(), path)
        }
    }

    @Test
    fun `all four sections load in order when every endpoint succeeds`() =
        runTest {
            enqueueAllSections()

            val result = repository().getSections()

            assertIs<DataResult.Success<*>>(result)
            assertEquals(
                listOf("Trending Now", "Now Playing", "Top Rated", "Coming Soon"),
                (result as DataResult.Success).data.map { it.title },
            )
        }

    @Test
    fun `one failing endpoint costs only its own section`() =
        runTest {
            enqueueAllSections(except = UPCOMING)
            server.failConnections(UPCOMING)

            val result = repository().getSections()

            val sections = (result as DataResult.Success).data
            assertEquals(listOf("Trending Now", "Now Playing", "Top Rated"), sections.map { it.title })
        }

    @Test
    fun `an empty section is dropped rather than rendered as a blank rail`() =
        runTest {
            enqueueAllSections(except = POPULAR)
            server.enqueue(200, emptyList<Int>().asMovieListJson(), POPULAR)

            val sections = (repository().getSections() as DataResult.Success).data

            assertTrue(sections.none { it.title == "Trending Now" })
            assertEquals(3, sections.size)
        }

    @Test
    fun `failure is only reported when every endpoint fails`() =
        runTest {
            server.failConnections()

            val result = repository().getSections()

            assertEquals(DataResult.Failure(AppError.Network), result)
        }

    @Test
    fun `a total failure reports the underlying error rather than a generic one`() =
        runTest {
            SECTION_PATHS.forEach { server.enqueue(401, path = it) }

            assertEquals(
                DataResult.Failure(AppError.Unauthorized),
                repository().getSections(),
            )
        }

    @Test
    fun `sections map through to domain movies`() =
        runTest {
            enqueueAllSections()

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
            server.enqueue(200, listOf(7, 8).asMovieListJson())

            val result = repository().searchMovies("blade runner")

            assertEquals("blade runner", server.takeRequest().queryParameter("query"))
            assertEquals(listOf(7, 8), (result as DataResult.Success).data.map { it.id })
        }

    @Test
    fun `a missing movie surfaces as NotFound`() =
        runTest {
            server.enqueue(404)

            assertEquals(
                DataResult.Failure(AppError.NotFound),
                repository().getMovieDetail(99),
            )
        }

    @Test
    fun `detail maps through to the domain model`() =
        runTest {
            server.enqueue(200, movieDetailJson(1061474))

            val result = repository().getMovieDetail(1061474)

            assertEquals(1061474, (result as DataResult.Success).data.summary.id)
        }

    @Test
    fun `popular movies are exposed for the search screen's empty state`() =
        runTest {
            server.enqueue(200, listOf(11, 12).asMovieListJson(), POPULAR)

            val result = repository().getPopularMovies()

            assertEquals(listOf(11, 12), (result as DataResult.Success).data.map { it.id })
        }

    private companion object {
        const val POPULAR = "/3/movie/popular"
        const val UPCOMING = "/3/movie/upcoming"

        /** In the order of [enqueueAllSections]'s movie ids. */
        val SECTION_PATHS = listOf(POPULAR, "/3/movie/top_rated", "/3/movie/now_playing", UPCOMING)
    }
}
