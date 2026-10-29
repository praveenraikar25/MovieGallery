package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.domain.model.AuthResult
import com.raikar.moviegallery.domain.model.CurrentUser
import com.raikar.moviegallery.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository(
    override var isSignedIn: Boolean = false,
) : AuthRepository {
    /** Set directly to simulate an account signing in, switching or signing out. */
    override val currentUserId = MutableStateFlow<String?>(null)

    /** Set directly to simulate the signed-in account's details changing. */
    override val currentUser = MutableStateFlow<CurrentUser?>(null)

    /** Result returned by the next [signIn], [signUp] or [signInWithGoogle] call. */
    var nextResult: AuthResult<Unit> = AuthResult.Success(Unit)

    /** When set, auth calls suspend until it completes, so tests can observe the in-flight state. */
    var gate: CompletableDeferred<Unit>? = null

    val signInCalls = mutableListOf<Pair<String, String>>()
    val signUpCalls = mutableListOf<Pair<String, String>>()
    val googleCalls = mutableListOf<String>()
    var signOutCalls = 0

    override suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult<Unit> {
        signInCalls += email to password
        return settle()
    }

    override suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult<Unit> {
        signUpCalls += email to password
        return settle()
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult<Unit> {
        googleCalls += idToken
        return settle()
    }

    override fun signOut() {
        signOutCalls++
        isSignedIn = false
        currentUserId.value = null
        currentUser.value = null
    }

    private suspend fun settle(): AuthResult<Unit> {
        gate?.await()
        if (nextResult is AuthResult.Success) isSignedIn = true
        return nextResult
    }
}
