package com.raikar.moviegallery.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locale-independent on purpose: the decimal separator follows the device locale, so
 * these check the digits and that exactly one separator sits before the last digit.
 */
class RatingFormatTest {
    private fun digits(rating: Double): String {
        val label = formatRating(rating)
        val separators = label.filterNot { it.isDigit() }
        assertEquals(1, separators.length, "one separator in \"$label\"")
        assertEquals(label.length - 2, label.indexOf(separators[0]), "one fraction digit in \"$label\"")
        return label.filter { it.isDigit() }
    }

    @Test
    fun `rounds to one fraction digit`() {
        assertEquals("74", digits(7.383))
        assertEquals("85", digits(8.46))
    }

    @Test
    fun `rounds half up`() {
        // 7.25 and 0.75 are exact in binary, so this is a true tie rather than float noise.
        assertEquals("73", digits(7.25))
        assertEquals("08", digits(0.75))
    }

    @Test
    fun `keeps a trailing zero and whole tens`() {
        assertEquals("00", digits(0.0))
        assertEquals("100", digits(10.0))
    }
}
