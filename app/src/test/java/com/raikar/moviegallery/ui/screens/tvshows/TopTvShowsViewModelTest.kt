package com.raikar.moviegallery.ui.screens.tvshows

import app.cash.turbine.test
import com.raikar.moviegallery.domain.model.TvShow
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
            title = "Show $id",
            firstAirYear = 2020,
            rating = 8.0,
            genres = listOf("Drama"),
        )

    @Test
    fun `shows a loading state until the first emission arrives`() {
        assertTrue(TopTvShowsUiState().isLoading)
    }

    @Test
    fun `emits the repository's shows in rank order`() =
        runTest {
            val repository = FakeTvShowRepository(shows = (1..10).map(::show))

            val viewModel = TopTvShowsViewModel(GetTopTvShowsUseCase(repository))

            viewModel.uiState.test {
                // The rule's UnconfinedTestDispatcher runs init {} eagerly, so the
                // first state a collector sees is already the loaded one.
                val loaded = awaitItem()
                assertFalse(loaded.isLoading)
                assertEquals((1..10).toList(), loaded.shows.map { it.id })

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `stops loading and stays empty when the repository has no shows`() =
        runTest {
            val repository = FakeTvShowRepository()

            val viewModel = TopTvShowsViewModel(GetTopTvShowsUseCase(repository))

            viewModel.uiState.test {
                val loaded = awaitItem()
                assertFalse(loaded.isLoading)
                assertTrue(loaded.isEmpty)

                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(1, repository.topTvShowsCalls)
        }
}
