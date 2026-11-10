package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.data.remote.ai.BridgedAiException
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.platform.AiFailureKind
import com.raikar.moviegallery.platform.BridgedAiFailure
import com.raikar.moviegallery.testutil.FakeFirebaseAiBridge
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * iOS chat must fail the way Android does: each failure kind maps to the `AppError` Android's
 * `safeAiCall` gives the equivalent exception (add-ios-platform-layer design, decision 3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IosAiChatRepositoryTest {
    private val bridge = FakeFirebaseAiBridge()
    private val repository = IosAiChatRepository(bridge, UnconfinedTestDispatcher())

    @Test
    fun `a reply is trimmed and the history and message reach the bridge`() =
        runTest {
            bridge.reply = "  Heat (1995)\n"

            assertEquals(DataResult.Success("Heat (1995)"), repository.sendMessage(HISTORY, "A heist film?"))
            assertEquals(HISTORY, bridge.lastHistory)
            assertEquals("A heist film?", bridge.lastMessage)
        }

    @Test
    fun `a null reply is Unknown`() =
        runTest {
            bridge.reply = null

            assertUnknown(repository.sendMessage(HISTORY, "Hi"), "The model returned no text")
        }

    @Test
    fun `a blank reply is Unknown`() =
        runTest {
            bridge.reply = "   "

            assertUnknown(repository.sendMessage(HISTORY, "Hi"), "The model returned no text")
        }

    @Test
    fun `Unauthorized maps to Unauthorized`() = assertFailsWith(AiFailureKind.Unauthorized, AppError.Unauthorized)

    @Test
    fun `Network maps to Network`() = assertFailsWith(AiFailureKind.Network, AppError.Network)

    @Test
    fun `Serialization maps to Serialization`() = assertFailsWith(AiFailureKind.Serialization, AppError.Serialization)

    @Test
    fun `a timeout is Unknown rather than Network`() =
        runTest {
            bridge.failure = BridgedAiFailure(AiFailureKind.Timeout, "The request timed out")

            assertUnknown(repository.sendMessage(HISTORY, "Hi"), "The request timed out")
        }

    @Test
    fun `an unreadable image is Unknown rather than Network and the URI reaches the bridge`() =
        runTest {
            bridge.failure = BridgedAiFailure(AiFailureKind.ImageUnreadable, "Couldn't read the image")

            assertUnknown(repository.identifyPoster(HISTORY, "file:///tmp/poster.jpg"), "Couldn't read the image")
            assertEquals("file:///tmp/poster.jpg", bridge.lastImageUri)
            assertEquals(HISTORY, bridge.lastHistory)
        }

    @Test
    fun `any other failure is Unknown and keeps the model's explanation`() =
        runTest {
            bridge.failure = BridgedAiFailure(AiFailureKind.Other, "The prompt was blocked")

            val error = assertUnknown(repository.sendMessage(HISTORY, "Hi"), "The prompt was blocked")
            assertEquals(AiFailureKind.Other, assertIs<BridgedAiException>(error.cause).kind)
        }

    @Test
    fun `a poster reply is trimmed like a text reply`() =
        runTest {
            bridge.reply = "Alien (1979) "

            assertEquals(DataResult.Success("Alien (1979)"), repository.identifyPoster(HISTORY, "file:///p.jpg"))
        }

    @Test
    fun `a completion fired twice is harmless and the first result wins`() =
        runTest {
            bridge.completeTwice = true

            assertEquals(DataResult.Success("A reply"), repository.sendMessage(HISTORY, "Hi"))
        }

    private fun assertFailsWith(
        kind: AiFailureKind,
        expected: AppError,
    ) = runTest {
        bridge.failure = BridgedAiFailure(kind, "Firebase says no")

        assertEquals(DataResult.Failure(expected), repository.sendMessage(HISTORY, "Hi"))
    }

    private fun assertUnknown(
        result: DataResult<String>,
        message: String,
    ): AppError.Unknown {
        val error = assertIs<AppError.Unknown>(assertIs<DataResult.Failure>(result).error)
        assertEquals(message, error.cause.message)
        return error
    }

    private companion object {
        val HISTORY = listOf(ChatTurn("Hello", isUser = false), ChatTurn("Something tense", isUser = true))
    }
}
