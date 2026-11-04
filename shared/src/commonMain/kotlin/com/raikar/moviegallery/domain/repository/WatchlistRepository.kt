package com.raikar.moviegallery.domain.repository

import com.raikar.moviegallery.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    /**
     * Holds the movie summaries themselves, not just ids — otherwise rendering the
     * watchlist would need a network round-trip per toggle. Ordered oldest-added first.
     */
    val watchlist: Flow<List<Movie>>

    fun isInWatchlist(id: Int): Flow<Boolean>

    suspend fun toggle(movie: Movie)
}
