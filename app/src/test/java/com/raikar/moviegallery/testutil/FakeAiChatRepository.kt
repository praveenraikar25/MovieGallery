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
    var callCount = 0
        private set

    override suspend fun sendMessage(
        history: List<ChatTurn>,
        message: String,
    ): DataResult<String> {
        lastHistory = history
        lastMessage = message
        callCount++
        return result
    }
}
