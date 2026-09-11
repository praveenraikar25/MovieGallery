package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.TvShow
import com.raikar.moviegallery.domain.repository.TvShowRepository
import javax.inject.Inject

class GetTopTvShowsUseCase
    @Inject
    constructor(
        private val tvShowRepository: TvShowRepository,
    ) {
        suspend operator fun invoke(): List<TvShow> = tvShowRepository.topTvShows()
    }
