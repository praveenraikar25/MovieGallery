package com.raikar.moviegallery.data.remote

/**
 * Builds TMDB image URLs.
 *
 * TMDB's docs suggest discovering the base URL and available sizes from the
 * `/configuration` endpoint. We hardcode them instead to avoid an extra request
 * on every cold start — the values have been stable for years. Keeping that
 * shortcut confined to this one file means moving to `/configuration` later is a
 * single-file change.
 */
internal object TmdbImages {
    private const val BASE_URL = "https://image.tmdb.org/t/p/"
    private const val POSTER_SIZE = "w500"
    private const val BACKDROP_SIZE = "w780"

    /** [path] already starts with a slash, e.g. `/abc123.jpg`. */
    fun posterUrl(path: String?): String? = urlFor(POSTER_SIZE, path)

    fun backdropUrl(path: String?): String? = urlFor(BACKDROP_SIZE, path)

    private fun urlFor(
        size: String,
        path: String?,
    ): String? = path?.takeIf { it.isNotBlank() }?.let { "$BASE_URL$size$it" }
}
