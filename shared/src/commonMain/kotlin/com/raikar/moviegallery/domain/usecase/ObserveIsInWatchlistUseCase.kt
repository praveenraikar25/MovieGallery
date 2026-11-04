package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow

class ObserveIsInWatchlistUseCase(
    private val watchlistRepository: WatchlistRepository,
) {
    operator fun invoke(id: Int): Flow<Boolean> = watchlistRepository.isInWatchlist(id)
}
