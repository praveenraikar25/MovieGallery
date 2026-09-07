package com.raikar.moviegallery.domain.model

/**
 * Outcome of a data-layer call. Repositories return this instead of throwing so
 * that failures are part of the contract the UI has to handle.
 */
sealed interface DataResult<out T> {
    data class Success<T>(
        val data: T,
    ) : DataResult<T>

    data class Failure(
        val error: AppError,
    ) : DataResult<Nothing>
}

sealed interface AppError {
    /** No connectivity, DNS failure, timeout — anything IOException-shaped. */
    data object Network : AppError

    /** HTTP 401. Almost always a missing or malformed TMDB token. */
    data object Unauthorized : AppError

    /** HTTP 404 — TMDB does not know this movie id. */
    data object NotFound : AppError

    data class Http(
        val code: Int,
    ) : AppError

    /** The response parsed as valid JSON but not into the shape we expect. */
    data object Serialization : AppError

    data class Unknown(
        val cause: Throwable,
    ) : AppError
}
