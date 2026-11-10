package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.domain.model.ChatTurn
import com.raikar.moviegallery.platform.BridgedAiFailure
import com.raikar.moviegallery.platform.FirebaseAiBridge

/**
 * A [FirebaseAiBridge] standing in for the Swift one. Every call completes at once with [reply]
 * and [failure]; with [completeTwice], the completion fires twice.
 */
class FakeFirebaseAiBridge : FirebaseAiBridge {
    var reply: String? = "A reply"
    var failure: BridgedAiFailure? = null
    var completeTwice = false

    var lastHistory: List<ChatTurn>? = null
    var lastMessage: String? = null
    var lastImageUri: String? = null

    override fun sendMessage(
        history: List<ChatTurn>,
        message: String,
        completion: (String?, BridgedAiFailure?) -> Unit,
    ) {
        lastHistory = history
        lastMessage = message
        finish(completion)
    }

    override fun identifyPoster(
        history: List<ChatTurn>,
        imageUri: String,
        completion: (String?, BridgedAiFailure?) -> Unit,
    ) {
        lastHistory = history
        lastImageUri = imageUri
        finish(completion)
    }

    private fun finish(completion: (String?, BridgedAiFailure?) -> Unit) {
        completion(reply, failure)
        if (completeTwice) completion("a second reply", null)
    }
}
