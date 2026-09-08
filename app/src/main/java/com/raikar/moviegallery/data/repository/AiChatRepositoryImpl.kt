package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.data.remote.ai.GeminiChatDataSource
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

@Singleton
class AiChatRepositoryImpl
    @Inject
    constructor(
        private val dataSource: GeminiChatDataSource,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : AiChatRepository {
        override suspend fun sendMessage(
            history: List<ChatTurn>,
            message: String,
        ): DataResult<String> =
            withContext(ioDispatcher) {
                when (val result = safeAiCall { dataSource.sendMessage(history, message) }) {
                    is DataResult.Failure -> result
                    is DataResult.Success -> {
                        // A candidate with no text part is a success as far as the SDK is
                        // concerned, but there is nothing to render, so treat it as a failure
                        // rather than appending an empty bubble.
                        val reply = result.data?.trim()
                        if (reply.isNullOrEmpty()) {
                            DataResult.Failure(AppError.Unknown(IllegalStateException("The model returned no text")))
                        } else {
                            DataResult.Success(reply)
                        }
                    }
                }
            }
    }
