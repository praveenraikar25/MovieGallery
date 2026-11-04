package com.raikar.moviegallery.data.remote

/**
 * TMDB's movie genre taxonomy, used to turn the `genre_ids` that list endpoints
 * return into names.
 *
 * This is deliberately static rather than fetched from `/genre/movie/list`: the
 * four Home sections load concurrently, so a lazily populated singleton cache
 * would either race or need single-flight coordination — a lot of machinery for
 * decorative label text. The list is reference data, not content, and TMDB has
 * not changed it in years. Detail responses carry resolved names already, so this
 * only feeds the summary mapper.
 */
internal object TmdbGenres {
    private val byId: Map<Int, String> =
        mapOf(
            28 to "Action",
            12 to "Adventure",
            16 to "Animation",
            35 to "Comedy",
            80 to "Crime",
            99 to "Documentary",
            18 to "Drama",
            10751 to "Family",
            14 to "Fantasy",
            36 to "History",
            27 to "Horror",
            10402 to "Music",
            9648 to "Mystery",
            10749 to "Romance",
            878 to "Science Fiction",
            10770 to "TV Movie",
            53 to "Thriller",
            10752 to "War",
            37 to "Western",
        )

    /** Unknown ids are dropped rather than rendered as a raw number. */
    fun namesFor(ids: List<Int>): List<String> = ids.mapNotNull(byId::get)
}
