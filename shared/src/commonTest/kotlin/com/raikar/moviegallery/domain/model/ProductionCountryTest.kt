package com.raikar.moviegallery.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProductionCountryTest {
    private fun flag(code: String) = ProductionCountry(code = code, name = "Somewhere").flag

    @Test
    fun `two-letter code becomes its flag`() {
        assertEquals("🇰🇷", flag("KR"))
        assertEquals("🇺🇸", flag("US"))
    }

    @Test
    fun `lowercase code is accepted`() {
        assertEquals("🇰🇷", flag("kr"))
    }

    @Test
    fun `code that is not two letters has no flag`() {
        assertNull(flag("XYZ"))
        assertNull(flag(""))
        assertNull(flag("K1"))
        assertNull(flag("K"))
    }

    @Test
    fun `a letter that uppercases to two letters has no flag`() {
        assertNull(flag("ß"))
    }
}
