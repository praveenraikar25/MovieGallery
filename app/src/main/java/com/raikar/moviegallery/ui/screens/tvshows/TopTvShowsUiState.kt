package com.raikar.moviegallery.ui.screens.tvshows

import com.raikar.moviegallery.domain.model.TvShow

data class TopTvShowsUiState(
    val shows: List<TvShow> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = shows.isEmpty()
}
