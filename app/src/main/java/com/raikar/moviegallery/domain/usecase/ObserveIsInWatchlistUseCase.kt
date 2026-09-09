package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveIsInWatchlistUseCase
    @Inject
    constructor(
        private val watchlistRepository: WatchlistRepository,
    ) {
        operator fun invoke(id: Int): Flow<Boolean> = watchlistRepository.isInWatchlist(id)
    }
