package com.raikar.moviegallery.ui.common

import com.raikar.moviegallery.domain.model.Movie
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the card subtitle, moved here from `:app`'s `MovieCard` (add-ios-app-shell design, decision 8), and the
 * synopsis fallback, moved from `MovieDetailScreen` (add-ios-browse-screens design, decision 4).
 */
class MovieLabelsTest {
    @Test
    fun `a rated film with genres shows the rating then two genres`() {
        assertEquals(
            "★ 8.4 · Drama · Crime",
            movie(rating = 8.4, genres = listOf("Drama", "Crime", "Thriller")).cardSubtitle,
        )
    }

    @Test
    fun `an unrated film shows only its genres`() {
        assertEquals("Horror", movie(rating = 0.0, genres = listOf("Horror")).cardSubtitle)
    }

    @Test
    fun `a rated film with no genres shows only the rating`() {
        assertEquals("★ 6.0", movie(rating = 6.0, genres = emptyList()).cardSubtitle)
    }

    @Test
    fun `with neither rating nor genres the year is shown`() {
        assertEquals("2027", movie(rating = 0.0, genres = emptyList(), year = 2027).cardSubtitle)
    }

    @Test
    fun `with no rating genres or year it reads TBA`() {
        assertEquals("TBA", movie(rating = 0.0, genres = emptyList(), year = null).cardSubtitle)
    }

    @Test
    fun `a present synopsis is shown as is`() {
        assertEquals("A heist.", movie(synopsis = "A heist.").synopsisText)
    }

    @Test
    fun `a blank synopsis falls back`() {
        assertEquals("No synopsis available.", movie(synopsis = "").synopsisText)
        assertEquals("No synopsis available.", movie(synopsis = "   ").synopsisText)
    }

    private fun movie(
        rating: Double = 7.0,
        genres: List<String> = emptyList(),
        year: Int? = 2024,
        synopsis: String = "",
    ) = Movie(
        id = 1,
        title = "A film",
        year = year,
        rating = rating,
        genres = genres,
        synopsis = synopsis,
        posterUrl = null,
        backdropUrl = null,
    )
}
