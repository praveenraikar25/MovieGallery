package com.raikar.moviegallery.data.repository

import com.apollographql.apollo.ApolloClient
import com.raikar.moviegallery.data.remote.graphql.countries.CountryDetailsQuery
import com.raikar.moviegallery.data.remote.safeGraphQlCall
import com.raikar.moviegallery.data.remote.toDomain
import com.raikar.moviegallery.di.qualifier.IoDispatcher
import com.raikar.moviegallery.domain.model.CountryDetails
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.repository.CountryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CountryRepositoryImpl
    @Inject
    constructor(
        private val apollo: ApolloClient,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : CountryRepository {
        /**
         * Country data does not change within a session, so successful lookups (including "no
         * record", stored as null) are kept for the process lifetime. Failures are not cached.
         */
        private val cache = mutableMapOf<String, CountryDetails?>()
        private val cacheLock = Mutex()

        override suspend fun getCountryDetails(code: String): DataResult<CountryDetails?> {
            val key = code.uppercase()
            cacheLock.withLock {
                if (key in cache) return DataResult.Success(cache[key])
            }
            val result =
                withContext(ioDispatcher) {
                    safeGraphQlCall { apollo.query(CountryDetailsQuery(code = key)).execute() }
                }
            return when (result) {
                is DataResult.Success -> {
                    val details = result.data.country?.toDomain()
                    cacheLock.withLock { cache[key] = details }
                    DataResult.Success(details)
                }
                is DataResult.Failure -> result
            }
        }
    }
