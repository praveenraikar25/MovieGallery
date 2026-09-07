package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.data.remote.TmdbApi
import com.raikar.moviegallery.data.remote.dto.MovieDetailDto
import com.raikar.moviegallery.data.remote.dto.MovieListResponseDto
import com.raikar.moviegallery.data.remote.dto.MovieSummaryDto

/**
 * Lets each endpoint be programmed independently, which is what the Home
 * partial-success behaviour needs to be tested.
 */
internal class FakeTmdbApi(
    var popularResponse: () -> MovieListResponseDto = { listOf(1).asResponse() },
    var topRatedResponse: () -> MovieListResponseDto = { listOf(2).asResponse() },
    var nowPlayingResponse: () -> MovieListResponseDto = { listOf(3).asResponse() },
    var upcomingResponse: () -> MovieListResponseDto = { listOf(4).asResponse() },
    var searchResponse: (String) -> MovieListResponseDto = { listOf(5).asResponse() },
    var detailResponse: (Int) -> MovieDetailDto = { MovieDetailDto(id = it, title = "Movie $it") },
) : TmdbApi {
    override suspend fun popular(
        language: String,
        page: Int,
    ) = popularResponse()

    override suspend fun topRated(
        language: String,
        page: Int,
    ) = topRatedResponse()

    override suspend fun nowPlaying(
        language: String,
        page: Int,
    ) = nowPlayingResponse()

    override suspend fun upcoming(
        language: String,
        page: Int,
    ) = upcomingResponse()

    override suspend fun searchMovies(
        query: String,
        includeAdult: Boolean,
        language: String,
        page: Int,
    ) = searchResponse(query)

    override suspend fun movieDetail(
        movieId: Int,
        append: String,
        language: String,
    ) = detailResponse(movieId)
}

internal fun List<Int>.asResponse(): MovieListResponseDto =
    MovieListResponseDto(
        results = map { MovieSummaryDto(id = it, title = "Movie $it") },
    )
