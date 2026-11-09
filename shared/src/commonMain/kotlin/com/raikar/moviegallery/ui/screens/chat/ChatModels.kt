package com.raikar.moviegallery.ui.screens.chat

import com.raikar.moviegallery.domain.model.AppError

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    /**
     * Set on a message the user sent as a photo. The bubble renders the image in
     * place of text, and [text] is empty for these.
     */
    val imageUri: String? = null,
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    /**
     * Narrows the in-flight state: identifying a poster is slower than answering
     * text, so the typing bubble says what it is waiting for.
     */
    val isIdentifyingPoster: Boolean = false,
    /**
     * A failed send. Surfaced beside the input rather than replacing the screen,
     * so the conversation so far stays readable, and cleared on the next attempt.
     */
    val error: AppError? = null,
)
