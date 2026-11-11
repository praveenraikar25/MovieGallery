package com.raikar.moviegallery.ui.common

import com.raikar.moviegallery.domain.model.ProductionCountry

/**
 * "🇰🇷 South Korea", or just the name when the code yields no flag. Shared so both apps' country
 * chips read the same.
 */
fun countryChipLabel(country: ProductionCountry): String = country.flag?.let { "$it ${country.name}" } ?: country.name
