package com.raikar.moviegallery.ui.common

import com.raikar.moviegallery.domain.model.AppError
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the full-screen error copy, moved here from `:app`'s `StateViews` so iOS shows the same
 * text (add-ios-app-shell design, decision 8). The expected strings are the literals `StateViews`
 * held before the move.
 */
class ErrorCopyTest {
    @Test
    fun `every error has the copy Android showed before the move`() {
        val expected =
            listOf(
                AppError.Network to
                    ("No connection" to "Check your internet connection and try again."),
                AppError.Unauthorized to
                    (
                        "TMDB rejected the request" to
                            "Your TMDB token is missing or invalid. Check the token this build was set up with."
                    ),
                AppError.AuthRequired to
                    (
                        "Sign-in required" to
                            "Your session couldn't be verified. Sign out and sign back in, then try again."
                    ),
                AppError.NotFound to ("Not found" to "TMDB doesn't have a record for this title."),
                AppError.Serialization to ("Unexpected response" to "TMDB returned data this app couldn't read."),
                AppError.Http(503) to ("Something went wrong" to "TMDB responded with an error (HTTP 503)."),
                AppError.Unknown(IllegalStateException("boom")) to
                    ("Something went wrong" to "An unexpected error occurred."),
            )

        expected.forEach { (error, copy) ->
            assertEquals(copy.first, error.errorTitle, "title for $error")
            assertEquals(copy.second, error.errorDetail, "detail for $error")
        }
    }
}
