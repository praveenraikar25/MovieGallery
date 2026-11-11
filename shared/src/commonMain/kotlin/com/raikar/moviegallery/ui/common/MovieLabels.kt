package com.raikar.moviegallery.ui.common

import com.raikar.moviegallery.domain.model.Movie

/**
 * Card subtitles, built by joining only the parts TMDB actually returned — an
 * unreleased film has no year and reports a 0.0 rating, and some titles carry no
 * genres at all. Shared so both apps' movie cards read the same.
 */
val Movie.cardSubtitle: String
    get() =
        listOfNotNull(
            "★ $ratingLabel".takeIf { hasRating },
            genreLabel.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifBlank { yearLabel }
