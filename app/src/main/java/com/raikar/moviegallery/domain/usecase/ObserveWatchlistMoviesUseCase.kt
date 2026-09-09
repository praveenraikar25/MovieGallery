package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWatchlistMoviesUseCase
    @Inject
    constructor(
        private val watchlistRepository: WatchlistRepository,
    ) {
        operator fun invoke(): Flow<List<Movie>> = watchlistRepository.watchlist
    }
