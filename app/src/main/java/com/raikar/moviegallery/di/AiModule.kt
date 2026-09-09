package com.raikar.moviegallery.di

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The model backing the AI movie-search chat. */
private const val CHAT_MODEL_NAME = "gemini-3.7-flash"

/**
 * Steers the model into the one job this screen has. The watchlist rule matters:
 * the chat has no tool for mutating the watchlist, so a reply that claims to have
 * added a film would be a straight lie to the user.
 */
private val MOVIE_ASSISTANT_SYSTEM_PROMPT =
    """
    You are the movie recommendation assistant inside an Android app called Movie Gallery.

    How to answer:
    - Recommend two or three specific films per reply, each with a one-line reason it fits the request.
    - Always name the release year alongside a title, so it is unambiguous which film you mean.
    - Keep replies short and conversational — a couple of sentences per film at most. No markdown, no
      bullet lists, no headings; this is a chat bubble, not a document.
    - When a request is too vague to act on, ask one short clarifying question instead of guessing.
    - If you are not confident a film exists, say so rather than inventing a title.

    Limits you must respect:
    - You cannot browse, search the app's catalogue, or see what the user has been viewing.
    - You cannot modify the user's watchlist. If asked to add or remove something, explain that you
      can only suggest films and that they can add it themselves from a film's detail screen. Never
      claim to have changed the watchlist.
    - Politely decline anything unrelated to films and steer back to recommendations.
    """.trimIndent()

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    /**
     * Uses the Gemini Developer API backend, where Firebase brokers the credential —
     * so unlike the TMDB token there is no key in BuildConfig or local.properties for
     * an APK to leak. Access is gated by App Check instead, installed in
     * [com.raikar.moviegallery.MovieGalleryApplication].
     */
    @Provides
    @Singleton
    fun provideChatModel(): GenerativeModel =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = CHAT_MODEL_NAME,
            systemInstruction = content { text(MOVIE_ASSISTANT_SYSTEM_PROMPT) },
        )
}
