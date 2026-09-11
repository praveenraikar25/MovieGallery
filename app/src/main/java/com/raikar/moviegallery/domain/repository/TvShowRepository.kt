package com.raikar.moviegallery.domain.repository

import com.raikar.moviegallery.domain.model.TvShow

interface TvShowRepository {
    /** Top-rated shows, already ordered best-first. */
    suspend fun topTvShows(): List<TvShow>
}
