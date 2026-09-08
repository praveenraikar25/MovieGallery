package com.raikar.moviegallery.ui.screens.chat

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.usecase.SendChatMessageUseCase
import com.raikar.moviegallery.testutil.FakeAiChatRepository
import com.raikar.moviegallery.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAiChatRepository()

    private fun viewModel() = AiChatViewModel(SendChatMessageUseCase(repository))

    @Test
    fun `starts with only the local greeting`() {
        val state = viewModel().uiState.value

        assertEquals(1, state.messages.size)
        assertFalse(state.messages.single().isUser)
        assertFalse(state.isSending)
        assertNull(state.error)
    }

    @Test
    fun `appends the user message and the reply`() =
        runTest {
            repository.result = DataResult.Success("Try Arrival (2016).")
            val viewModel = viewModel()

            viewModel.sendMessage("something cerebral")

            val messages = viewModel.uiState.value.messages
            assertEquals(3, messages.size)
            assertEquals("something cerebral" to true, messages[1].text to messages[1].isUser)
            assertEquals("Try Arrival (2016)." to false, messages[2].text to messages[2].isUser)
            assertFalse(viewModel.uiState.value.isSending)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `sends prior turns as history but never the greeting or the current message`() =
        runTest {
            val viewModel = viewModel()

            viewModel.sendMessage("first")
            viewModel.sendMessage("second")

            assertEquals("second", repository.lastMessage)
            assertEquals(
                listOf(
                    ChatTurn(text = "first", isUser = true),
                    ChatTurn(text = "A reply", isUser = false),
                ),
                repository.lastHistory,
            )
        }

    @Test
    fun `trims the outgoing message`() =
        runTest {
            viewModel().sendMessage("  padded  ")

            assertEquals("padded", repository.lastMessage)
        }

    @Test
    fun `ignores blank input`() =
        runTest {
            val viewModel = viewModel()

            viewModel.sendMessage("   ")

            assertEquals(0, repository.callCount)
            assertEquals(1, viewModel.uiState.value.messages.size)
        }

    @Test
    fun `keeps the user message and reports the error on failure`() =
        runTest {
            repository.result = DataResult.Failure(AppError.Network)
            val viewModel = viewModel()

            viewModel.sendMessage("hello")

            val state = viewModel.uiState.value
            assertEquals(2, state.messages.size)
            assertTrue(state.messages.last().isUser)
            assertEquals(AppError.Network, state.error)
            assertFalse(state.isSending)
        }

    @Test
    fun `retry resends the last user message without duplicating it`() =
        runTest {
            repository.result = DataResult.Failure(AppError.Network)
            val viewModel = viewModel()
            viewModel.sendMessage("hello")

            repository.result = DataResult.Success("Welcome back.")
            viewModel.retry()

            val state = viewModel.uiState.value
            assertEquals("hello", repository.lastMessage)
            assertEquals(listOf("hello", "Welcome back."), state.messages.drop(1).map { it.text })
            assertNull(state.error)
        }

    @Test
    fun `retry sends history that excludes the failed turn`() =
        runTest {
            val viewModel = viewModel()
            viewModel.sendMessage("first")
            repository.result = DataResult.Failure(AppError.Network)
            viewModel.sendMessage("second")

            repository.result = DataResult.Success("ok")
            viewModel.retry()

            assertEquals("second", repository.lastMessage)
            assertEquals(
                listOf(
                    ChatTurn(text = "first", isUser = true),
                    ChatTurn(text = "A reply", isUser = false),
                ),
                repository.lastHistory,
            )
        }

    @Test
    fun `dismissError clears the error`() =
        runTest {
            repository.result = DataResult.Failure(AppError.Network)
            val viewModel = viewModel()
            viewModel.sendMessage("hello")

            viewModel.dismissError()

            assertNull(viewModel.uiState.value.error)
        }
}
