package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeWatchlistRepository(
    initial: List<Movie> = emptyList(),
) : WatchlistRepository {
    private val _watchlist = MutableStateFlow(initial)
    override val watchlist: StateFlow<List<Movie>> = _watchlist.asStateFlow()

    val toggleCalls = mutableListOf<Movie>()

    override fun isInWatchlist(id: Int): Flow<Boolean> =
        watchlist.map { movies -> movies.any { it.id == id } }.distinctUntilChanged()

    override suspend fun toggle(movie: Movie) {
        toggleCalls += movie
        _watchlist.update { movies ->
            if (movies.any { it.id == movie.id }) {
                movies.filterNot { it.id == movie.id }
            } else {
                movies + movie
            }
        }
    }
}
