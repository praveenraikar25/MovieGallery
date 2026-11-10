package com.raikar.moviegallery.data.remote.ai

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult

/**
 * A candidate with no text part is a success as far as the SDK is concerned, but
 * there is nothing to render, so treat it as a failure rather than appending an
 * empty bubble. Both platforms' chat repositories apply it to every reply; a non-empty
 * reply comes back trimmed.
 */
fun DataResult<String?>.requireReplyText(): DataResult<String> =
    when (this) {
        is DataResult.Failure -> this
        is DataResult.Success -> {
            val reply = data?.trim()
            if (reply.isNullOrEmpty()) {
                DataResult.Failure(AppError.Unknown(IllegalStateException("The model returned no text")))
            } else {
                DataResult.Success(reply)
            }
        }
    }
