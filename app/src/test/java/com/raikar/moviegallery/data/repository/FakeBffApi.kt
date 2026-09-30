package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.data.remote.BffApi
import com.raikar.moviegallery.data.remote.dto.TvShowListResponseDto
import com.raikar.moviegallery.data.remote.dto.TvShowSummaryDto

internal class FakeBffApi(
    var topTvShowsResponse: (Int) -> TvShowListResponseDto = { page -> listOf(1).asTvShowResponse(page) },
) : BffApi {
    override suspend fun topTvShows(page: Int) = topTvShowsResponse(page)
}

internal fun List<Int>.asTvShowResponse(
    page: Int = 1,
    totalPages: Int = 1,
): TvShowListResponseDto =
    TvShowListResponseDto(
        page = page,
        totalPages = totalPages,
        results = map { TvShowSummaryDto(id = it, name = "Show $it") },
    )
