package com.raikar.moviegallery.domain.model

/**
 * One exchanged message in an AI chat conversation, in the domain's own terms so
 * that nothing above the data layer depends on a Gemini type.
 */
data class ChatTurn(
    val text: String,
    val isUser: Boolean,
)
