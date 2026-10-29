package com.raikar.moviegallery.domain.repository

import com.raikar.moviegallery.domain.model.AuthResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Whether a user session exists. Read synchronously from local storage, so it is safe at startup. */
    val isSignedIn: Boolean

    /**
     * The signed-in account's stable id, or null when signed out. Emits the current value on
     * collection and again on every sign-in or sign-out, so per-account data can follow it.
     */
    val currentUserId: Flow<String?>

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
