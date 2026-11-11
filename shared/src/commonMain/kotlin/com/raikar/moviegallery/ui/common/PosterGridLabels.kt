package com.raikar.moviegallery.ui.common

import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.model.TvShow

/**
 * What a poster grid tile displays, independent of whether it came from a [Movie] or a [TvShow].
 * Shared so Android's `PosterGridCard` and iOS's `PosterGridTile` show the same text.
 */
data class PosterGridItem(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val subtitle: String,
    /** "8.5"-style rating, or null for unrated (0.0) titles. */
    val ratingLabel: String?,
)

fun Movie.toPosterGridItem() =
    PosterGridItem(
        id = id,
        title = title,
        posterUrl = posterUrl,
        subtitle = gridSubtitle(year, genreLabel),
        ratingLabel = ratingLabel.takeIf { hasRating },
    )

fun TvShow.toPosterGridItem() =
    PosterGridItem(
        id = id,
        title = name,
        posterUrl = posterUrl,
        subtitle = gridSubtitle(firstAirYear, genreLabel),
        ratingLabel = ratingLabel.takeIf { hasRating },
    )

/** Joins only the parts the backend actually returned, e.g. "2011 · Drama"; "TBA" when there are none. */
internal fun gridSubtitle(
    year: Int?,
    genreLabel: String,
): String =
    listOfNotNull(
        year?.toString(),
        genreLabel.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { "TBA" }
