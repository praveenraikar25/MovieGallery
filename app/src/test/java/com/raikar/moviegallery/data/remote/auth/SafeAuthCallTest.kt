package com.raikar.moviegallery.data.remote.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.raikar.moviegallery.domain.model.AuthError
import com.raikar.moviegallery.domain.model.AuthResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class SafeAuthCallTest {
    private fun errorOf(throwable: Throwable): AuthError =
        (safeAuthCall<Unit> { throw throwable } as AuthResult.Failure).error

    @Test
    fun `success is wrapped`() {
        assertEquals(AuthResult.Success(42), safeAuthCall { 42 })
    }

    @Test
    fun `invalid credentials map to InvalidCredentials`() {
        assertEquals(
            AuthError.InvalidCredentials,
            errorOf(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "bad")),
        )
    }

    @Test
    fun `unknown user maps to InvalidCredentials`() {
        assertEquals(
            AuthError.InvalidCredentials,
            errorOf(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "none")),
        )
    }

    @Test
    fun `weak password is not swallowed by the invalid credentials branch`() {
        assertEquals(
            AuthError.WeakPassword,
            errorOf(FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "weak", "too short")),
        )
    }

    @Test
    fun `existing account maps to EmailInUse`() {
        assertEquals(
            AuthError.EmailInUse,
            errorOf(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "taken")),
        )
    }

    @Test
    fun `rate limiting maps to TooManyRequests`() {
        assertEquals(AuthError.TooManyRequests, errorOf(FirebaseTooManyRequestsException("slow down")))
    }

    @Test
    fun `network failures map to Network`() {
        assertEquals(AuthError.Network, errorOf(FirebaseNetworkException("offline")))
        assertEquals(AuthError.Network, errorOf(IOException("offline")))
    }

    @Test
    fun `anything else maps to Unknown with the cause kept`() {
        val cause = IllegalStateException("boom")
        val error = errorOf(cause)
        assertTrue(error is AuthError.Unknown)
        assertEquals(cause, (error as AuthError.Unknown).cause)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is rethrown`() {
        safeAuthCall<Unit> { throw CancellationException("cancelled") }
    }
}
