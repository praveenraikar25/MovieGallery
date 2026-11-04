package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow

class ObserveWatchlistMoviesUseCase(
    private val watchlistRepository: WatchlistRepository,
) {
    operator fun invoke(): Flow<List<Movie>> = watchlistRepository.watchlist
}
