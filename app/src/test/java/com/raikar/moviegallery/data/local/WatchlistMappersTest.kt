package com.raikar.moviegallery.data.local

import com.raikar.moviegallery.domain.model.Movie
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchlistMappersTest {
    private val upcoming =
        Movie(
            id = 42,
            title = "Upcoming",
            year = null,
            rating = 0.0,
            genres = listOf("Drama", "Science Fiction", "Thriller"),
            synopsis = "Not out yet.",
            posterUrl = null,
            backdropUrl = "https://image.tmdb.org/t/p/w780/backdrop.jpg",
        )

    @Test
    fun `a movie round-trips through the entity unchanged`() {
        val entity = upcoming.toWatchlistEntity(ownerUid = "uid-a", addedAt = 1_000L)

        assertEquals("uid-a", entity.ownerUid)
        assertEquals(42, entity.movieId)
        assertEquals(1_000L, entity.addedAt)
        assertEquals(upcoming, entity.toDomain())
    }

    @Test
    fun `genres round-trip through the JSON column`() {
        val json = Converters.genresToJson(upcoming.genres)

        assertEquals(upcoming.genres, Converters.genresFromJson(json))
    }

    @Test
    fun `empty genres and genres with quotes round-trip`() {
        listOf(emptyList(), listOf("Sci \"Fi\"", "Kids, Family")).forEach { genres ->
            assertEquals(genres, Converters.genresFromJson(Converters.genresToJson(genres)))
        }
    }
}
