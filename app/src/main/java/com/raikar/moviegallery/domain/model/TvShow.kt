package com.raikar.moviegallery.domain.model

/**
 * A TV show as it appears in a ranked list. Deliberately narrower than [Movie] —
 * nothing in the app needs a synopsis or artwork for a show yet.
 */
data class TvShow(
    val id: Int,
    val title: String,
    /** Null when the first air date is unknown, mirroring [Movie.year]. */
    val firstAirYear: Int?,
    val rating: Double,
    val genres: List<String>,
) {
    /** More than two genres ellipsizes to mush on a single-line row. */
    val genreLabel: String get() = genres.take(2).joinToString(" · ")

    val ratingLabel: String get() = "%.1f".format(rating)

    val hasRating: Boolean get() = rating > 0.0

    val yearLabel: String get() = firstAirYear?.toString() ?: "TBA"
}
