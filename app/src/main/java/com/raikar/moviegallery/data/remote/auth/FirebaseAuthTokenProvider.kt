package com.raikar.moviegallery.data.remote.auth

import com.google.firebase.auth.FirebaseAuth
import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import kotlinx.coroutines.tasks.await

class FirebaseAuthTokenProvider(
    private val firebaseAuth: FirebaseAuth,
) : AuthTokenProvider {
    override suspend fun idToken(forceRefresh: Boolean): String? =
        firebaseAuth.currentUser
            ?.getIdToken(forceRefresh)
            ?.await()
            ?.token
}
