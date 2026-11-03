package com.raikar.moviegallery.domain.model

/**
 * A country TMDB credits with producing a movie. [code] is ISO 3166-1 alpha-2 (`KR`), though
 * TMDB still uses some retired codes (`SU`, `YU`) for older films; [name] is TMDB's English name.
 */
data class ProductionCountry(
    val code: String,
    val name: String,
) {
    /**
     * The flag emoji built from [code]'s two regional-indicator symbols, or null when [code] is
     * not two ASCII letters. A retired code still yields a pair, which some fonts draw as letters.
     */
    val flag: String? get() = flagEmoji(code)
}

private const val REGIONAL_INDICATOR_A = 0x1F1E6

internal fun flagEmoji(code: String): String? {
    // Checked before uppercasing: "ß".uppercase() is "SS", which would pass as South Sudan.
    if (code.length != 2 || code.any { it !in 'A'..'Z' && it !in 'a'..'z' }) return null
    return code
        .uppercase()
        .map { Character.toChars(REGIONAL_INDICATOR_A + (it - 'A')).concatToString() }
        .joinToString("")
}
