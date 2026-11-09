package com.raikar.moviegallery.ui.screens.tvshows

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.TvShow

data class TopTvShowsUiState(
    val shows: List<TvShow> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: AppError? = null,
) {
    val isEmpty: Boolean get() = shows.isEmpty() && !isLoading && error == null

    val canLoadMore: Boolean get() = page < totalPages
}
