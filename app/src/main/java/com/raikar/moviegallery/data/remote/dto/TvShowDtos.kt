package com.raikar.moviegallery.data.remote.dto

import kotlinx.serialization.Serializable

/** Envelope returned by the BFF's `tv/top` endpoint. Fields are already camelCase. */
@Serializable
data class TvShowListResponseDto(
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalResults: Int = 0,
    val results: List<TvShowSummaryDto> = emptyList(),
)

@Serializable
data class TvShowSummaryDto(
    val id: Int,
    val name: String = "",
    val overview: String = "",
    /** May be absent or null — never assume 10 characters. */
    val firstAirDate: String? = null,
    val voteAverage: Double = 0.0,
    val posterUrl: String? = null,
    val genres: List<String> = emptyList(),
)
