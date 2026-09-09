package com.raikar.moviegallery.domain.usecase

import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.repository.AiChatRepository
import javax.inject.Inject

class SendChatMessageUseCase
    @Inject
    constructor(
        private val aiChatRepository: AiChatRepository,
    ) {
        suspend operator fun invoke(
            history: List<ChatTurn>,
            message: String,
        ): DataResult<String> = aiChatRepository.sendMessage(history = history, message = message)
    }
