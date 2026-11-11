package com.raikar.moviegallery.ui.common

import com.raikar.moviegallery.domain.model.AppError

// The chat's copy that both apps show, moved here from `:app`'s `AiChatScreen`
// (add-ios-chat-and-google-signin design, decision 5).

/** The error row's text for a failed send (ai-chat "Failures keep the message and offer retry"). */
val AppError.chatErrorMessage: String
    get() =
        when (this) {
            AppError.Network -> "No connection — check your internet and try again."
            // The likely causes are all setup rather than user error, so name them: an
            // unregistered App Check debug token is the usual one on a fresh checkout.
            AppError.Unauthorized ->
                "Firebase rejected the request. Check that AI Logic is enabled and this device's " +
                    "App Check debug token is registered."
            AppError.AuthRequired -> "Sign in again to chat with the assistant."
            AppError.Serialization -> "The assistant sent a reply this app couldn't read."
            AppError.NotFound -> "The assistant is unavailable right now."
            is AppError.Http -> "The assistant is unavailable right now (HTTP $code)."
            is AppError.Unknown -> "Couldn't get a reply. Tap retry to try again."
        }

/** The typing bubble's label: identifying a poster is slower than a text reply, so it says so. */
fun chatTypingLabel(isIdentifyingPoster: Boolean): String? = if (isIdentifyingPoster) "Identifying poster…" else null

/** Shown, with Dismiss and no Retry, when nothing on the device can take a photo. */
const val NO_CAMERA_APP_MESSAGE = "No app on this device can take a photo."

/** Shown, with Dismiss and no Retry, when nothing on the device can pick an image. */
const val NO_PICKER_APP_MESSAGE = "No app on this device can pick an image."
