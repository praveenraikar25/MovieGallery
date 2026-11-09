package com.raikar.moviegallery.ui.screens.detail

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.CountryDetails
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.domain.model.ProductionCountry

data class MovieDetailUiState(
    val isLoading: Boolean = true,
    val movie: MovieDetail? = null,
    val isInWatchlist: Boolean = false,
    val error: AppError? = null,
    /** The open country details sheet, or null when it is closed. */
    val countrySheet: CountrySheetState? = null,
)

/**
 * One country's details sheet. [country] is the tapped chip, whose flag and name head the sheet
 * until [details] arrive and in the not-found and error states.
 */
data class CountrySheetState(
    val country: ProductionCountry,
    val isLoading: Boolean = true,
    val details: CountryDetails? = null,
    /** The source has no record of this code; shown without a Retry. */
    val notFound: Boolean = false,
    val error: AppError? = null,
)
