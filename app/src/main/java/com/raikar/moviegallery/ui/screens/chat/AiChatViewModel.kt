package com.raikar.moviegallery.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.usecase.IdentifyMoviePosterUseCase
import com.raikar.moviegallery.domain.usecase.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

/**
 * A local welcome bubble, not part of the conversation: it is never sent to the
 * model as history, so the model is not primed to imitate its phrasing.
 */
private const val GREETING_ID = 1L

private val greeting =
    ChatMessage(
        id = GREETING_ID,
        text =
            "Hi! Tell me what you're in the mood for, or send a photo of a movie poster " +
                "and I'll identify it.",
        isUser = false,
    )

/**
 * Stands in for an image turn in the history sent to the model. The turn itself has
 * no text, and the SDK drops empty text parts, so an image turn would otherwise
 * reach the model as an empty message and desynchronise the user/model alternation.
 */
private const val IMAGE_TURN_PLACEHOLDER = "[sent a photo of a movie poster]"

@HiltViewModel
class AiChatViewModel
    @Inject
    constructor(
        private val sendChatMessage: SendChatMessageUseCase,
        private val identifyMoviePoster: IdentifyMoviePosterUseCase,
    ) : ViewModel() {
        private val nextId = AtomicLong(GREETING_ID + 1L)

        private val _uiState = MutableStateFlow(ChatUiState(messages = listOf(greeting)))
        val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

        fun sendMessage(text: String) {
            val trimmed = text.trim()
            if (trimmed.isEmpty() || _uiState.value.isSending) return

            // The history sent to the model is everything already exchanged, which
            // excludes both the local greeting and the message being sent right now.
            val history = _uiState.value.messages.toHistory()
            val userMessage = ChatMessage(id = nextId.getAndIncrement(), text = trimmed, isUser = true)
            appendAndMarkSending(userMessage, isIdentifyingPoster = false)

            viewModelScope.launch {
                handle(sendChatMessage(history = history, message = trimmed))
            }
        }

        /** Sends a gallery pick or camera capture at [imageUri] for the model to identify. */
        fun sendPoster(imageUri: String) {
            if (_uiState.value.isSending) return

            val history = _uiState.value.messages.toHistory()
            val userMessage =
                ChatMessage(id = nextId.getAndIncrement(), text = "", isUser = true, imageUri = imageUri)
            appendAndMarkSending(userMessage, isIdentifyingPoster = true)

            viewModelScope.launch {
                handle(identifyMoviePoster(history = history, imageUri = imageUri))
            }
        }

        /** Re-sends the last user message — text or poster — after a failure. */
        fun retry() {
            val state = _uiState.value
            if (state.isSending) return
            val lastUserMessage = state.messages.lastOrNull { it.isUser } ?: return

            // Drop the failed turn first: the send re-appends it, and it must not appear
            // in the history that send derives either.
            _uiState.value =
                state.copy(
                    messages = state.messages.filterNot { it.id == lastUserMessage.id },
                    error = null,
                )
            val imageUri = lastUserMessage.imageUri
            if (imageUri != null) {
                sendPoster(imageUri)
            } else {
                sendMessage(lastUserMessage.text)
            }
        }

        fun dismissError() {
            _uiState.value = _uiState.value.copy(error = null)
        }

        private fun appendAndMarkSending(
            userMessage: ChatMessage,
            isIdentifyingPoster: Boolean,
        ) {
            _uiState.value =
                _uiState.value.copy(
                    messages = _uiState.value.messages + userMessage,
                    isSending = true,
                    isIdentifyingPoster = isIdentifyingPoster,
                    error = null,
                )
        }

        private fun handle(result: DataResult<String>) {
            _uiState.value =
                when (result) {
                    is DataResult.Success -> {
                        val reply = ChatMessage(id = nextId.getAndIncrement(), text = result.data, isUser = false)
                        _uiState.value.copy(
                            messages = _uiState.value.messages + reply,
                            isSending = false,
                            isIdentifyingPoster = false,
                        )
                    }
                    // The user's own message stays on screen so retry has something to
                    // resend, and so their typing or photo is not silently thrown away.
                    is DataResult.Failure ->
                        _uiState.value.copy(
                            isSending = false,
                            isIdentifyingPoster = false,
                            error = result.error,
                        )
                }
        }

        private fun List<ChatMessage>.toHistory(): List<ChatTurn> =
            filterNot { it.id == GREETING_ID }.map { message ->
                ChatTurn(
                    text = if (message.imageUri != null) IMAGE_TURN_PLACEHOLDER else message.text,
                    isUser = message.isUser,
                )
            }
    }
