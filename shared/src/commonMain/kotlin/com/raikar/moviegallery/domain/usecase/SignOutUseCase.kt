package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.repository.AuthRepository

class SignOutUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke() = authRepository.signOut()
}
