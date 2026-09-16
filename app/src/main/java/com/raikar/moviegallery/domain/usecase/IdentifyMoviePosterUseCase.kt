package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.repository.AiChatRepository
import javax.inject.Inject

class IdentifyMoviePosterUseCase
    @Inject
    constructor(
        private val aiChatRepository: AiChatRepository,
    ) {
        suspend operator fun invoke(
            history: List<ChatTurn>,
            imageUri: String,
        ): DataResult<String> = aiChatRepository.identifyPoster(history = history, imageUri = imageUri)
    }
