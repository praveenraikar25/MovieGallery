package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.domain.repository.MovieRepository

class GetMovieUseCase(
    private val movieRepository: MovieRepository,
) {
    suspend operator fun invoke(id: Int): DataResult<MovieDetail> = movieRepository.getMovieDetail(id)
}
