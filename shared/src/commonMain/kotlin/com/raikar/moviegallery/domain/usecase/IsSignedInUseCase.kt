package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.repository.AuthRepository

class IsSignedInUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Boolean = authRepository.isSignedIn
}
