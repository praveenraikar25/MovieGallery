package com.raikar.moviegallery.data.repository

import app.cash.turbine.test
import com.raikar.moviegallery.data.remote.auth.BridgedAuthException
import com.raikar.moviegallery.data.remote.auth.IosAuthTokenProvider
import com.raikar.moviegallery.domain.model.AuthError
import com.raikar.moviegallery.domain.model.AuthResult
import com.raikar.moviegallery.domain.model.CurrentUser
import com.raikar.moviegallery.platform.BridgedAuthError
import com.raikar.moviegallery.platform.BridgedUser
import com.raikar.moviegallery.testutil.FakeFirebaseAuthBridge
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * iOS auth must fail the way Android does: each Firebase iOS error maps to the `AuthError`
 * Android's `safeAuthCall` gives the equivalent exception (add-ios-platform-layer design, decision 3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IosAuthRepositoryTest {
    private val bridge = FakeFirebaseAuthBridge()
    private val repository = IosAuthRepository(bridge)

    @Test
    fun `isSignedIn follows the bridge's current user`() {
        assertFalse(repository.isSignedIn)

        bridge.user = USER_A

        assertTrue(repository.isSignedIn)
    }

    @Test
    fun `currentUserId emits the current user then each change without repeats`() =
        runTest {
            bridge.user = USER_A

            repository.currentUserId.test {
                assertEquals("uid-a", awaitItem())
                bridge.emit(USER_A)
                bridge.emit(USER_B)
                assertEquals("uid-b", awaitItem())
                bridge.emit(null)
                assertEquals(null, awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `cancelling collection removes the listener`() =
        runTest {
            val job = repository.currentUserId.launchIn(this)
            runCurrent()
            assertEquals(1, bridge.activeListeners)

            job.cancel()
            runCurrent()

            assertEquals(0, bridge.activeListeners)
        }

    @Test
    fun `currentUser maps the email and display name`() =
        runTest {
            repository.currentUser.test {
                assertEquals(null, awaitItem())
                bridge.emit(USER_A)
                assertEquals(CurrentUser(email = "a@example.com", displayName = "Ada"), awaitItem())
            }
        }

    @Test
    fun `signIn forwards the credentials and succeeds`() =
        runTest {
            assertEquals(AuthResult.Success(Unit), repository.signIn("a@example.com", "secret1"))
            assertEquals(listOf("signIn a@example.com secret1"), bridge.calls)
        }

    @Test
    fun `signUp and Google sign-in forward and map their errors`() =
        runTest {
            bridge.nextError = authError(17007)

            assertEquals(AuthResult.Failure(AuthError.EmailInUse), repository.signUp("a@example.com", "secret1"))
            assertEquals(AuthResult.Failure(AuthError.EmailInUse), repository.signInWithGoogle("google-token"))
            assertEquals(listOf("signUp a@example.com secret1", "signInWithGoogle google-token"), bridge.calls)
        }

    @Test
    fun `weakPassword maps to WeakPassword`() = assertSignInFails(authError(17026), AuthError.WeakPassword)

    @Test
    fun `emailAlreadyInUse maps to EmailInUse`() = assertSignInFails(authError(17007), AuthError.EmailInUse)

    @Test
    fun `wrong password and unknown or disabled accounts all map to InvalidCredentials`() =
        runTest {
            // wrongPassword, invalidCredential, invalidEmail, userNotFound, userDisabled
            listOf(17009L, 17004L, 17008L, 17011L, 17005L).forEach { code ->
                bridge.nextError = authError(code)
                assertEquals(
                    AuthResult.Failure(AuthError.InvalidCredentials),
                    repository.signIn("a@b.co", "x"),
                    "code $code",
                )
            }
        }

    @Test
    fun `tooManyRequests maps to TooManyRequests`() = assertSignInFails(authError(17010), AuthError.TooManyRequests)

    @Test
    fun `networkError maps to Network`() = assertSignInFails(authError(17020), AuthError.Network)

    @Test
    fun `any NSURLErrorDomain error maps to Network`() =
        assertSignInFails(
            BridgedAuthError("NSURLErrorDomain", -1009, "The Internet connection appears to be offline"),
            AuthError.Network,
        )

    @Test
    fun `an unknown auth code is Unknown and keeps what the SDK reported`() =
        runTest {
            bridge.nextError = BridgedAuthError("FIRAuthErrorDomain", 17999, "Something new")

            val result = assertIs<AuthResult.Failure>(repository.signIn("a@b.co", "x"))

            val cause = assertIs<BridgedAuthException>(assertIs<AuthError.Unknown>(result.error).cause)
            assertEquals("FIRAuthErrorDomain", cause.domain)
            assertEquals(17999, cause.code)
            assertEquals("FIRAuthErrorDomain 17999: Something new", cause.message)
        }

    @Test
    fun `an error from another domain is Unknown even with a known code`() =
        runTest {
            bridge.nextError = BridgedAuthError("SomeOtherDomain", 17009, "Not Firebase")

            val result = assertIs<AuthResult.Failure>(repository.signIn("a@b.co", "x"))

            assertIs<AuthError.Unknown>(result.error)
        }

    @Test
    fun `a completion fired twice is harmless`() =
        runTest {
            bridge.completeTwice = true

            assertEquals(AuthResult.Success(Unit), repository.signIn("a@b.co", "x"))
        }

    @Test
    fun `a late completion after the caller is cancelled is dropped`() =
        runTest {
            bridge.holdCompletions = true
            val call = async(start = CoroutineStart.UNDISPATCHED) { repository.signIn("a@b.co", "x") }

            call.cancel()
            bridge.releaseHeld()
            runCurrent()

            assertTrue(call.isCancelled)
        }

    @Test
    fun `signOut forwards to the bridge`() {
        repository.signOut()

        assertEquals(listOf("signOut"), bridge.calls)
    }

    @Test
    fun `the token provider passes forceRefresh through and returns the token`() =
        runTest {
            val provider = IosAuthTokenProvider(bridge)

            assertEquals("id-token", provider.idToken(forceRefresh = false))
            bridge.token = null
            assertEquals(null, provider.idToken(forceRefresh = true))
            assertEquals(listOf(false, true), bridge.tokenRequests)
        }

    private fun assertSignInFails(
        error: BridgedAuthError,
        expected: AuthError,
    ) = runTest {
        bridge.nextError = error

        assertEquals(AuthResult.Failure(expected), repository.signIn("a@b.co", "x"))
    }

    private companion object {
        val USER_A = BridgedUser(uid = "uid-a", email = "a@example.com", displayName = "Ada")
        val USER_B = BridgedUser(uid = "uid-b", email = "b@example.com", displayName = null)

        fun authError(code: Long) = BridgedAuthError("FIRAuthErrorDomain", code, "Firebase says no")
    }
}
