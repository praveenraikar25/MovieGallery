package com.raikar.moviegallery.domain.model

/** What the details sheet shows for one production country. Missing values are null or empty. */
data class CountryDetails(
    val code: String,
    val name: String,
    /** Null when it is the same as [name]. */
    val nativeName: String?,
    val flag: String?,
    val capital: String?,
    val continent: String?,
    val languages: List<String>,
    val currencies: List<String>,
)
