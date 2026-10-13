package com.raikar.moviegallery.data.remote.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.raikar.moviegallery.domain.model.AuthError
import com.raikar.moviegallery.domain.model.AuthResult
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * The Firebase Auth counterpart to [com.raikar.moviegallery.data.remote.safeApiCall].
 *
 * Separate because auth failures are `FirebaseAuthException` subclasses, not Retrofit
 * `HttpException`s, and map to [AuthError] rather than `AppError`.
 *
 * Catch order matters: [FirebaseAuthWeakPasswordException] extends
 * [FirebaseAuthInvalidCredentialsException], so it has to come first.
 * [CancellationException] must be rethrown so a ViewModel scope tearing down
 * mid-request does not look like a failed sign-in.
 */
internal inline fun <T> safeAuthCall(block: () -> T): AuthResult<T> =
    try {
        AuthResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseAuthWeakPasswordException) {
        AuthResult.Failure(AuthError.WeakPassword)
    } catch (e: FirebaseAuthUserCollisionException) {
        AuthResult.Failure(AuthError.EmailInUse)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        AuthResult.Failure(AuthError.InvalidCredentials)
    } catch (e: FirebaseAuthInvalidUserException) {
        // Unknown or disabled account. Reported the same as a wrong password so the
        // login screen does not reveal which emails have accounts.
        AuthResult.Failure(AuthError.InvalidCredentials)
    } catch (e: FirebaseTooManyRequestsException) {
        AuthResult.Failure(AuthError.TooManyRequests)
    } catch (e: FirebaseNetworkException) {
        AuthResult.Failure(AuthError.Network)
    } catch (e: IOException) {
        AuthResult.Failure(AuthError.Network)
    } catch (e: Throwable) {
        AuthResult.Failure(AuthError.Unknown(e))
    }
