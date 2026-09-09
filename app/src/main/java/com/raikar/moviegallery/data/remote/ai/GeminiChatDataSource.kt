package com.raikar.moviegallery.data.remote.ai

import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.raikar.moviegallery.domain.model.ChatTurn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only place that speaks Gemini. Everything above it sees [ChatTurn] and
 * [com.raikar.moviegallery.domain.model.DataResult].
 */
@Singleton
class GeminiChatDataSource
    @Inject
    constructor(
        private val model: GenerativeModel,
    ) {
        /**
         * Starts a fresh [com.google.firebase.ai.Chat] seeded with [history] on every
         * call. Rebuilding it is cheap and local — `startChat` makes no network
         * request — and it keeps this data source stateless, so two conversations can
         * never bleed into one another.
         *
         * Returns the reply text, which the SDK leaves nullable when a candidate came
         * back without any text part.
         */
        suspend fun sendMessage(
            history: List<ChatTurn>,
            message: String,
        ): String? {
            val chat = model.startChat(history = history.map { it.toContent() })
            return chat.sendMessage(message).text
        }

        private fun ChatTurn.toContent(): Content =
            content(role = if (isUser) ROLE_USER else ROLE_MODEL) { text(this@toContent.text) }

        private companion object {
            const val ROLE_USER = "user"
            const val ROLE_MODEL = "model"
        }
    }
