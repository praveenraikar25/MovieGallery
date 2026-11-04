package com.raikar.moviegallery.domain.model

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterRoundHalfUp

// Matches Android's "%.1f": exactly one fraction digit, half-up rounding, no grouping.
private val ratingFormatter =
    NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        minimumFractionDigits = 1u
        maximumFractionDigits = 1u
        roundingMode = NSNumberFormatterRoundHalfUp
        usesGroupingSeparator = false
    }

actual fun formatRating(rating: Double): String = ratingFormatter.stringFromNumber(NSNumber(rating)).orEmpty()
