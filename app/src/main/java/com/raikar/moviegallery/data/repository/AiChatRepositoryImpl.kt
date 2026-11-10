package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.data.remote.ai.GeminiChatDataSource
import com.raikar.moviegallery.data.remote.ai.PosterImageDecoder
import com.raikar.moviegallery.data.remote.ai.requireReplyText
import com.raikar.moviegallery.data.remote.ai.safeAiCall
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.repository.AiChatRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class AiChatRepositoryImpl(
    private val dataSource: GeminiChatDataSource,
    private val imageDecoder: PosterImageDecoder,
    private val ioDispatcher: CoroutineDispatcher,
) : AiChatRepository {
    override suspend fun sendMessage(
        history: List<ChatTurn>,
        message: String,
    ): DataResult<String> =
        withContext(ioDispatcher) {
            safeAiCall { dataSource.sendMessage(history, message) }.requireReplyText()
        }

    override suspend fun identifyPoster(
        history: List<ChatTurn>,
        imageUri: String,
    ): DataResult<String> =
        withContext(ioDispatcher) {
            // Decoding is caught separately rather than inside safeAiCall: an
            // unreadable URI throws IOException, which safeAiCall reads as a network
            // failure and would have the UI offer "check your internet".
            val image =
                try {
                    imageDecoder.decode(imageUri)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    return@withContext DataResult.Failure(AppError.Unknown(e))
                }
            safeAiCall { dataSource.identifyPoster(history, image) }.requireReplyText()
        }
}
