package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.auth.MissingAuthTokenException
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_NOT_FOUND = 404

/**
 * Runs a network call and turns its failure modes into [AppError]s.
 *
 * Catch order matters:
 * - [CancellationException] is rethrown first: swallowing it would make normal `WhileSubscribed`
 *   teardown look like an error.
 * - [ResponseException] (a non-2xx status, thrown because the clients set `expectSuccess`) is not an
 *   [IOException], so it is handled on its own.
 * - Ktor's timeout exceptions are named explicitly, so they map to [AppError.Network] whether or not a
 *   platform makes them [IOException]s.
 * - Bodies are decoded with kotlinx.serialization directly (see `decode`), so a bad body is a
 *   [SerializationException].
 */
internal inline fun <T> safeApiCall(block: () -> T): DataResult<T> =
    try {
        DataResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: MissingAuthTokenException) {
        DataResult.Failure(AppError.AuthRequired)
    } catch (e: ResponseException) {
        val code = e.response.status.value
        DataResult.Failure(
            when (code) {
                HTTP_UNAUTHORIZED -> AppError.Unauthorized
                HTTP_NOT_FOUND -> AppError.NotFound
                else -> AppError.Http(code)
            },
        )
    } catch (e: HttpRequestTimeoutException) {
        DataResult.Failure(AppError.Network)
    } catch (e: ConnectTimeoutException) {
        DataResult.Failure(AppError.Network)
    } catch (e: SocketTimeoutException) {
        DataResult.Failure(AppError.Network)
    } catch (e: IOException) {
        DataResult.Failure(AppError.Network)
    } catch (e: SerializationException) {
        DataResult.Failure(AppError.Serialization)
    } catch (e: Throwable) {
        DataResult.Failure(AppError.Unknown(e))
    }
