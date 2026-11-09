package com.raikar.moviegallery.ui.screens.search

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.Movie

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = true,
    val results: List<Movie> = emptyList(),
    val error: AppError? = null,
) {
    /**
     * The loading branch matters: without it the label reads "0 results" against a
     * not-yet-populated list on every keystroke.
     */
    val headerLabel: String
        get() =
            when {
                isLoading -> "Searching…"
                error != null -> ""
                query.isBlank() -> "Popular now"
                else -> "${results.size} results"
            }
}
