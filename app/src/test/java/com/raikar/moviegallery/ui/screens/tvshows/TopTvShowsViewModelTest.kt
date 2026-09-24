package com.raikar.moviegallery.ui.screens.tvshows

import app.cash.turbine.test
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.TvShow
import com.raikar.moviegallery.domain.model.TvShowPage
import com.raikar.moviegallery.domain.usecase.GetTopTvShowsUseCase
import com.raikar.moviegallery.testutil.FakeTvShowRepository
import com.raikar.moviegallery.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TopTvShowsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun show(id: Int) =
        TvShow(
            id = id,
            name = "Show $id",
            overview = "Overview $id",
            firstAirYear = 2020,
            rating = 8.0,
            posterUrl = null,
            genres = listOf("Drama"),
        )

    @Test
    fun `shows a loading state until the first emission arrives`() {
        assertTrue(TopTvShowsUiState().isLoading)
    }

    @Test
    fun `loads and shows the first page`() =
        runTest {
            val repository =
                FakeTvShowRepository(
                    pages =
                        mapOf(
                            1 to DataResult.Success(TvShowPage(shows = (1..10).map(::show), page = 1, totalPages = 2)),
                        ),
                )

            val viewModel = TopTvShowsViewModel(GetTopTvShowsUseCase(repository))

            viewModel.uiState.test {
                val loaded = awaitItem()
                assertFalse(loaded.isLoading)
                assertEquals((1..10).toList(), loaded.shows.map { it.id })
                assertEquals(1, loaded.page)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `loadNextPage appends the next page's shows`() =
        runTest {
            val repository =
                FakeTvShowRepository(
                    pages =
                        mapOf(
                            1 to DataResult.Success(TvShowPage(shows = listOf(show(1)), page = 1, totalPages = 2)),
                            2 to DataResult.Success(TvShowPage(shows = listOf(show(2)), page = 2, totalPages = 2)),
                        ),
                )

            val viewModel = TopTvShowsViewModel(GetTopTvShowsUseCase(repository))

            viewModel.uiState.test {
                awaitItem() // page 1 loaded

                viewModel.loadNextPage()

                val loaded = awaitItem()
                assertEquals(listOf(1, 2), loaded.shows.map { it.id })
                assertFalse(loaded.canLoadMore)

                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(2, repository.topTvShowsCalls)
        }

    @Test
    fun `loadNextPage is a no-op once every page has loaded`() =
        runTest {
            val repository =
                FakeTvShowRepository(
                    pages =
                        mapOf(
                            1 to DataResult.Success(TvShowPage(shows = listOf(show(1)), page = 1, totalPages = 1)),
                        ),
                )

            val viewModel = TopTvShowsViewModel(GetTopTvShowsUseCase(repository))
            viewModel.uiState.test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.loadNextPage()

            assertEquals(1, repository.topTvShowsCalls)
        }

    @Test
    fun `stops loading and stays empty when the repository has no shows`() =
        runTest {
            val repository =
                FakeTvShowRepository(
                    pages = mapOf(1 to DataResult.Success(TvShowPage(shows = emptyList(), page = 1, totalPages = 1))),
                )

            val viewModel = TopTvShowsViewModel(GetTopTvShowsUseCase(repository))

            viewModel.uiState.test {
                val loaded = awaitItem()
                assertFalse(loaded.isLoading)
                assertTrue(loaded.isEmpty)

                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(1, repository.topTvShowsCalls)
        }

    @Test
    fun `a failure surfaces as an error and retry re-requests the same page`() =
        runTest {
            var attempt = 0
            val repository =
                object : com.raikar.moviegallery.domain.repository.TvShowRepository {
                    override suspend fun topTvShows(page: Int): DataResult<TvShowPage> {
                        attempt++
                        return if (attempt == 1) {
                            DataResult.Failure(AppError.Network)
                        } else {
                            DataResult.Success(TvShowPage(shows = listOf(show(1)), page = 1, totalPages = 1))
                        }
                    }
                }

            val viewModel = TopTvShowsViewModel(GetTopTvShowsUseCase(repository))

            viewModel.uiState.test {
                val failed = awaitItem()
                assertFalse(failed.isLoading)
                assertEquals(AppError.Network, failed.error)

                viewModel.loadNextPage()

                val retried = awaitItem()
                assertEquals(listOf(1), retried.shows.map { it.id })
                assertEquals(null, retried.error)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
