package com.raikar.moviegallery.ui.components

import com.raikar.moviegallery.domain.model.ProductionCountry
import org.junit.Assert.assertEquals
import org.junit.Test

class CountryChipLabelTest {
    @Test
    fun `label is the flag followed by the name`() {
        assertEquals("🇰🇷 South Korea", countryChipLabel(ProductionCountry("KR", "South Korea")))
    }

    @Test
    fun `a code without a flag shows the name alone`() {
        assertEquals("Soviet Union", countryChipLabel(ProductionCountry("SUN", "Soviet Union")))
    }
}
