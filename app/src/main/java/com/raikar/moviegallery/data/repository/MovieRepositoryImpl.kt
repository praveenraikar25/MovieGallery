package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.data.remote.TmdbApi
import com.raikar.moviegallery.data.remote.dto.MovieListResponseDto
import com.raikar.moviegallery.data.remote.safeApiCall
import com.raikar.moviegallery.data.remote.toDomain
import com.raikar.moviegallery.di.qualifier.IoDispatcher
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.domain.model.MovieSection
import com.raikar.moviegallery.domain.repository.MovieRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl
    @Inject
    constructor(
        private val api: TmdbApi,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : MovieRepository {
        /** The Home rails, in display order. */
        private val sectionSpecs: List<SectionSpec> =
            listOf(
                SectionSpec("Trending Now") { api.popular() },
                SectionSpec("Now Playing") { api.nowPlaying() },
                SectionSpec("Top Rated") { api.topRated() },
                SectionSpec("Coming Soon") { api.upcoming() },
            )

        /**
         * Loads all four rails concurrently. A `supervisorScope` with per-call error
         * handling means one failing endpoint costs one rail rather than the whole
         * screen; we only report failure when nothing at all came back.
         */
        override suspend fun getSections(): DataResult<List<MovieSection>> =
            withContext(ioDispatcher) {
                val outcomes =
                    supervisorScope {
                        sectionSpecs
                            .map { spec -> async { spec to safeApiCall { spec.fetch() } } }
                            .awaitAll()
                    }

                val sections =
                    outcomes.mapNotNull { (spec, result) ->
                        val movies = (result as? DataResult.Success)?.data?.results.orEmpty()
                        if (movies.isEmpty()) {
                            null
                        } else {
                            MovieSection(title = spec.title, movies = movies.map { it.toDomain() })
                        }
                    }

                if (sections.isNotEmpty()) {
                    DataResult.Success(sections)
                } else {
                    val firstError =
                        outcomes.firstNotNullOfOrNull { (_, result) ->
                            (result as? DataResult.Failure)?.error
                        }
                    DataResult.Failure(firstError ?: AppError.Unknown(IllegalStateException("No sections returned")))
                }
            }

        override suspend fun getPopularMovies(): DataResult<List<Movie>> =
            withContext(ioDispatcher) {
                safeApiCall { api.popular().results.map { it.toDomain() } }
            }

        override suspend fun searchMovies(query: String): DataResult<List<Movie>> =
            withContext(ioDispatcher) {
                safeApiCall { api.searchMovies(query = query).results.map { it.toDomain() } }
            }

        override suspend fun getMovieDetail(id: Int): DataResult<MovieDetail> =
            withContext(ioDispatcher) {
                safeApiCall { api.movieDetail(movieId = id).toDomain() }
            }

        private class SectionSpec(
            val title: String,
            val fetch: suspend () -> MovieListResponseDto,
        )
    }
