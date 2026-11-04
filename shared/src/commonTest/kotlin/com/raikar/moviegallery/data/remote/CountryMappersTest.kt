package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.graphql.countries.CountryDetailsQuery
import com.raikar.moviegallery.domain.model.CountryDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CountryMappersTest {
    private fun country(
        code: String = "US",
        name: String = "United States",
        native: String = "United States",
        emoji: String = "🇺🇸",
        capital: String? = "Washington D.C.",
        currencies: List<String> = listOf("USD", "USN", "USS"),
        continent: String = "North America",
        languages: List<String> = listOf("English"),
    ) = CountryDetailsQuery.Country(
        code = code,
        name = name,
        native = native,
        emoji = emoji,
        capital = capital,
        currencies = currencies,
        continent = CountryDetailsQuery.Continent(name = continent),
        languages = languages.map { CountryDetailsQuery.Language(name = it) },
    )

    @Test
    fun `maps every field`() {
        assertEquals(
            CountryDetails(
                code = "JP",
                name = "Japan",
                nativeName = "日本",
                flag = "🇯🇵",
                capital = "Tokyo",
                continent = "Asia",
                languages = listOf("Japanese"),
                currencies = listOf("JPY"),
            ),
            country(
                code = "JP",
                name = "Japan",
                native = "日本",
                emoji = "🇯🇵",
                capital = "Tokyo",
                currencies = listOf("JPY"),
                continent = "Asia",
                languages = listOf("Japanese"),
            ).toDomain(),
        )
    }

    @Test
    fun `several languages and currencies are all kept trimmed and without blanks`() {
        val details =
            country(
                currencies = listOf(" USD", "USN ", "", "USS"),
                languages = listOf("English", "Spanish"),
            ).toDomain()

        assertEquals(listOf("USD", "USN", "USS"), details.currencies)
        assertEquals(listOf("English", "Spanish"), details.languages)
    }

    @Test
    fun `a native name equal to the name is dropped`() {
        assertNull(country(name = "United States", native = "United States").toDomain().nativeName)
    }

    @Test
    fun `the name is trimmed before it is compared with the native name`() {
        val details = country(name = "Japan ", native = "Japan").toDomain()

        assertEquals("Japan", details.name)
        assertNull(details.nativeName)
    }

    @Test
    fun `a missing or blank capital is null`() {
        assertNull(country(capital = null).toDomain().capital)
        assertNull(country(capital = "  ").toDomain().capital)
    }

    @Test
    fun `a blank emoji falls back to a flag built from the code`() {
        assertEquals("🇺🇸", country(emoji = "").toDomain().flag)
    }
}
