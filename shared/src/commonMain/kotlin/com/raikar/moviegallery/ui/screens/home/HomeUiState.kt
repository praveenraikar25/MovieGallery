package com.raikar.moviegallery.ui.screens.home

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.MovieSection

data class HomeUiState(
    val isLoading: Boolean = true,
    val sections: List<MovieSection> = emptyList(),
    val error: AppError? = null,
)
