package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.MovieSection
import com.raikar.moviegallery.domain.repository.MovieRepository

class GetHomeSectionsUseCase(
    private val movieRepository: MovieRepository,
) {
    suspend operator fun invoke(): DataResult<List<MovieSection>> = movieRepository.getSections()
}
