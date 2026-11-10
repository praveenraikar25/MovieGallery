package com.raikar.moviegallery.data.remote.auth

import com.raikar.moviegallery.domain.model.AuthError
import com.raikar.moviegallery.platform.BridgedAuthError

/**
 * The iOS counterpart to `:app`'s `safeAuthCall`: maps a Firebase iOS Auth error to the same
 * [AuthError] Android produces for the equivalent failure.
 *
 * An unknown or disabled account is reported the same as a wrong password, so the login screen
 * does not reveal which emails have accounts. A malformed email also reads as invalid
 * credentials, as Android's `FirebaseAuthInvalidCredentialsException` does.
 */
internal fun mapBridgedAuthError(error: BridgedAuthError): AuthError =
    when {
        error.domain == NS_URL_ERROR_DOMAIN -> AuthError.Network
        error.domain != AUTH_ERROR_DOMAIN -> AuthError.Unknown(BridgedAuthException(error))
        else ->
            when (error.code) {
                WEAK_PASSWORD -> AuthError.WeakPassword
                EMAIL_ALREADY_IN_USE -> AuthError.EmailInUse
                WRONG_PASSWORD, INVALID_CREDENTIAL, INVALID_EMAIL, USER_NOT_FOUND, USER_DISABLED ->
                    AuthError.InvalidCredentials
                TOO_MANY_REQUESTS -> AuthError.TooManyRequests
                NETWORK_ERROR -> AuthError.Network
                else -> AuthError.Unknown(BridgedAuthException(error))
            }
    }

/** The cause of an [AuthError.Unknown] from iOS, keeping what the SDK reported. */
class BridgedAuthException(
    val domain: String,
    val code: Long,
    message: String,
) : Exception("$domain $code: $message") {
    internal constructor(error: BridgedAuthError) : this(error.domain, error.code, error.message)
}

/*
 * Firebase iOS's `AuthErrorCode` values, checked against firebase-ios-sdk 13.1.1,
 * FirebaseAuth/Sources/Swift/Utilities/AuthErrors.swift.
 */
internal const val AUTH_ERROR_DOMAIN = "FIRAuthErrorDomain" // AuthErrors.domain
internal const val NS_URL_ERROR_DOMAIN = "NSURLErrorDomain"
internal const val INVALID_CREDENTIAL = 17004L // .invalidCredential
internal const val USER_DISABLED = 17005L // .userDisabled
internal const val EMAIL_ALREADY_IN_USE = 17007L // .emailAlreadyInUse
internal const val INVALID_EMAIL = 17008L // .invalidEmail
internal const val WRONG_PASSWORD = 17009L // .wrongPassword
internal const val TOO_MANY_REQUESTS = 17010L // .tooManyRequests
internal const val USER_NOT_FOUND = 17011L // .userNotFound
internal const val NETWORK_ERROR = 17020L // .networkError
internal const val WEAK_PASSWORD = 17026L // .weakPassword
