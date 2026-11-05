package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.dto.TvShowListResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** The BFF demo host backing TV-show data — a separate service from TMDB — over a client from [createBffHttpClient]. */
class BffApi(
    private val client: HttpClient,
) {
    suspend fun topTvShows(page: Int = 1): TvShowListResponseDto =
        client.get("tv/top") { parameter("page", page) }.decode()

    companion object {
        /** The trailing slash makes endpoint paths resolve under it. */
        const val BASE_URL = "https://bffdemo.onrender.com/"
    }
}
