package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.CountryDetails
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.repository.CountryRepository

class GetCountryDetailsUseCase(
    private val countryRepository: CountryRepository,
) {
    suspend operator fun invoke(code: String): DataResult<CountryDetails?> = countryRepository.getCountryDetails(code)
}
