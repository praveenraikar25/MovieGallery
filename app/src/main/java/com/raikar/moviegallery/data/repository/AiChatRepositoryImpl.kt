package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.data.remote.ai.GeminiChatDataSource
import com.raikar.moviegallery.data.remote.ai.PosterImageDecoder
import com.raikar.moviegallery.data.remote.ai.safeAiCall
import com.raikar.moviegallery.di.qualifier.IoDispatcher
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.repository.AiChatRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class AiChatRepositoryImpl
    @Inject
    constructor(
        private val dataSource: GeminiChatDataSource,
        private val imageDecoder: PosterImageDecoder,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : AiChatRepository {
        override suspend fun sendMessage(
            history: List<ChatTurn>,
            message: String,
        ): DataResult<String> =
            withContext(ioDispatcher) {
                safeAiCall { dataSource.sendMessage(history, message) }.requireText()
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
                safeAiCall { dataSource.identifyPoster(history, image) }.requireText()
            }

        /**
         * A candidate with no text part is a success as far as the SDK is concerned, but
         * there is nothing to render, so treat it as a failure rather than appending an
         * empty bubble.
         */
        private fun DataResult<String?>.requireText(): DataResult<String> =
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
    }
