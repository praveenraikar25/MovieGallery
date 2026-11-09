package com.raikar.moviegallery.ui.screens.watchlist

import app.cash.turbine.test
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.usecase.ObserveWatchlistMoviesUseCase
import com.raikar.moviegallery.testutil.FakeWatchlistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    // viewModelScope runs on Dispatchers.Main, which tests replace with a test dispatcher.
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

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
    fun `starts empty when the watchlist is empty`() =
        runTest {
            val repository = FakeWatchlistRepository()

            val viewModel = WatchlistViewModel(ObserveWatchlistMoviesUseCase(repository))

            viewModel.uiState.test {
                assertTrue(awaitItem().isEmpty)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `reflects the watchlist repository's initial movies`() =
        runTest {
            val repository = FakeWatchlistRepository(initial = listOf(movie(1), movie(2)))

            val viewModel = WatchlistViewModel(ObserveWatchlistMoviesUseCase(repository))

            viewModel.uiState.test {
                assertEquals(listOf(1, 2), awaitItem().movies.map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `updates when the repository's watchlist changes`() =
        runTest {
            val repository = FakeWatchlistRepository()
            val viewModel = WatchlistViewModel(ObserveWatchlistMoviesUseCase(repository))

            viewModel.uiState.test {
                assertTrue(awaitItem().isEmpty)

                repository.toggle(movie(1))
                assertEquals(listOf(1), awaitItem().movies.map { it.id })

                repository.toggle(movie(1))
                assertTrue(awaitItem().isEmpty)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
