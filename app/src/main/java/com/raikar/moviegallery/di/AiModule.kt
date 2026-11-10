package com.raikar.moviegallery.di

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.content
import com.raikar.moviegallery.data.remote.ai.ChatModelConfig
import com.raikar.moviegallery.data.remote.ai.GeminiChatDataSource
import com.raikar.moviegallery.data.remote.ai.PosterImageDecoder
import com.raikar.moviegallery.data.repository.AiChatRepositoryImpl
import com.raikar.moviegallery.domain.repository.AiChatRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/** Names this app in the shared system prompt ([ChatModelConfig.systemPrompt]). */
private const val PLATFORM_NAME = "Android"

/** The AI movie-search chat: the Firebase AI model and the data sources over it. */
val aiModule =
    module {
        /*
         * Uses the Gemini Developer API backend, where Firebase brokers the credential —
         * so unlike the TMDB token there is no key in BuildConfig or local.properties for
         * an APK to leak. Access is gated by App Check instead, installed in
         * [com.raikar.moviegallery.MovieGalleryApplication].
         */
        single {
            Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = ChatModelConfig.MODEL_NAME,
                systemInstruction = content { text(ChatModelConfig.systemPrompt(PLATFORM_NAME)) },
                requestOptions = RequestOptions(timeoutInMillis = ChatModelConfig.REQUEST_TIMEOUT_MILLIS),
            )
        }
        singleOf(::GeminiChatDataSource)
        single { PosterImageDecoder(androidContext()) }
        single<AiChatRepository> { AiChatRepositoryImpl(get(), get(), get(IoDispatcher)) }
    }
