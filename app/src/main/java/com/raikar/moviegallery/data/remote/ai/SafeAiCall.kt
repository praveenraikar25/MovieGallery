package com.raikar.moviegallery.data.remote.ai

import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServiceDisabledException
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import com.google.firebase.ai.type.SerializationException as FirebaseAiSerializationException

/**
 * The Firebase AI counterpart to [com.raikar.moviegallery.data.remote.safeApiCall].
 *
 * A separate function rather than a shared one because `safeApiCall` keys off
 * Retrofit's `HttpException`, which Gemini calls never throw — they throw
 * `FirebaseAIException` subclasses, and those carry no HTTP status code, so
 * [AppError.Http] is not reachable from here.
 *
 * [CancellationException] must be rethrown so that a ViewModel scope tearing down
 * mid-request does not look like a failed reply.
 */
internal inline fun <T> safeAiCall(block: () -> T): DataResult<T> =
    try {
        DataResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: InvalidAPIKeyException) {
        DataResult.Failure(AppError.Unauthorized)
    } catch (e: APINotConfiguredException) {
        DataResult.Failure(AppError.Unauthorized)
    } catch (e: ServiceDisabledException) {
        DataResult.Failure(AppError.Unauthorized)
    } catch (e: RequestTimeoutException) {
        DataResult.Failure(AppError.Network)
    } catch (e: FirebaseAiSerializationException) {
        DataResult.Failure(AppError.Serialization)
    } catch (e: IOException) {
        DataResult.Failure(AppError.Network)
    } catch (e: Throwable) {
        // Covers ServerException (App Check rejection lands here), PromptBlockedException,
        // ResponseStoppedException and QuotaExceededException. The cause is kept so the
        // chat UI can show the model's own explanation instead of a generic message.
        DataResult.Failure(AppError.Unknown(e))
    }
