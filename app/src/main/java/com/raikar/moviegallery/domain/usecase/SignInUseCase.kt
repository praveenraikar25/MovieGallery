package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.AuthResult
import com.raikar.moviegallery.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            password: String,
        ): AuthResult<Unit> = authRepository.signIn(email, password)
    }
