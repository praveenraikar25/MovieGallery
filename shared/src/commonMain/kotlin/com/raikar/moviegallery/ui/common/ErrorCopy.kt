package com.raikar.moviegallery.ui.common

import com.raikar.moviegallery.domain.model.AppError

/*
 * The full-screen error copy for each AppError, shared so Android's StateViews and iOS's
 * ErrorStateView show the same text.
 */

/** The error's headline. */
val AppError.errorTitle: String
    get() =
        when (this) {
            AppError.Network -> "No connection"
            AppError.Unauthorized -> "TMDB rejected the request"
            AppError.AuthRequired -> "Sign-in required"
            AppError.NotFound -> "Not found"
            AppError.Serialization -> "Unexpected response"
            is AppError.Http -> "Something went wrong"
            is AppError.Unknown -> "Something went wrong"
        }

/** The sentence under [errorTitle] saying what to do about it. */
val AppError.errorDetail: String
    get() =
        when (this) {
            AppError.Network -> "Check your internet connection and try again."
            // The overwhelmingly likely cause on a fresh checkout, so name it directly rather than
            // making someone decode a 401. Platform-neutral: each app reads the token from its own
            // build setup (local.properties on Android, Secrets.xcconfig on iOS).
            AppError.Unauthorized ->
                "Your TMDB token is missing or invalid. Check the token this build was set up with."
            AppError.AuthRequired -> "Your session couldn't be verified. Sign out and sign back in, then try again."
            AppError.NotFound -> "TMDB doesn't have a record for this title."
            AppError.Serialization -> "TMDB returned data this app couldn't read."
            is AppError.Http -> "TMDB responded with an error (HTTP $code)."
            is AppError.Unknown -> "An unexpected error occurred."
        }
