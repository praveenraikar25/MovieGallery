package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.data.remote.dto.MovieDetailDto
import com.raikar.moviegallery.data.remote.dto.MovieListResponseDto
import com.raikar.moviegallery.data.remote.dto.MovieSummaryDto
import com.raikar.moviegallery.data.remote.dto.TvShowListResponseDto
import com.raikar.moviegallery.data.remote.dto.TvShowSummaryDto
import com.raikar.moviegallery.data.remote.testJson

/** A TMDB movie-list body whose movies have these ids, titled `Movie <id>`. */
fun List<Int>.asMovieListJson(): String =
    testJson.encodeToString(
        MovieListResponseDto(results = map { MovieSummaryDto(id = it, title = "Movie $it") }),
    )

/** A TMDB movie-detail body for [id], titled `Movie <id>`. */
fun movieDetailJson(id: Int): String = testJson.encodeToString(MovieDetailDto(id = id, title = "Movie $id"))

/** A BFF top-shows body whose shows have these ids, named `Show <id>`. */
fun List<Int>.asTvShowListJson(
    page: Int = 1,
    totalPages: Int = 1,
): String =
    testJson.encodeToString(
        TvShowListResponseDto(
            page = page,
            totalPages = totalPages,
            results = map { TvShowSummaryDto(id = it, name = "Show $it") },
        ),
    )
