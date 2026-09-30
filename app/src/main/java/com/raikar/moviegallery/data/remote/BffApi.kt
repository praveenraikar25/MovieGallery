package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.dto.TvShowListResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/** The BFF demo host backing TV-show data — a separate service from TMDB. */
interface BffApi {
    @GET("tv/top")
    suspend fun topTvShows(
        @Query("page") page: Int = 1,
    ): TvShowListResponseDto

    companion object {
        /** Trailing slash is required by Retrofit for relative endpoint paths. */
        const val BASE_URL = "https://bffdemo.onrender.com/"
    }
}
