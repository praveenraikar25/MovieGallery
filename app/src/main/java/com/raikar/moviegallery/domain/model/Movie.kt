package com.raikar.moviegallery.domain.model

/**
 * A movie as it appears in a list. TMDB's list endpoints carry no runtime and no
 * cast, so anything detail-only lives on [MovieDetail] rather than being nullable
 * here.
 *
 * [posterUrl] / [backdropUrl] are complete URLs — the data layer decides which
 * TMDB image size to request so the UI never has to know about image paths.
 */
data class Movie(
    val id: Int,
    val title: String,
    /** Null when TMDB has no release date yet, which is common for upcoming films. */
    val year: Int?,
    val rating: Double,
    val genres: List<String>,
    val synopsis: String,
    val posterUrl: String?,
    val backdropUrl: String?,
) {
    /** TMDB often lists 3+ genres; more than two ellipsizes to mush on a card. */
    val genreLabel: String get() = genres.take(2).joinToString(" · ")

    /** `vote_average` is a raw double like 7.383. */
    val ratingLabel: String get() = "%.1f".format(rating)

    /** False for unreleased films, where TMDB reports 0.0 rather than "unknown". */
    val hasRating: Boolean get() = rating > 0.0

    val yearLabel: String get() = year?.toString() ?: "TBA"
}

data class MovieDetail(
    val summary: Movie,
    /** Null or absent for films TMDB has no runtime for. */
    val runtimeMinutes: Int?,
    val tagline: String?,
    val cast: List<CastMember>,
) {
    /** Joins only the parts TMDB actually gave us, e.g. "2024 · 118 min · Drama". */
    val metadataLabel: String
        get() =
            listOfNotNull(
                summary.year?.toString(),
                runtimeMinutes?.takeIf { it > 0 }?.let { "$it min" },
                summary.genreLabel.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
}

data class CastMember(
    val name: String,
    val role: String,
) {
    val initial: String get() = name.firstOrNull()?.uppercase() ?: "?"
}
