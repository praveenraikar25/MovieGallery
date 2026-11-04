package com.raikar.moviegallery.domain.repository

import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.domain.model.MovieSection

interface MovieRepository {
    suspend fun getSections(): DataResult<List<MovieSection>>

    /** Backs the search screen's empty-query state; TMDB rejects a blank query. */
    suspend fun getPopularMovies(): DataResult<List<Movie>>

    suspend fun searchMovies(query: String): DataResult<List<Movie>>

    suspend fun getMovieDetail(id: Int): DataResult<MovieDetail>
}
