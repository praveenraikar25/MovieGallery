package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.dto.TvShowListResponseDto
import com.raikar.moviegallery.data.remote.dto.TvShowSummaryDto
import com.raikar.moviegallery.domain.model.TvShow
import com.raikar.moviegallery.domain.model.TvShowPage

// TEMP(share-core-kmp-module): public while the Retrofit repositories in :app use it; data layer only.
fun TvShowSummaryDto.toDomain(): TvShow =
    TvShow(
        id = id,
        name = name,
        overview = overview,
        firstAirYear = firstAirDate.toReleaseYear(),
        rating = voteAverage,
        posterUrl = posterUrl,
        genres = genres,
    )

// TEMP(share-core-kmp-module): public while the Retrofit repositories in :app use it; data layer only.
fun TvShowListResponseDto.toDomain(): TvShowPage =
    TvShowPage(
        shows = results.map { it.toDomain() },
        page = page,
        totalPages = totalPages,
    )
