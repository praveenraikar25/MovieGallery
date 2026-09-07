package com.raikar.moviegallery.ui.screens.detail

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.MovieDetail

data class MovieDetailUiState(
    val isLoading: Boolean = true,
    val movie: MovieDetail? = null,
    val isInWatchlist: Boolean = false,
    val error: AppError? = null,
)
