package com.raikar.moviegallery.domain.repository

import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult

interface AiChatRepository {
    /**
     * Sends [message] with [history] as prior context and returns the assistant's
     * reply text.
     *
     * The history is passed in per call rather than held here, which keeps the
     * repository stateless: the ViewModel stays the single owner of conversation
     * state, and the whole thing is testable with a plain fake.
     */
    suspend fun sendMessage(
        history: List<ChatTurn>,
        message: String,
    ): DataResult<String>

    /**
     * Sends the image at [imageUri] for the model to identify, with [history] as
     * prior context, and returns the assistant's reply text.
     *
     * [imageUri] is an opaque `content://` or `file://` string so that nothing above
     * the data layer touches a `ContentResolver` or a `Bitmap`; resolving it is the
     * implementation's job.
     *
     * The image is not added to [ChatTurn], and so is never replayed on later turns:
     * it is context for this one exchange, and the model's own reply already carries
     * whatever it identified forward into the history.
     */
    suspend fun identifyPoster(
        history: List<ChatTurn>,
        imageUri: String,
    ): DataResult<String>
}
