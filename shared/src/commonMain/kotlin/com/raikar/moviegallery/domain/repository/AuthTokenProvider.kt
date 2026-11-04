package com.raikar.moviegallery.domain.repository

/** Supplies the signed-in user's ID token for backends that verify it. */
interface AuthTokenProvider {
    /**
     * The current user's ID token, or null when nobody is signed in or no token can be obtained.
     * [forceRefresh] skips the cached token, for recovering from a rejected one.
     */
    suspend fun idToken(forceRefresh: Boolean = false): String?
}
