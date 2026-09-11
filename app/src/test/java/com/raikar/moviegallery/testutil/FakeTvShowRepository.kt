package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.domain.model.TvShow
import com.raikar.moviegallery.domain.repository.TvShowRepository

class FakeTvShowRepository(
    private val shows: List<TvShow> = emptyList(),
) : TvShowRepository {
    var topTvShowsCalls = 0
        private set

    override suspend fun topTvShows(): List<TvShow> {
        topTvShowsCalls++
        return shows
    }
}
