package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.repository.AuthRepository
import javax.inject.Inject

class IsSignedInUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        operator fun invoke(): Boolean = authRepository.isSignedIn
    }
