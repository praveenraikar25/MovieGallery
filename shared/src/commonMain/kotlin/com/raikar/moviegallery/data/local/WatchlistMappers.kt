package com.raikar.moviegallery.data.local

import com.raikar.moviegallery.domain.model.Movie

internal fun WatchlistEntity.toDomain(): Movie =
    Movie(
        id = movieId,
        title = title,
        year = year,
        rating = rating,
        genres = genres,
        synopsis = synopsis,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
    )

internal fun Movie.toWatchlistEntity(
    ownerUid: String,
    addedAt: Long,
): WatchlistEntity =
    WatchlistEntity(
        ownerUid = ownerUid,
        movieId = id,
        title = title,
        year = year,
        rating = rating,
        genres = genres,
        synopsis = synopsis,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        addedAt = addedAt,
    )
