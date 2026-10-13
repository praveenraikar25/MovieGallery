package com.raikar.moviegallery.domain.model

/**
 * Outcome of an auth call. Mirrors [DataResult] but carries [AuthError], whose cases
 * are specific to sign-in and sign-up rather than HTTP-shaped like [AppError].
 */
sealed interface AuthResult<out T> {
    data class Success<T>(
        val data: T,
    ) : AuthResult<T>

    data class Failure(
        val error: AuthError,
    ) : AuthResult<Nothing>
}

sealed interface AuthError {
    /** Wrong password or unknown account — deliberately not distinguished. */
    data object InvalidCredentials : AuthError

    /** Sign-up with an email that already has an account. */
    data object EmailInUse : AuthError

    /** Sign-up password rejected by the account service. */
    data object WeakPassword : AuthError

    /** No connectivity to the account service. */
    data object Network : AuthError

    /** The account service is rate limiting this device. */
    data object TooManyRequests : AuthError

    data class Unknown(
        val cause: Throwable,
    ) : AuthError
}
