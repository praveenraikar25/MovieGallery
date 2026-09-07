package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.MovieSection
import com.raikar.moviegallery.domain.repository.MovieRepository
import javax.inject.Inject

class GetHomeSectionsUseCase
    @Inject
    constructor(
        private val movieRepository: MovieRepository,
    ) {
        suspend operator fun invoke(): DataResult<List<MovieSection>> = movieRepository.getSections()
    }
