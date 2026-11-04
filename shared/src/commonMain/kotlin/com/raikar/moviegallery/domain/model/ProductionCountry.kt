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
        .map { supplementaryToString(REGIONAL_INDICATOR_A + (it - 'A')) }
        .joinToString("")
}

// Common-code stand-in for the JVM's Character.toChars: a code point above U+FFFF is a
// UTF-16 surrogate pair.
private fun supplementaryToString(codePoint: Int): String {
    val offset = codePoint - 0x10000
    return charArrayOf(
        (0xD800 + (offset shr 10)).toChar(),
        (0xDC00 + (offset and 0x3FF)).toChar(),
    ).concatToString()
}
