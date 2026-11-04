package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.repository.AiChatRepository

class IdentifyMoviePosterUseCase(
    private val aiChatRepository: AiChatRepository,
) {
    suspend operator fun invoke(
        history: List<ChatTurn>,
        imageUri: String,
    ): DataResult<String> = aiChatRepository.identifyPoster(history = history, imageUri = imageUri)
}
