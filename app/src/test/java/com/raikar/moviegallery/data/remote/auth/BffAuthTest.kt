package com.raikar.moviegallery.data.remote.auth

import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

class BffAuthTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private val server get() = serverRule.server

    private class FakeTokenProvider(
        var cached: String? = "cached",
        var refreshed: String? = "refreshed",
    ) : AuthTokenProvider {
        val forceRefreshCalls = mutableListOf<Boolean>()

        override suspend fun idToken(forceRefresh: Boolean): String? {
            forceRefreshCalls += forceRefresh
            return if (forceRefresh) refreshed else cached
        }
    }

    private fun client(provider: AuthTokenProvider) =
        OkHttpClient
            .Builder()
            .addInterceptor(BffAuthInterceptor(provider))
            .authenticator(BffTokenAuthenticator(provider, server.hostName))
            .build()

    private fun call(provider: AuthTokenProvider) =
        client(provider).newCall(Request.Builder().url(server.url("/")).build())

    private fun enqueue(code: Int) = server.enqueue(MockResponse.Builder().code(code).build())

    @Test
    fun `sends the token as a bearer Authorization header`() =
        runTest {
            enqueue(200)

            call(FakeTokenProvider()).execute().close()

            assertEquals("Bearer cached", server.takeRequest().headers["Authorization"])
        }

    @Test
    fun `sends no request when there is no token`() =
        runTest {
            assertThrows(MissingAuthTokenException::class.java) {
                call(FakeTokenProvider(cached = null)).execute()
            }

            assertEquals(0, server.requestCount)
        }

    @Test
    fun `a 401 is retried once with a refreshed token`() =
        runTest {
            enqueue(401)
            enqueue(200)
            val provider = FakeTokenProvider()

            val response = call(provider).execute()

            assertEquals(200, response.code)
            response.close()
            assertEquals("Bearer cached", server.takeRequest().headers["Authorization"])
            assertEquals("Bearer refreshed", server.takeRequest().headers["Authorization"])
            assertEquals(listOf(false, true), provider.forceRefreshCalls)
        }

    @Test
    fun `a persistent 401 is surfaced after a single retry`() =
        runTest {
            enqueue(401)
            enqueue(401)
            enqueue(200)

            val response = call(FakeTokenProvider()).execute()

            assertEquals(401, response.code)
            response.close()
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `a 401 is surfaced without a retry when no refreshed token exists`() =
        runTest {
            enqueue(401)

            val response = call(FakeTokenProvider(refreshed = null)).execute()

            assertEquals(401, response.code)
            response.close()
            assertEquals(1, server.requestCount)
            assertNull(response.priorResponse)
        }
}
