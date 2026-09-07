package com.raikar.moviegallery.ui.screens.watchlist

import com.raikar.moviegallery.domain.model.Movie

data class WatchlistUiState(
    val movies: List<Movie> = emptyList(),
) {
    val isEmpty: Boolean get() = movies.isEmpty()
}
