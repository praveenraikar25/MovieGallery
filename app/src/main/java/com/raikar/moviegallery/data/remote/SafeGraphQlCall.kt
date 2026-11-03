package com.raikar.moviegallery.data.remote

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.ApolloGraphQLException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloOfflineException
import com.apollographql.apollo.exception.ApolloParseException
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import kotlin.coroutines.cancellation.CancellationException

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_NOT_FOUND = 404

/**
 * The GraphQL counterpart of [safeApiCall]. Apollo reports transport failures on the response
 * ([ApolloResponse.exception]) rather than throwing, and GraphQL errors in [ApolloResponse.errors],
 * so both are mapped here. A response with data is a success even when it carries partial errors.
 */
internal inline fun <D : Operation.Data> safeGraphQlCall(block: () -> ApolloResponse<D>): DataResult<D> {
    val response =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return DataResult.Failure(AppError.Unknown(e))
        }
    val data = response.data
    val exception = response.exception
    return when {
        data != null -> DataResult.Success(data)
        exception != null -> DataResult.Failure(exception.toAppError())
        !response.errors.isNullOrEmpty() ->
            DataResult.Failure(AppError.Unknown(ApolloGraphQLException(response.errors.orEmpty())))
        else -> DataResult.Failure(AppError.Serialization)
    }
}

internal fun Throwable.toAppError(): AppError =
    when (this) {
        is ApolloNetworkException, is ApolloOfflineException -> AppError.Network
        is ApolloHttpException ->
            when (statusCode) {
                HTTP_UNAUTHORIZED -> AppError.Unauthorized
                HTTP_NOT_FOUND -> AppError.NotFound
                else -> AppError.Http(statusCode)
            }
        is ApolloParseException -> AppError.Serialization
        else -> AppError.Unknown(this)
    }
