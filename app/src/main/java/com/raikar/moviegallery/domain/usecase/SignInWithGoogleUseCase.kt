package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.AuthResult
import com.raikar.moviegallery.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(idToken: String): AuthResult<Unit> = authRepository.signInWithGoogle(idToken)
    }
