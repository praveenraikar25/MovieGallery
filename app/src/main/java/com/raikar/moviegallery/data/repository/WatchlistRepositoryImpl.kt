package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.data.local.InMemoryWatchlistDataSource
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl
    @Inject
    constructor(
        private val local: InMemoryWatchlistDataSource,
    ) : WatchlistRepository {
        override val watchlist: StateFlow<List<Movie>> = local.movies

        override fun isInWatchlist(id: Int): Flow<Boolean> =
            local.movies.map { movies -> movies.any { it.id == id } }.distinctUntilChanged()

        override suspend fun toggle(movie: Movie) = local.toggle(movie)
    }
