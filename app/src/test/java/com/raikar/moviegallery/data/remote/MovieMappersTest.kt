package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.dto.MovieDetailDto
import com.raikar.moviegallery.data.remote.dto.MovieListResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses real TMDB response shapes through the production [Json] config and maps
 * them, so both the DTO contract and the mapper are covered.
 */
class MovieMappersTest {
    private fun popular(): MovieListResponseDto = testJson.decodeFromString(fixture("movie_list_popular.json"))

    private fun detail(name: String): MovieDetailDto = testJson.decodeFromString(fixture(name))

    @Test
    fun `maps a fully populated summary`() {
        val movie = popular().results[0].toDomain()

        assertEquals(1061474, movie.id)
        assertEquals("Superman", movie.title)
        assertEquals(2025, movie.year)
        assertEquals(7.383, movie.rating, 0.0001)
        assertEquals(listOf("Science Fiction", "Adventure", "Action"), movie.genres)
        assertEquals(
            "https://image.tmdb.org/t/p/w500/ombsmhYUqR4qqOLOxAyr5V8hbyv.jpg",
            movie.posterUrl,
        )
        assertEquals(
            "https://image.tmdb.org/t/p/w780/aZgWnhSHmoYcvNjRfsBmVXpJ7Ln.jpg",
            movie.backdropUrl,
        )
    }

    @Test
    fun `empty release date yields a null year rather than throwing`() {
        val movie = popular().results[1].toDomain()

        assertNull(movie.year)
        assertEquals("TBA", movie.yearLabel)
    }

    @Test
    fun `missing poster and backdrop paths map to null urls`() {
        val movie = popular().results[1].toDomain()

        assertNull(movie.posterUrl)
        assertNull(movie.backdropUrl)
    }

    @Test
    fun `partial release date still yields a year`() {
        // "2026-03" — shorter than a full date, so take(4) must not be substring(0,4).
        assertEquals(2026, popular().results[2].toDomain().year)
    }

    @Test
    fun `unpublished genre ids are dropped instead of rendered`() {
        assertEquals(listOf("Action"), popular().results[2].toDomain().genres)
    }

    @Test
    fun `a zero vote average is reported as having no rating`() {
        val unrated = popular().results[1].toDomain()

        assertEquals(0.0, unrated.rating, 0.0)
        assertTrue(!unrated.hasRating)
        assertTrue(popular().results[0].toDomain().hasRating)
    }

    @Test
    fun `rating label rounds to one decimal place`() {
        assertEquals("7.4", popular().results[0].toDomain().ratingLabel)
    }

    @Test
    fun `genre label shows at most two genres`() {
        assertEquals("Science Fiction · Adventure", popular().results[0].toDomain().genreLabel)
    }

    @Test
    fun `the upcoming endpoint's extra dates object does not break parsing`() {
        val response: MovieListResponseDto =
            testJson.decodeFromString(fixture("movie_list_upcoming.json"))

        assertEquals(1, response.results.size)
        assertEquals("Jurassic World Rebirth", response.results[0].toDomain().title)
    }

    @Test
    fun `maps detail including appended credits`() {
        val detail = detail("movie_detail_with_credits.json").toDomain()

        assertEquals(130, detail.runtimeMinutes)
        assertEquals("Look up.", detail.tagline)
        // Detail responses carry resolved genre names, not ids.
        assertEquals(listOf("Science Fiction", "Adventure", "Action"), detail.summary.genres)
        assertEquals(3, detail.cast.size)
    }

    @Test
    fun `cast is ordered by TMDB billing order, not response order`() {
        val cast = detail("movie_detail_with_credits.json").toDomain().cast

        assertEquals(listOf("David Corenswet", "Rachel Brosnahan", "Nameless Extra"), cast.map { it.name })
        assertEquals("D", cast[0].initial)
    }

    @Test
    fun `a blank character name falls back to a placeholder role`() {
        val cast = detail("movie_detail_with_credits.json").toDomain().cast

        assertEquals("—", cast.last().role)
    }

    @Test
    fun `zero runtime and blank tagline become null`() {
        val detail = detail("movie_detail_sparse.json").toDomain()

        assertNull(detail.runtimeMinutes)
        assertNull(detail.tagline)
        assertNull(detail.summary.year)
        assertTrue(detail.cast.isEmpty())
    }

    @Test
    fun `metadata label omits the fields TMDB did not return`() {
        assertEquals(
            "2025 · 130 min · Science Fiction · Adventure",
            detail("movie_detail_with_credits.json").toDomain().metadataLabel,
        )
        // Nothing to show at all, rather than "null · 0 min".
        assertEquals("", detail("movie_detail_sparse.json").toDomain().metadataLabel)
    }
}
