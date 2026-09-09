package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_NOT_FOUND = 404

/**
 * Runs a network call and turns its failure modes into [AppError]s.
 *
 * Catch order matters: Retrofit's [HttpException] is *not* an [IOException], so it
 * has to be handled explicitly, and [CancellationException] must be rethrown —
 * swallowing it would make normal `WhileSubscribed` teardown look like an error.
 */
internal inline fun <T> safeApiCall(block: () -> T): DataResult<T> =
    try {
        DataResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        DataResult.Failure(
            when (e.code()) {
                HTTP_UNAUTHORIZED -> AppError.Unauthorized
                HTTP_NOT_FOUND -> AppError.NotFound
                else -> AppError.Http(e.code())
            },
        )
    } catch (e: IOException) {
        DataResult.Failure(AppError.Network)
    } catch (e: SerializationException) {
        DataResult.Failure(AppError.Serialization)
    } catch (e: Throwable) {
        DataResult.Failure(AppError.Unknown(e))
    }
