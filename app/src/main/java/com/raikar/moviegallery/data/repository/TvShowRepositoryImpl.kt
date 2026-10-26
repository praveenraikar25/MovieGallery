package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.data.remote.BffApi
import com.raikar.moviegallery.data.remote.safeApiCall
import com.raikar.moviegallery.data.remote.toDomain
import com.raikar.moviegallery.di.qualifier.IoDispatcher
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.TvShowPage
import com.raikar.moviegallery.domain.repository.TvShowRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvShowRepositoryImpl
    @Inject
    constructor(
        private val api: BffApi,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : TvShowRepository {
        override suspend fun topTvShows(page: Int): DataResult<TvShowPage> =
            withContext(ioDispatcher) {
                when (val result = safeApiCall { api.topTvShows(page = page).toDomain() }) {
                    // The BFF 401 means our token was rejected, not that a TMDB token is wrong.
                    DataResult.Failure(AppError.Unauthorized) -> DataResult.Failure(AppError.AuthRequired)
                    else -> result
                }
            }
    }
