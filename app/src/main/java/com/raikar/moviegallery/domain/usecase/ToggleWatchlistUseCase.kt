package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.repository.WatchlistRepository
import javax.inject.Inject

class ToggleWatchlistUseCase
    @Inject
    constructor(
        private val watchlistRepository: WatchlistRepository,
    ) {
        suspend operator fun invoke(movie: Movie) = watchlistRepository.toggle(movie)
    }
