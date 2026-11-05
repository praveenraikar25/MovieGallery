package com.raikar.moviegallery.data.remote.auth

import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

/** Thrown instead of sending a BFF request that has no token to carry. */
class MissingAuthTokenException : Exception("No signed-in user token is available")

private const val TOKEN_LOOKUP_TIMEOUT_MS = 10_000L

/** Configuration for [BffAuth]. */
class BffAuthConfig {
    /** Where the signed-in user's Firebase ID token comes from. Required. */
    var tokenProvider: AuthTokenProvider? = null

    /** Only 401s from this host are retried, so a token is never refreshed for another service. */
    var bffHost: String = ""
}

/**
 * Authenticates BFF requests with the signed-in user's ID token, as the tv-shows spec requires:
 * - every request carries `Authorization: Bearer <token>`, looked up fresh each time (Firebase
 *   caches it), so a Retry after sign-in uses the new account's token;
 * - no request is sent without a token: the call fails with [MissingAuthTokenException];
 * - a 401 from the BFF host is retried once with a force-refreshed token, and a second 401 is
 *   returned to the caller.
 *
 * Ktor's own `Auth` bearer provider isn't used because it caches tokens across requests, keys its
 * refresh on `WWW-Authenticate`, and sends the request unauthenticated when there is no token.
 */
val BffAuth =
    createClientPlugin("BffAuth", ::BffAuthConfig) {
        val tokenProvider = checkNotNull(pluginConfig.tokenProvider) { "BffAuth needs a tokenProvider" }
        val bffHost = pluginConfig.bffHost

        onRequest { request, _ ->
            val token = tokenProvider.lookUpToken(forceRefresh = false) ?: throw MissingAuthTokenException()
            request.headers[HttpHeaders.Authorization] = "Bearer $token"
        }

        on(Send) { request ->
            val call = proceed(request)
            if (call.response.status != HttpStatusCode.Unauthorized || request.url.host != bffHost) return@on call
            val refreshed = tokenProvider.lookUpToken(forceRefresh = true) ?: return@on call
            request.headers[HttpHeaders.Authorization] = "Bearer $refreshed"
            proceed(request)
        }
    }

/** The token, or null when there is none, the lookup fails or it takes longer than 10 s. */
private suspend fun AuthTokenProvider.lookUpToken(forceRefresh: Boolean): String? =
    try {
        withTimeoutOrNull(TOKEN_LOOKUP_TIMEOUT_MS) { idToken(forceRefresh) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
