package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.TvShowPage
import com.raikar.moviegallery.domain.repository.TvShowRepository
import javax.inject.Inject

class GetTopTvShowsUseCase
    @Inject
    constructor(
        private val tvShowRepository: TvShowRepository,
    ) {
        suspend operator fun invoke(page: Int): DataResult<TvShowPage> = tvShowRepository.topTvShows(page)
    }
