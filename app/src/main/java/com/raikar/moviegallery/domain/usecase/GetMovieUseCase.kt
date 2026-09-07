package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.domain.repository.MovieRepository
import javax.inject.Inject

class GetMovieUseCase
    @Inject
    constructor(
        private val movieRepository: MovieRepository,
    ) {
        suspend operator fun invoke(id: Int): DataResult<MovieDetail> = movieRepository.getMovieDetail(id)
    }
