package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.dto.CastMemberDto
import com.raikar.moviegallery.data.remote.dto.MovieDetailDto
import com.raikar.moviegallery.data.remote.dto.MovieSummaryDto
import com.raikar.moviegallery.data.remote.dto.ProductionCountryDto
import com.raikar.moviegallery.domain.model.CastMember
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.domain.model.ProductionCountry

/** How many cast members the detail screen shows. */
private const val MAX_CAST = 10

internal fun MovieSummaryDto.toDomain(): Movie =
    Movie(
        id = id,
        title = title,
        year = releaseDate.toReleaseYear(),
        rating = voteAverage,
        genres = TmdbGenres.namesFor(genreIds),
        synopsis = overview,
        posterUrl = TmdbImages.posterUrl(posterPath),
        backdropUrl = TmdbImages.backdropUrl(backdropPath),
    )

internal fun MovieDetailDto.toDomain(): MovieDetail =
    MovieDetail(
        summary =
            Movie(
                id = id,
                title = title,
                year = releaseDate.toReleaseYear(),
                rating = voteAverage,
                genres = genres.map { it.name }.filter { it.isNotBlank() },
                synopsis = overview,
                posterUrl = TmdbImages.posterUrl(posterPath),
                backdropUrl = TmdbImages.backdropUrl(backdropPath),
            ),
        runtimeMinutes = runtime?.takeIf { it > 0 },
        tagline = tagline?.takeIf { it.isNotBlank() },
        cast =
            credits
                ?.cast
                ?.sortedBy { it.order }
                ?.take(MAX_CAST)
                ?.map { it.toDomain() }
                .orEmpty(),
        productionCountries = productionCountries.mapNotNull { it.toDomain() },
    )

/**
 * Null only for an entry with no name, which the screen could not label. A blank or malformed
 * code still yields a chip, shown by name without a flag.
 */
private fun ProductionCountryDto.toDomain(): ProductionCountry? =
    if (name.isBlank()) null else ProductionCountry(code = code.trim(), name = name.trim())

private fun CastMemberDto.toDomain(): CastMember =
    CastMember(
        name = name,
        role = character.ifBlank { "—" },
    )

/**
 * TMDB dates look like `2024-03-15`, but the field is frequently `""` (notably on
 * `upcoming`) and occasionally malformed, so this never indexes into the string.
 */
internal fun String?.toReleaseYear(): Int? = this?.take(4)?.toIntOrNull()
