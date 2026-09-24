package com.raikar.moviegallery.domain.repository

import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.TvShowPage

interface TvShowRepository {
    /** Top-rated shows, already ordered best-first. */
    suspend fun topTvShows(page: Int): DataResult<TvShowPage>
}
