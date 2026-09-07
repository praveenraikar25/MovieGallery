package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.domain.model.MovieSection
import com.raikar.moviegallery.domain.repository.MovieRepository

/** Lets each endpoint's result be programmed independently for ViewModel tests. */
class FakeMovieRepository(
    var sectionsResult: DataResult<List<MovieSection>> = DataResult.Success(emptyList()),
    var popularResult: DataResult<List<Movie>> = DataResult.Success(emptyList()),
    var searchResult: DataResult<List<Movie>> = DataResult.Success(emptyList()),
    var movieDetailResult: DataResult<MovieDetail> = DataResult.Failure(AppError.NotFound),
) : MovieRepository {
    var lastSearchQuery: String? = null
        private set

    override suspend fun getSections(): DataResult<List<MovieSection>> = sectionsResult

    override suspend fun getPopularMovies(): DataResult<List<Movie>> = popularResult

    override suspend fun searchMovies(query: String): DataResult<List<Movie>> {
        lastSearchQuery = query
        return searchResult
    }

    override suspend fun getMovieDetail(id: Int): DataResult<MovieDetail> = movieDetailResult
}
