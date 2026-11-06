package com.raikar.moviegallery.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raikar.moviegallery.domain.usecase.ObserveWatchlistMoviesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WatchlistViewModel(
    observeWatchlistMovies: ObserveWatchlistMoviesUseCase,
) : ViewModel() {
    val uiState: StateFlow<WatchlistUiState> =
        observeWatchlistMovies()
            .map { movies -> WatchlistUiState(movies = movies) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchlistUiState())
}
