package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.repository.AiChatRepository

/** Records what the ViewModel sent so history handling can be asserted on. */
class FakeAiChatRepository(
    var result: DataResult<String> = DataResult.Success("A reply"),
) : AiChatRepository {
    var lastHistory: List<ChatTurn>? = null
        private set
    var lastMessage: String? = null
        private set
    var lastImageUri: String? = null
        private set
    var callCount = 0
        private set

    /** Which method the ViewModel last routed to — the distinction retry() has to get right. */
    var lastCallWasPoster = false
        private set

    /**
     * Invoked at the start of a call, while the ViewModel's in-flight state is still
     * set. Lets a test observe that state without a suspending gate, which the
     * unconfined dispatcher would otherwise run straight through.
     */
    var onCall: (() -> Unit)? = null

    override suspend fun sendMessage(
        history: List<ChatTurn>,
        message: String,
    ): DataResult<String> {
        lastHistory = history
        lastMessage = message
        lastCallWasPoster = false
        callCount++
        onCall?.invoke()
        return result
    }

    override suspend fun identifyPoster(
        history: List<ChatTurn>,
        imageUri: String,
    ): DataResult<String> {
        lastHistory = history
        lastImageUri = imageUri
        lastCallWasPoster = true
        callCount++
        onCall?.invoke()
        return result
    }
}
