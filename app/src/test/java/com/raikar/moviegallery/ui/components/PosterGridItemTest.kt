package com.raikar.moviegallery.ui.components

import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.model.TvShow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PosterGridItemTest {
    private fun movie(
        year: Int? = 2026,
        rating: Double = 8.5,
        genres: List<String> = listOf("Science Fiction", "Action", "Adventure"),
    ) = Movie(
        id = 1,
        title = "Spider-Man: Brand New Day",
        year = year,
        rating = rating,
        genres = genres,
        synopsis = "",
        posterUrl = "poster",
        backdropUrl = null,
    )

    private fun show(
        year: Int? = 2011,
        rating: Double = 8.4,
        genres: List<String> = listOf("Drama"),
    ) = TvShow(
        id = 2,
        name = "Game of Thrones",
        overview = "",
        firstAirYear = year,
        rating = rating,
        posterUrl = null,
        genres = genres,
    )

    @Test
    fun `movie shows year and first two genres with a one-decimal rating`() {
        val item = movie().toPosterGridItem()

        assertEquals("Spider-Man: Brand New Day", item.title)
        assertEquals("2026 · Science Fiction · Action", item.subtitle)
        assertEquals("8.5", item.ratingLabel)
        assertEquals("poster", item.posterUrl)
    }

    @Test
    fun `movie without a release date drops the year`() {
        assertEquals("Action", movie(year = null, genres = listOf("Action")).toPosterGridItem().subtitle)
    }

    @Test
    fun `movie with no year and no genres reads TBA`() {
        assertEquals("TBA", movie(year = null, genres = emptyList()).toPosterGridItem().subtitle)
    }

    @Test
    fun `movie with no genres shows only the year`() {
        assertEquals("2026", movie(genres = emptyList()).toPosterGridItem().subtitle)
    }

    @Test
    fun `unrated movie has no rating label`() {
        assertNull(movie(rating = 0.0).toPosterGridItem().ratingLabel)
    }

    @Test
    fun `tv show uses its name and first air year`() {
        val item = show().toPosterGridItem()

        assertEquals("Game of Thrones", item.title)
        assertEquals("2011 · Drama", item.subtitle)
        assertEquals("8.4", item.ratingLabel)
    }

    @Test
    fun `tv show with no air year and no genres reads TBA`() {
        assertEquals("TBA", show(year = null, genres = emptyList()).toPosterGridItem().subtitle)
    }

    @Test
    fun `unrated tv show has no rating label`() {
        assertNull(show(rating = 0.0).toPosterGridItem().ratingLabel)
    }
}
