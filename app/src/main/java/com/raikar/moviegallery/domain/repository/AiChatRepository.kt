package com.raikar.moviegallery.domain.repository

import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult

interface AiChatRepository {
    /**
     * Sends [message] with [history] as prior context and returns the assistant's
     * reply text.
     *
     * The history is passed in per call rather than held here, which keeps the
     * repository stateless: the ViewModel stays the single owner of conversation
     * state, and the whole thing is testable with a plain fake.
     */
    suspend fun sendMessage(
        history: List<ChatTurn>,
        message: String,
    ): DataResult<String>
}
