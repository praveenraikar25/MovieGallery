package com.raikar.moviegallery.data.remote.ai

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.platform.AiFailureKind
import com.raikar.moviegallery.platform.BridgedAiFailure

/**
 * The iOS counterpart to `:app`'s `safeAiCall`: maps a failure Swift classified to the same
 * [AppError] Android produces for the equivalent Firebase AI failure.
 */
internal fun mapBridgedAiFailure(failure: BridgedAiFailure): AppError =
    when (failure.kind) {
        AiFailureKind.Unauthorized -> AppError.Unauthorized
        AiFailureKind.Network -> AppError.Network
        // Not AppError.Network: the usual cause is the model stalling, not the device
        // being offline, and telling the user to check their internet sends them after
        // the wrong thing. Unknown's copy just offers a retry, which is the right advice.
        AiFailureKind.Timeout -> AppError.Unknown(BridgedAiException(failure))
        AiFailureKind.Serialization -> AppError.Serialization
        // Not Network either: an unreadable image isn't fixed by checking the connection.
        AiFailureKind.ImageUnreadable -> AppError.Unknown(BridgedAiException(failure))
        // The cause is kept so the chat UI can show the model's own explanation.
        AiFailureKind.Other -> AppError.Unknown(BridgedAiException(failure))
    }

/** The cause of an [AppError.Unknown] from iOS, carrying the SDK's description as its message. */
class BridgedAiException(
    val kind: AiFailureKind,
    message: String,
) : Exception(message) {
    internal constructor(failure: BridgedAiFailure) : this(failure.kind, failure.message)
}
