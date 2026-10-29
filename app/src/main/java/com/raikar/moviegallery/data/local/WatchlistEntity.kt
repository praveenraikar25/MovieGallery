package com.raikar.moviegallery.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity

/**
 * One saved movie for one account. Keyed by (owner, movie) so the same movie can be
 * saved by several accounts on a device but never twice by one. The movie's details are
 * a snapshot taken when it was saved, which is what lets the Watchlist tab render offline.
 */
@Entity(
    tableName = "watchlist",
    primaryKeys = ["owner_uid", "movie_id"],
)
data class WatchlistEntity(
    @ColumnInfo(name = "owner_uid") val ownerUid: String,
    @ColumnInfo(name = "movie_id") val movieId: Int,
    val title: String,
    val year: Int?,
    val rating: Double,
    val genres: List<String>,
    val synopsis: String,
    @ColumnInfo(name = "poster_url") val posterUrl: String?,
    @ColumnInfo(name = "backdrop_url") val backdropUrl: String?,
    /** Epoch milliseconds; the watchlist is listed oldest-added first. */
    @ColumnInfo(name = "added_at") val addedAt: Long,
)
