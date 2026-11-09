package com.raikar.moviegallery.ui.bridge

import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.usecase.ObserveWatchlistMoviesUseCase
import com.raikar.moviegallery.testutil.FakeWatchlistRepository
import com.raikar.moviegallery.ui.screens.watchlist.WatchlistUiState
import com.raikar.moviegallery.ui.screens.watchlist.WatchlistViewModel
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
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** What a SwiftUI screen relies on: watching a ViewModel's state, and ending its lifetime. */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelBridgeTest {
    // Both viewModelScope and the watcher run on Dispatchers.Main.
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
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

    private fun owner(repository: FakeWatchlistRepository) =
        ViewModelOwner { WatchlistViewModel(ObserveWatchlistMoviesUseCase(repository)) }

    private fun WatchlistUiState.ids() = movies.map { it.id }

    @Test
    fun `a watcher delivers the current state first and then each change`() =
        runTest {
            val repository = FakeWatchlistRepository(initial = listOf(movie(1)))
            val owner = owner(repository)
            val seen = mutableListOf<List<Int>>()

            val handle = StateFlowWatcher(owner.viewModel.uiState).watch { seen += it.ids() }
            repository.toggle(movie(2))

            // The empty list is stateIn's initial value, before the repository's first emission.
            assertEquals(listOf(emptyList(), listOf(1), listOf(1, 2)), seen)
            assertEquals(listOf(1, 2), StateFlowWatcher(owner.viewModel.uiState).value.ids())
            handle.cancel()
            owner.clear()
        }

    @Test
    fun `no values arrive after the watch is cancelled`() =
        runTest {
            val repository = FakeWatchlistRepository()
            val owner = owner(repository)
            val seen = mutableListOf<List<Int>>()
            // Keeps the ViewModel's stateIn subscribed, so its state keeps updating.
            val keepAlive = StateFlowWatcher(owner.viewModel.uiState).watch { }

            val handle = StateFlowWatcher(owner.viewModel.uiState).watch { seen += it.ids() }
            handle.cancel()
            repository.toggle(movie(1))

            assertFalse(handle.isActive)
            assertEquals(listOf(emptyList()), seen)
            assertEquals(
                listOf(1),
                owner.viewModel.uiState.value
                    .ids(),
            )
            keepAlive.cancel()
            owner.clear()
        }

    @Test
    fun `the owner returns the same ViewModel on every read`() {
        val owner = owner(FakeWatchlistRepository())

        assertSame(owner.viewModel, owner.viewModel)
        owner.clear()
    }

    @Test
    fun `clearing the owner cancels the ViewModel's coroutines`() =
        runTest {
            val repository = FakeWatchlistRepository()
            val owner = owner(repository)
            val seen = mutableListOf<List<Int>>()
            val handle = StateFlowWatcher(owner.viewModel.uiState).watch { seen += it.ids() }

            owner.clear()
            repository.toggle(movie(1))

            // viewModelScope is gone, so the stateIn coroutine no longer forwards changes.
            assertEquals(listOf(emptyList()), seen)
            assertTrue(owner.viewModel.uiState.value.isEmpty)
            handle.cancel()
        }
}
