package com.raikar.moviegallery.domain.model

/**
 * Formats a TMDB `vote_average` (a raw double like 7.383) to one decimal place,
 * rounding half up, with the current locale's decimal separator.
 *
 * Per platform because number formatting is: Android keeps the exact
 * `String.format` output it has always shown, iOS uses `NSNumberFormatter`.
 */
expect fun formatRating(rating: Double): String
