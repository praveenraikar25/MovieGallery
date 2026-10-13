package com.raikar.moviegallery.domain.repository

import com.raikar.moviegallery.domain.model.AuthResult

interface AuthRepository {
    /** Whether a user session exists. Read synchronously from local storage, so it is safe at startup. */
    val isSignedIn: Boolean

    suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult<Unit>

    suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult<Unit>

    /** Exchanges a Google ID token for a session. The token is obtained by the UI layer. */
    suspend fun signInWithGoogle(idToken: String): AuthResult<Unit>

    fun signOut()
}
