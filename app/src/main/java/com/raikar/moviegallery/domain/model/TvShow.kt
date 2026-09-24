package com.raikar.moviegallery.domain.model

/**
 * A TV show as it appears in a ranked list.
 *
 * [posterUrl] is a complete URL, mirroring [Movie.posterUrl] — the data layer owns
 * image-path resolution so the UI never has to know about it.
 */
data class TvShow(
    val id: Int,
    val name: String,
    val overview: String,
    /** Null when the first air date is unknown, mirroring [Movie.year]. */
    val firstAirYear: Int?,
    val rating: Double,
    val posterUrl: String?,
    val genres: List<String>,
) {
    /** More than two genres ellipsizes to mush on a single-line row. */
    val genreLabel: String get() = genres.take(2).joinToString(" · ")

    val ratingLabel: String get() = "%.1f".format(rating)

    val hasRating: Boolean get() = rating > 0.0

    val yearLabel: String get() = firstAirYear?.toString() ?: "TBA"
}
