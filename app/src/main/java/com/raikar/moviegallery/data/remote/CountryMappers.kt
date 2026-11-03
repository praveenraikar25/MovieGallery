package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.graphql.countries.CountryDetailsQuery
import com.raikar.moviegallery.domain.model.CountryDetails
import com.raikar.moviegallery.domain.model.flagEmoji

internal fun CountryDetailsQuery.Country.toDomain(): CountryDetails =
    CountryDetails(
        code = code,
        name = name,
        nativeName = native.trim().takeIf { it.isNotEmpty() && it != name },
        flag = emoji.takeIf { it.isNotBlank() } ?: flagEmoji(code),
        capital = capital?.trim()?.takeIf { it.isNotEmpty() },
        continent = continent.name.trim().takeIf { it.isNotEmpty() },
        languages = languages.map { it.name.trim() }.filter { it.isNotEmpty() },
        currencies = currencies.map { it.trim() }.filter { it.isNotEmpty() },
    )
