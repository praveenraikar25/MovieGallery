package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.CurrentUser
import com.raikar.moviegallery.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveCurrentUserUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<CurrentUser?> = authRepository.currentUser
}
