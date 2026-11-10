package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.data.remote.ai.mapBridgedAiFailure
import com.raikar.moviegallery.data.remote.ai.requireReplyText
import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.repository.AiChatRepository
import com.raikar.moviegallery.platform.BridgedAiFailure
import com.raikar.moviegallery.platform.FirebaseAiBridge
import com.raikar.moviegallery.platform.awaitBridge
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * [AiChatRepository] on iOS, over the Firebase AI SDK that Swift supplies as [FirebaseAiBridge].
 * It behaves as Android's `AiChatRepositoryImpl` does: failures map through [mapBridgedAiFailure],
 * and a reply with no text is a failure ([requireReplyText]).
 */
class IosAiChatRepository(
    private val bridge: FirebaseAiBridge,
    private val ioDispatcher: CoroutineDispatcher,
) : AiChatRepository {
    override suspend fun sendMessage(
        history: List<ChatTurn>,
        message: String,
    ): DataResult<String> = aiCall { bridge.sendMessage(history, message, it) }

    override suspend fun identifyPoster(
        history: List<ChatTurn>,
        imageUri: String,
    ): DataResult<String> = aiCall { bridge.identifyPoster(history, imageUri, it) }

    private suspend fun aiCall(call: (completion: (String?, BridgedAiFailure?) -> Unit) -> Unit): DataResult<String> =
        withContext(ioDispatcher) {
            val (text, failure) =
                awaitBridge<Pair<String?, BridgedAiFailure?>> { complete ->
                    call { reply, error -> complete(reply to error) }
                }
            when (failure) {
                null -> DataResult.Success(text).requireReplyText()
                else -> DataResult.Failure(mapBridgedAiFailure(failure))
            }
        }
}
