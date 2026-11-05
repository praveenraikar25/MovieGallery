package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.dto.MovieDetailDto
import com.raikar.moviegallery.data.remote.dto.MovieListResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * TMDB v3 REST surface over a client from [createTmdbHttpClient]. Authentication is a v4 bearer
 * token the client adds, so no endpoint takes an `api_key` parameter. Paths are relative to
 * [BASE_URL] and must not start with `/`.
 */
class TmdbApi(
    private val client: HttpClient,
) {
    suspend fun popular(
        language: String = DEFAULT_LANGUAGE,
        page: Int = 1,
    ): MovieListResponseDto = movieList("movie/popular", language, page)

    suspend fun topRated(
        language: String = DEFAULT_LANGUAGE,
        page: Int = 1,
    ): MovieListResponseDto = movieList("movie/top_rated", language, page)

    suspend fun nowPlaying(
        language: String = DEFAULT_LANGUAGE,
        page: Int = 1,
    ): MovieListResponseDto = movieList("movie/now_playing", language, page)

    suspend fun upcoming(
        language: String = DEFAULT_LANGUAGE,
        page: Int = 1,
    ): MovieListResponseDto = movieList("movie/upcoming", language, page)

    /** TMDB answers with HTTP 422 if [query] is blank, so callers must not send one. */
    suspend fun searchMovies(
        query: String,
        includeAdult: Boolean = false,
        language: String = DEFAULT_LANGUAGE,
        page: Int = 1,
    ): MovieListResponseDto =
        client
            .get("search/movie") {
                parameter("query", query)
                parameter("include_adult", includeAdult)
                parameter("language", language)
                parameter("page", page)
            }.decode()

    /** Details and cast in a single request via `append_to_response`. */
    suspend fun movieDetail(
        movieId: Int,
        append: String = "credits",
        language: String = DEFAULT_LANGUAGE,
    ): MovieDetailDto =
        client
            .get("movie/$movieId") {
                parameter("append_to_response", append)
                parameter("language", language)
            }.decode()

    private suspend fun movieList(
        path: String,
        language: String,
        page: Int,
    ): MovieListResponseDto =
        client
            .get(path) {
                parameter("language", language)
                parameter("page", page)
            }.decode()

    companion object {
        /** The trailing slash makes endpoint paths resolve under `/3/`. */
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val DEFAULT_LANGUAGE = "en-US"
    }
}
