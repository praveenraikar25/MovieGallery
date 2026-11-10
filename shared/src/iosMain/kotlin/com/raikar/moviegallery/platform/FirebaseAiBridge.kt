package com.raikar.moviegallery.platform

import com.raikar.moviegallery.domain.model.ChatTurn

/**
 * Firebase AI Logic as the iOS app supplies it. Firebase AI has no Kotlin Multiplatform SDK, so
 * Swift adopts this protocol with a `GenerativeModel` built from `ChatModelConfig` (model name,
 * timeout, `systemPrompt("iOS")`). Kotlin's `IosAiChatRepository` builds on it and maps failures
 * to the same errors Android shows (add-ios-platform-layer design, decisions 1 to 3).
 *
 * Each call starts a fresh chat seeded with the history, as Android's `GeminiChatDataSource` does,
 * so two conversations never bleed into one another. Each completion must be called exactly once,
 * with the reply text or a failure. It may be called on any queue. A second call is ignored.
 */
interface FirebaseAiBridge {
    /** Sends [message] with [history] as prior context. */
    fun sendMessage(
        history: List<ChatTurn>,
        message: String,
        completion: (String?, BridgedAiFailure?) -> Unit,
    )

    /**
     * Loads the image at [imageUri] (a `file://` URL) and sends it with
     * `ChatModelConfig.IDENTIFY_POSTER_PROMPT`, with [history] as prior context. An image that can't
     * be read fails with [AiFailureKind.ImageUnreadable].
     */
    fun identifyPoster(
        history: List<ChatTurn>,
        imageUri: String,
        completion: (String?, BridgedAiFailure?) -> Unit,
    )
}

/** A failed AI call: the class Swift put it in, and the SDK's own description. */
data class BridgedAiFailure(
    val kind: AiFailureKind,
    val message: String,
)

/**
 * How Swift classifies a Firebase AI failure. The SDK reports Swift error enums rather than stable
 * codes, so the classification happens in Swift, and Kotlin maps each kind to an `AppError`.
 */
enum class AiFailureKind {
    /** An invalid API key, the API not enabled for the project, or an App Check rejection of the key. */
    Unauthorized,

    /** An `NSURLErrorDomain` error: no connectivity, DNS failure, connection lost. */
    Network,

    /** The request hit `ChatModelConfig.REQUEST_TIMEOUT_MILLIS`. Usually the model stalling, not the device offline. */
    Timeout,

    /** A response the SDK couldn't decode (`GenerateContentError.internalError` wrapping a `DecodingError`). */
    Serialization,

    /** The image at the given URI couldn't be loaded or decoded. */
    ImageUnreadable,

    /** Anything else: a blocked prompt, a response stopped early, quota exceeded, a server error. */
    Other,
}
