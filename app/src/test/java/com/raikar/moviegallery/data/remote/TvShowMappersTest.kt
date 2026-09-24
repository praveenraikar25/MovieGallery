package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.dto.TvShowListResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvShowMappersTest {
    private fun topShows(): TvShowListResponseDto = testJson.decodeFromString(fixture("tv_show_list_top.json"))

    @Test
    fun `maps a fully populated summary and the envelope's paging fields`() {
        val page = topShows().toDomain()

        assertEquals(1, page.page)
        assertEquals(500, page.totalPages)

        val show = page.shows[0]
        assertEquals(1399, show.id)
        assertEquals("Game of Thrones", show.name)
        assertEquals(2011, show.firstAirYear)
        assertEquals(8.4, show.rating, 0.0001)
        assertEquals(listOf("Drama", "Sci-Fi & Fantasy"), show.genres)
        assertEquals("https://image.tmdb.org/t/p/w500/xxx.jpg", show.posterUrl)
    }

    @Test
    fun `a null first air date yields a null year rather than throwing`() {
        val show = topShows().toDomain().shows[1]

        assertNull(show.firstAirYear)
        assertEquals("TBA", show.yearLabel)
    }

    @Test
    fun `a zero vote average is reported as having no rating`() {
        val show = topShows().toDomain().shows[1]

        assertEquals(0.0, show.rating, 0.0)
        assertTrue(!show.hasRating)
    }

    @Test
    fun `a null poster url maps to null rather than an empty string`() {
        assertNull(topShows().toDomain().shows[1].posterUrl)
    }

    @Test
    fun `a partial first air date still yields a year`() {
        assertEquals(2026, topShows().toDomain().shows[2].firstAirYear)
    }
}
