package com.raikar.moviegallery.ui.screens.chat

import com.raikar.moviegallery.domain.model.AppError

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    /**
     * A failed send. Surfaced beside the input rather than replacing the screen,
     * so the conversation so far stays readable, and cleared on the next attempt.
     */
    val error: AppError? = null,
)
