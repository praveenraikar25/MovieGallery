package com.raikar.moviegallery.data.remote.auth

import com.raikar.moviegallery.data.remote.BffApi
import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Thrown instead of sending a BFF request that has no token to carry. It must be an
 * [IOException]: OkHttp only tolerates that type escaping an interceptor.
 */
class MissingAuthTokenException : IOException("No signed-in user token is available")

/**
 * Both classes below look the token up with [runBlocking]. That is safe here because OkHttp
 * runs interceptors and authenticators on its own dispatcher threads, never the main thread,
 * and Firebase serves a cached token until shortly before it expires.
 */
private const val TOKEN_LOOKUP_TIMEOUT_MS = 10_000L

private fun AuthTokenProvider.blockingToken(forceRefresh: Boolean): String? =
    runBlocking {
        try {
            withTimeoutOrNull(TOKEN_LOOKUP_TIMEOUT_MS) { idToken(forceRefresh) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

private fun Request.Builder.bearer(token: String): Request.Builder = header("Authorization", "Bearer $token")

/** Adds the signed-in user's ID token to every request, or fails the call before it is sent. */
class BffAuthInterceptor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.blockingToken(forceRefresh = false) ?: throw MissingAuthTokenException()
        return chain.proceed(
            chain
                .request()
                .newBuilder()
                .bearer(token)
                .build(),
        )
    }
}

/**
 * On a 401, retries once with a force-refreshed token. Gives up (returning null so the 401 is
 * surfaced) when a 401 was already retried, the request left the BFF host, or no token can be obtained.
 */
class BffTokenAuthenticator(
    private val tokenProvider: AuthTokenProvider,
    private val bffHost: String = BffApi.BASE_URL.toHttpUrl().host,
) : Authenticator {
    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        if (generateSequence(response.priorResponse) { it.priorResponse }.any { it.code == 401 }) return null
        if (response.request.url.host != bffHost) return null
        val token = tokenProvider.blockingToken(forceRefresh = true) ?: return null
        return response.request
            .newBuilder()
            .bearer(token)
            .build()
    }
}
