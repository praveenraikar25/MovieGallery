package com.raikar.moviegallery.domain.repository

import com.raikar.moviegallery.domain.model.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface WatchlistRepository {
    /**
     * Holds the movie summaries themselves, not just ids — otherwise rendering the
     * watchlist would need a network round-trip per toggle.
     */
    val watchlist: StateFlow<List<Movie>>

    fun isInWatchlist(id: Int): Flow<Boolean>

    suspend fun toggle(movie: Movie)
}
