package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.repository.MovieRepository
import javax.inject.Inject

class SearchMoviesUseCase
    @Inject
    constructor(
        private val movieRepository: MovieRepository,
    ) {
        /**
         * A blank query falls back to the popular list: TMDB answers `search/movie`
         * with HTTP 422 when `query` is empty.
         */
        suspend operator fun invoke(query: String): DataResult<List<Movie>> =
            if (query.isBlank()) {
                movieRepository.getPopularMovies()
            } else {
                movieRepository.searchMovies(query)
            }
    }
