package com.raikar.moviegallery.data.local

import com.raikar.moviegallery.domain.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the watchlisted movies themselves rather than just their ids, so the
 * Watchlist screen renders without going back to the network. In-memory only —
 * the list does not survive process death.
 */
@Singleton
class InMemoryWatchlistDataSource
    @Inject
    constructor() {
        // A list rather than a set/map keyed by id: it keeps insertion order for the
        // Watchlist grid and lets the repository expose it as a StateFlow without
        // needing an application-scoped CoroutineScope to map through. The watchlist
        // is small enough that the linear id lookup is irrelevant.
        private val _movies = MutableStateFlow<List<Movie>>(emptyList())
        val movies: StateFlow<List<Movie>> = _movies

        fun toggle(movie: Movie) {
            _movies.update { current ->
                if (current.any { it.id == movie.id }) {
                    current.filterNot { it.id == movie.id }
                } else {
                    current + movie
                }
            }
        }
    }
