package com.raikar.moviegallery.domain.repository

import com.raikar.moviegallery.domain.model.CountryDetails
import com.raikar.moviegallery.domain.model.DataResult

interface CountryRepository {
    /**
     * Details for an ISO 3166-1 alpha-2 [code]. `Success(null)` means the source has no record
     * of the code (a retired one such as `SU`), which is not a failure and is not worth retrying.
     */
    suspend fun getCountryDetails(code: String): DataResult<CountryDetails?>
}
