package com.raikar.moviegallery.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult
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
            "Hi! Tell me what you're in the mood for — a genre, a mood, or a movie you loved — " +
                "and I'll find something for you.",
        isUser = false,
    )

@HiltViewModel
class AiChatViewModel
    @Inject
    constructor(
        private val sendChatMessage: SendChatMessageUseCase,
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
            _uiState.value =
                _uiState.value.copy(
                    messages = _uiState.value.messages + userMessage,
                    isSending = true,
                    error = null,
                )

            viewModelScope.launch {
                when (val result = sendChatMessage(history = history, message = trimmed)) {
                    is DataResult.Success -> {
                        val reply = ChatMessage(id = nextId.getAndIncrement(), text = result.data, isUser = false)
                        _uiState.value =
                            _uiState.value.copy(
                                messages = _uiState.value.messages + reply,
                                isSending = false,
                            )
                    }
                    // The user's own message stays on screen so retry has something to
                    // resend, and so their typing is not silently thrown away.
                    is DataResult.Failure ->
                        _uiState.value = _uiState.value.copy(isSending = false, error = result.error)
                }
            }
        }

        /** Re-sends the last user message after a failure. */
        fun retry() {
            val state = _uiState.value
            if (state.isSending) return
            val lastUserMessage = state.messages.lastOrNull { it.isUser } ?: return

            // Drop the failed turn first: sendMessage() re-appends it, and it must not
            // appear in the history it derives either.
            _uiState.value =
                state.copy(
                    messages = state.messages.filterNot { it.id == lastUserMessage.id },
                    error = null,
                )
            sendMessage(lastUserMessage.text)
        }

        fun dismissError() {
            _uiState.value = _uiState.value.copy(error = null)
        }

        private fun List<ChatMessage>.toHistory(): List<ChatTurn> =
            filterNot { it.id == GREETING_ID }.map { ChatTurn(text = it.text, isUser = it.isUser) }
    }
