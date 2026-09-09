package com.raikar.moviegallery.ui.screens.search

import app.cash.turbine.test
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.usecase.SearchMoviesUseCase
import com.raikar.moviegallery.testutil.FakeMovieRepository
import com.raikar.moviegallery.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Matches the `QUERY_DEBOUNCE_MS` constant private to [SearchViewModel]. */
private const val QUERY_DEBOUNCE_MS = 500L

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private fun movie(id: Int) =
        Movie(
            id = id,
            title = "Movie $id",
            year = 2024,
            rating = 7.0,
            genres = listOf("Drama"),
            synopsis = "",
            posterUrl = null,
            backdropUrl = null,
        )

    @Test
    fun `shows popular movies for a blank query`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeMovieRepository(popularResult = DataResult.Success(listOf(movie(1))))
            val viewModel = SearchViewModel(SearchMoviesUseCase(repository))

            viewModel.uiState.test {
                assertTrue(awaitItem().isLoading)

                advanceTimeBy(QUERY_DEBOUNCE_MS + 1)
                val loaded = awaitItem()
                assertEquals(listOf(1), loaded.results.map { it.id })
                assertEquals("", loaded.query)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onQueryChange echoes the query immediately then debounces the search`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository =
                FakeMovieRepository(
                    popularResult = DataResult.Success(emptyList()),
                    searchResult = DataResult.Success(listOf(movie(9))),
                )
            val viewModel = SearchViewModel(SearchMoviesUseCase(repository))

            viewModel.uiState.test {
                awaitItem() // initial loading state
                advanceTimeBy(QUERY_DEBOUNCE_MS + 1)
                awaitItem() // popular results for the blank query

                viewModel.onQueryChange("matrix")

                // The query echoes into state right away, before the debounced search runs.
                val echoed = awaitItem()
                assertEquals("matrix", echoed.query)

                advanceTimeBy(QUERY_DEBOUNCE_MS + 1)
                assertTrue(awaitItem().isLoading)

                val results = awaitItem()
                assertEquals("matrix", results.query)
                assertEquals(listOf(9), results.results.map { it.id })
                assertEquals("matrix", repository.lastSearchQuery)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits an error when the search fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository =
                FakeMovieRepository(
                    popularResult = DataResult.Success(emptyList()),
                    searchResult = DataResult.Failure(AppError.Network),
                )
            val viewModel = SearchViewModel(SearchMoviesUseCase(repository))

            viewModel.uiState.test {
                awaitItem() // initial loading state
                advanceTimeBy(QUERY_DEBOUNCE_MS + 1)
                awaitItem() // popular results for the blank query

                viewModel.onQueryChange("matrix")
                awaitItem() // echoed query

                advanceTimeBy(QUERY_DEBOUNCE_MS + 1)
                awaitItem() // loading
                val errored = awaitItem()
                assertEquals(AppError.Network, errored.error)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `retry re-runs the current query`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository =
                FakeMovieRepository(
                    popularResult = DataResult.Success(emptyList()),
                    searchResult = DataResult.Failure(AppError.Network),
                )
            val viewModel = SearchViewModel(SearchMoviesUseCase(repository))

            viewModel.uiState.test {
                awaitItem() // initial loading state
                advanceTimeBy(QUERY_DEBOUNCE_MS + 1)
                awaitItem() // popular results for the blank query

                viewModel.onQueryChange("matrix")
                awaitItem() // echoed query
                advanceTimeBy(QUERY_DEBOUNCE_MS + 1)
                awaitItem() // loading
                val errored = awaitItem()
                assertEquals(AppError.Network, errored.error)

                repository.searchResult = DataResult.Success(listOf(movie(9)))
                viewModel.retry()

                assertTrue(awaitItem().isLoading)
                val recovered = awaitItem()
                assertEquals(listOf(9), recovered.results.map { it.id })

                cancelAndIgnoreRemainingEvents()
            }
        }
}
