package com.raikar.moviegallery.ui.common

import com.raikar.moviegallery.domain.model.CountryDetails
import kotlin.test.Test
import kotlin.test.assertEquals

class CountryDetailRowsTest {
    private val japan =
        CountryDetails(
            code = "JP",
            name = "Japan",
            nativeName = "日本",
            flag = "🇯🇵",
            capital = "Tokyo",
            continent = "Asia",
            languages = listOf("Japanese"),
            currencies = listOf("JPY"),
        )

    @Test
    fun `rows are capital then continent then languages then currency`() {
        assertEquals(
            listOf(
                "Capital" to "Tokyo",
                "Continent" to "Asia",
                "Languages" to "Japanese",
                "Currency" to "JPY",
            ),
            countryDetailRows(japan),
        )
    }

    @Test
    fun `several languages and currencies are joined with a comma`() {
        val rows =
            countryDetailRows(
                japan.copy(languages = listOf("Dutch", "French"), currencies = listOf("USD", "USN", "USS")),
            ).toMap()

        assertEquals("Dutch, French", rows["Languages"])
        assertEquals("USD, USN, USS", rows["Currency"])
    }

    @Test
    fun `missing values show a dash`() {
        val rows =
            countryDetailRows(
                japan.copy(capital = null, continent = null, languages = emptyList(), currencies = emptyList()),
            ).toMap()

        assertEquals("—", rows["Capital"])
        assertEquals("—", rows["Continent"])
        assertEquals("—", rows["Languages"])
        assertEquals("—", rows["Currency"])
    }
}
