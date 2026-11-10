package com.raikar.moviegallery.data.remote.auth

import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import com.raikar.moviegallery.platform.FirebaseAuthBridge
import com.raikar.moviegallery.platform.awaitBridge

/** [AuthTokenProvider] on iOS: the signed-in user's Firebase ID token, through [FirebaseAuthBridge]. */
class IosAuthTokenProvider(
    private val bridge: FirebaseAuthBridge,
) : AuthTokenProvider {
    override suspend fun idToken(forceRefresh: Boolean): String? = awaitBridge { bridge.idToken(forceRefresh, it) }
}
