package com.raikar.moviegallery.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Envelope shared by `movie/popular`, `movie/top_rated`, `movie/now_playing`,
 * `movie/upcoming` and `search/movie`. The last two also return a top-level
 * `dates` object, which `ignoreUnknownKeys` drops.
 */
@Serializable
data class MovieListResponseDto(
    val page: Int = 1,
    val results: List<MovieSummaryDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
data class MovieSummaryDto(
    val id: Int,
    val title: String = "",
    /** May be absent, null, or the empty string — never assume 10 characters. */
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    /** List endpoints only give ids; names come from [com.raikar.moviegallery.data.remote.TmdbGenres]. */
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
)

/** `movie/{id}?append_to_response=credits`. */
@Serializable
data class MovieDetailDto(
    val id: Int,
    val title: String = "",
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    /** Unlike the list endpoints, detail returns fully resolved genre names. */
    val genres: List<GenreDto> = emptyList(),
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    /** Nullable, and sometimes 0 for films with no known runtime. */
    val runtime: Int? = null,
    val tagline: String? = null,
    /** Present only because of `append_to_response=credits`. */
    val credits: CreditsDto? = null,
)

@Serializable
data class GenreDto(
    val id: Int,
    val name: String = "",
)

@Serializable
data class CreditsDto(
    val cast: List<CastMemberDto> = emptyList(),
)

@Serializable
data class CastMemberDto(
    val id: Int = 0,
    val name: String = "",
    val character: String = "",
    val order: Int = 0,
)
