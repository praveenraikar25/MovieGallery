package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.platform.BridgeRegistration
import com.raikar.moviegallery.platform.BridgedAuthError
import com.raikar.moviegallery.platform.BridgedUser
import com.raikar.moviegallery.platform.FirebaseAuthBridge

/**
 * A [FirebaseAuthBridge] standing in for the Swift one. Calls complete at once with [nextError]
 * unless [holdCompletions] is set, in which case [releaseHeld] completes them later. With
 * [completeTwice], every completion fires twice, as a buggy Swift bridge might.
 */
class FakeFirebaseAuthBridge(
    var user: BridgedUser? = null,
) : FirebaseAuthBridge {
    val calls = mutableListOf<String>()
    val tokenRequests = mutableListOf<Boolean>()
    var nextError: BridgedAuthError? = null
    var token: String? = "id-token"
    var completeTwice = false
    var holdCompletions = false

    private val listeners = mutableListOf<(BridgedUser?) -> Unit>()
    private val held = mutableListOf<() -> Unit>()

    /** Listeners registered and not yet removed. */
    val activeListeners: Int get() = listeners.size

    /** Changes the signed-in user and tells every listener, as Firebase does. */
    fun emit(user: BridgedUser?) {
        this.user = user
        listeners.toList().forEach { it(user) }
    }

    fun releaseHeld() {
        held.toList().forEach { it() }
        held.clear()
    }

    override fun currentUser(): BridgedUser? = user

    override fun addAuthStateListener(onChange: (BridgedUser?) -> Unit): BridgeRegistration {
        listeners += onChange
        return object : BridgeRegistration {
            override fun remove() {
                listeners -= onChange
            }
        }
    }

    override fun signIn(
        email: String,
        password: String,
        completion: (BridgedAuthError?) -> Unit,
    ) {
        calls += "signIn $email $password"
        finish { completion(nextError) }
    }

    override fun signUp(
        email: String,
        password: String,
        completion: (BridgedAuthError?) -> Unit,
    ) {
        calls += "signUp $email $password"
        finish { completion(nextError) }
    }

    override fun signInWithGoogle(
        idToken: String,
        completion: (BridgedAuthError?) -> Unit,
    ) {
        calls += "signInWithGoogle $idToken"
        finish { completion(nextError) }
    }

    override fun signOut() {
        calls += "signOut"
    }

    override fun idToken(
        forceRefresh: Boolean,
        completion: (String?) -> Unit,
    ) {
        tokenRequests += forceRefresh
        finish { completion(token) }
    }

    private fun finish(complete: () -> Unit) {
        val run = {
            complete()
            if (completeTwice) complete()
        }
        if (holdCompletions) held += run else run()
    }
}
