package com.raikar.moviegallery.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.CastMember
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.domain.usecase.GetMovieUseCase
import com.raikar.moviegallery.domain.usecase.ObserveIsInWatchlistUseCase
import com.raikar.moviegallery.domain.usecase.ToggleWatchlistUseCase
import com.raikar.moviegallery.testutil.FakeMovieRepository
import com.raikar.moviegallery.testutil.FakeWatchlistRepository
import com.raikar.moviegallery.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val movie =
        Movie(
            id = 42,
            title = "Movie 42",
            year = 2024,
            rating = 7.0,
            genres = listOf("Drama"),
            synopsis = "",
            posterUrl = null,
            backdropUrl = null,
        )

    private val movieDetail =
        MovieDetail(
            summary = movie,
            runtimeMinutes = 118,
            tagline = "A tagline",
            cast = listOf(CastMember(name = "Someone", role = "Someone Else")),
        )

    private fun viewModel(
        movieRepository: FakeMovieRepository = FakeMovieRepository(movieDetailResult = DataResult.Success(movieDetail)),
        watchlistRepository: FakeWatchlistRepository = FakeWatchlistRepository(),
    ) = MovieDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("movieId" to movie.id)),
        getMovie = GetMovieUseCase(movieRepository),
        observeIsInWatchlist = ObserveIsInWatchlistUseCase(watchlistRepository),
        toggleWatchlist = ToggleWatchlistUseCase(watchlistRepository),
    )

    @Test
    fun `loads the movie detail on init`() =
        runTest {
            val vm = viewModel()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals(movieDetail, state.movie)
            assertNull(state.error)
        }

    @Test
    fun `emits error state when loading fails`() =
        runTest {
            val vm =
                viewModel(
                    movieRepository = FakeMovieRepository(movieDetailResult = DataResult.Failure(AppError.NotFound)),
                )

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.movie)
            assertEquals(AppError.NotFound, state.error)
        }

    @Test
    fun `retry reloads the movie detail and clears the previous error`() =
        runTest {
            val movieRepository = FakeMovieRepository(movieDetailResult = DataResult.Failure(AppError.NotFound))
            val vm = viewModel(movieRepository = movieRepository)
            assertEquals(AppError.NotFound, vm.uiState.value.error)

            movieRepository.movieDetailResult = DataResult.Success(movieDetail)
            vm.retry()

            val state = vm.uiState.value
            assertNull(state.error)
            assertEquals(movieDetail, state.movie)
        }

    @Test
    fun `reflects watchlist membership and updates when it changes`() =
        runTest {
            val watchlistRepository = FakeWatchlistRepository()
            val vm = viewModel(watchlistRepository = watchlistRepository)

            vm.uiState.test {
                assertFalse(awaitItem().isInWatchlist)

                watchlistRepository.toggle(movie)
                assertTrue(awaitItem().isInWatchlist)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onToggleWatchlist toggles the current movie in the watchlist repository`() =
        runTest {
            val watchlistRepository = FakeWatchlistRepository()
            val vm = viewModel(watchlistRepository = watchlistRepository)

            vm.onToggleWatchlist(movieDetail)

            assertEquals(listOf(movie), watchlistRepository.toggleCalls)
        }
}
