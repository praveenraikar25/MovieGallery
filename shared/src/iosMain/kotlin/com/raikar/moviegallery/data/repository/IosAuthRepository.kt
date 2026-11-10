package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.data.remote.auth.mapBridgedAuthError
import com.raikar.moviegallery.domain.model.AuthResult
import com.raikar.moviegallery.domain.model.CurrentUser
import com.raikar.moviegallery.domain.repository.AuthRepository
import com.raikar.moviegallery.platform.BridgedAuthError
import com.raikar.moviegallery.platform.BridgedUser
import com.raikar.moviegallery.platform.FirebaseAuthBridge
import com.raikar.moviegallery.platform.awaitBridge
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * [AuthRepository] on iOS, over the Firebase iOS SDK that Swift supplies as [FirebaseAuthBridge].
 * It behaves as Android's `AuthRepositoryImpl` does, with errors mapped by [mapBridgedAuthError].
 */
class IosAuthRepository(
    private val bridge: FirebaseAuthBridge,
) : AuthRepository {
    // Firebase restores the persisted user from the keychain at configure(), so this is valid at cold start.
    override val isSignedIn: Boolean
        get() = bridge.currentUser() != null

    /*
     * Sends the current user before registering, so the flow doesn't rely on Firebase calling a
     * new listener at once (it does, but that's an SDK detail). distinctUntilChanged drops the
     * repeat when it does.
     */
    private val users: Flow<BridgedUser?> =
        callbackFlow {
            send(bridge.currentUser())
            val registration = bridge.addAuthStateListener { trySend(it) }
            awaitClose { registration.remove() }
        }

    override val currentUserId: Flow<String?> = users.map { it?.uid }.distinctUntilChanged()

    override val currentUser: Flow<CurrentUser?> =
        users
            .map { user -> user?.let { CurrentUser(email = it.email, displayName = it.displayName) } }
            .distinctUntilChanged()

    override suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult<Unit> = authCall { bridge.signIn(email, password, it) }

    override suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult<Unit> = authCall { bridge.signUp(email, password, it) }

    override suspend fun signInWithGoogle(idToken: String): AuthResult<Unit> =
        authCall { bridge.signInWithGoogle(idToken, it) }

    override fun signOut() = bridge.signOut()

    private suspend fun authCall(call: (completion: (BridgedAuthError?) -> Unit) -> Unit): AuthResult<Unit> =
        when (val error = awaitBridge(call)) {
            null -> AuthResult.Success(Unit)
            else -> AuthResult.Failure(mapBridgedAuthError(error))
        }
}
