package com.raikar.moviegallery.ui.screens.home

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.model.MovieSection
import com.raikar.moviegallery.domain.usecase.GetHomeSectionsUseCase
import com.raikar.moviegallery.testutil.FakeMovieRepository
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
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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
    fun `loads sections successfully on init`() =
        runTest {
            val repository =
                FakeMovieRepository(
                    sectionsResult = DataResult.Success(listOf(MovieSection("Popular", listOf(movie(1))))),
                )

            val viewModel = HomeViewModel(GetHomeSectionsUseCase(repository))

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(1, state.sections.size)
            assertEquals("Popular", state.sections.single().title)
            assertNull(state.error)
        }

    @Test
    fun `emits error state when loading fails`() =
        runTest {
            val repository = FakeMovieRepository(sectionsResult = DataResult.Failure(AppError.Network))

            val viewModel = HomeViewModel(GetHomeSectionsUseCase(repository))

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.sections.isEmpty())
            assertEquals(AppError.Network, state.error)
        }

    @Test
    fun `retry reloads sections and clears the previous error`() =
        runTest {
            val repository = FakeMovieRepository(sectionsResult = DataResult.Failure(AppError.Network))
            val viewModel = HomeViewModel(GetHomeSectionsUseCase(repository))
            assertEquals(AppError.Network, viewModel.uiState.value.error)

            repository.sectionsResult =
                DataResult.Success(listOf(MovieSection("Popular", listOf(movie(1)))))
            viewModel.retry()

            val state = viewModel.uiState.value
            assertNull(state.error)
            assertEquals(1, state.sections.size)
        }
}
