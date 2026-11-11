package com.raikar.moviegallery.ui.common

import com.raikar.moviegallery.domain.model.AppError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Pins the chat copy moved from `:app`'s `AiChatScreen` (add-ios-chat-and-google-signin design, decision 5). */
class ChatCopyTest {
    @Test
    fun `a network failure asks the user to check their connection`() {
        assertEquals("No connection — check your internet and try again.", AppError.Network.chatErrorMessage)
    }

    @Test
    fun `an unauthorized failure names the setup steps`() {
        assertEquals(
            "Firebase rejected the request. Check that AI Logic is enabled and this device's " +
                "App Check debug token is registered.",
            AppError.Unauthorized.chatErrorMessage,
        )
    }

    @Test
    fun `an auth failure asks the user to sign in again`() {
        assertEquals("Sign in again to chat with the assistant.", AppError.AuthRequired.chatErrorMessage)
    }

    @Test
    fun `an unreadable reply says so`() {
        assertEquals("The assistant sent a reply this app couldn't read.", AppError.Serialization.chatErrorMessage)
    }

    @Test
    fun `not found reads as unavailable`() {
        assertEquals("The assistant is unavailable right now.", AppError.NotFound.chatErrorMessage)
    }

    @Test
    fun `an HTTP failure includes its status code`() {
        assertEquals("The assistant is unavailable right now (HTTP 503).", AppError.Http(503).chatErrorMessage)
    }

    @Test
    fun `an unknown failure offers retry`() {
        assertEquals(
            "Couldn't get a reply. Tap retry to try again.",
            AppError.Unknown(IllegalStateException("boom")).chatErrorMessage,
        )
    }

    @Test
    fun `identifying a poster labels the typing bubble`() {
        assertEquals("Identifying poster…", chatTypingLabel(isIdentifyingPoster = true))
    }

    @Test
    fun `a text reply leaves the typing bubble unlabelled`() {
        assertNull(chatTypingLabel(isIdentifyingPoster = false))
    }
}
