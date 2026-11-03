package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.domain.model.CountryDetails
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.repository.CountryRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * Returns [results] per country code, else [defaultResult]. A code with an entry in [gates]
 * suspends until that gate completes, so tests can hold a load in flight.
 */
class FakeCountryRepository(
    var defaultResult: DataResult<CountryDetails?> = DataResult.Success(null),
    val results: MutableMap<String, DataResult<CountryDetails?>> = mutableMapOf(),
    val gates: MutableMap<String, CompletableDeferred<Unit>> = mutableMapOf(),
) : CountryRepository {
    val requestedCodes = mutableListOf<String>()

    override suspend fun getCountryDetails(code: String): DataResult<CountryDetails?> {
        requestedCodes += code
        gates[code]?.await()
        return results[code] ?: defaultResult
    }
}
