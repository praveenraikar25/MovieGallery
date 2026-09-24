package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.TvShowPage
import com.raikar.moviegallery.domain.repository.TvShowRepository

class FakeTvShowRepository(
    private val pages: Map<Int, DataResult<TvShowPage>> = emptyMap(),
) : TvShowRepository {
    var topTvShowsCalls = 0
        private set

    override suspend fun topTvShows(page: Int): DataResult<TvShowPage> {
        topTvShowsCalls++
        return pages[page] ?: DataResult.Failure(AppError.Unknown(IllegalStateException("No page $page programmed")))
    }
}
