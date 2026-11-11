package com.raikar.moviegallery.ui.common

import com.raikar.moviegallery.domain.model.CountryDetails

private const val MISSING_VALUE = "—"

/**
 * The country details sheet's label/value rows, with "—" for anything the source left out.
 * Shared so Android's and iOS's sheets list the same rows.
 */
fun countryDetailRows(details: CountryDetails): List<Pair<String, String>> =
    listOf(
        "Capital" to (details.capital ?: MISSING_VALUE),
        "Continent" to (details.continent ?: MISSING_VALUE),
        "Languages" to details.languages.joinToString(", ").ifEmpty { MISSING_VALUE },
        "Currency" to details.currencies.joinToString(", ").ifEmpty { MISSING_VALUE },
    )
