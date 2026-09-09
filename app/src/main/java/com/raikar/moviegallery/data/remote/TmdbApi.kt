package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.dto.MovieDetailDto
import com.raikar.moviegallery.data.remote.dto.MovieListResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDB v3 REST surface. Authentication is a v4 bearer token attached by an
 * interceptor, so no endpoint takes an `api_key` parameter.
 */
interface TmdbApi {
    @GET("movie/popular")
    suspend fun popular(
        @Query("language") language: String = DEFAULT_LANGUAGE,
        @Query("page") page: Int = 1,
    ): MovieListResponseDto

    @GET("movie/top_rated")
    suspend fun topRated(
        @Query("language") language: String = DEFAULT_LANGUAGE,
        @Query("page") page: Int = 1,
    ): MovieListResponseDto

    @GET("movie/now_playing")
    suspend fun nowPlaying(
        @Query("language") language: String = DEFAULT_LANGUAGE,
        @Query("page") page: Int = 1,
    ): MovieListResponseDto

    @GET("movie/upcoming")
    suspend fun upcoming(
        @Query("language") language: String = DEFAULT_LANGUAGE,
        @Query("page") page: Int = 1,
    ): MovieListResponseDto

    /** TMDB answers with HTTP 422 if [query] is blank, so callers must not send one. */
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = DEFAULT_LANGUAGE,
        @Query("page") page: Int = 1,
    ): MovieListResponseDto

    /** Details and cast in a single request via `append_to_response`. */
    @GET("movie/{movieId}")
    suspend fun movieDetail(
        @Path("movieId") movieId: Int,
        @Query("append_to_response") append: String = "credits",
        @Query("language") language: String = DEFAULT_LANGUAGE,
    ): MovieDetailDto

    companion object {
        /** Trailing slash is required by Retrofit for relative endpoint paths. */
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val DEFAULT_LANGUAGE = "en-US"
    }
}
