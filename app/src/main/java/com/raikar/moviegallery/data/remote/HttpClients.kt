package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.data.remote.auth.BffAuth
import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import kotlinx.serialization.json.Json

private const val TIMEOUT_MILLIS = 15_000L

/** The JSON configuration both backends are parsed with. */
val networkJson: Json =
    Json {
        // TMDB adds fields over time and returns endpoint-specific extras such as
        // the `dates` object on now_playing/upcoming.
        ignoreUnknownKeys = true
        // Turns an explicit JSON null into the property's declared default.
        coerceInputValues = true
    }

/**
 * The TMDB client: every request carries the v4 read access token as a bearer header.
 * [baseUrl] keeps its trailing slash and endpoint paths have none, or `/3` would be dropped.
 */
fun createTmdbHttpClient(
    engine: HttpClientEngine,
    accessToken: String,
    logLevel: LogLevel = LogLevel.NONE,
    logger: Logger = Logger.EMPTY,
    baseUrl: String = TmdbApi.BASE_URL,
): HttpClient =
    HttpClient(engine) {
        installCommon(logLevel, logger)
        defaultRequest {
            url(baseUrl)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.Accept, "application/json")
        }
    }

/** The BFF client: every request carries the app version and the signed-in user's ID token. */
fun createBffHttpClient(
    engine: HttpClientEngine,
    tokenProvider: AuthTokenProvider,
    appVersion: String,
    logLevel: LogLevel = LogLevel.NONE,
    logger: Logger = Logger.EMPTY,
    baseUrl: String = BffApi.BASE_URL,
    bffHost: String = Url(baseUrl).host,
): HttpClient =
    HttpClient(engine) {
        installCommon(logLevel, logger)
        install(BffAuth) {
            this.tokenProvider = tokenProvider
            this.bffHost = bffHost
        }
        defaultRequest {
            url(baseUrl)
            header("X-APP-VERSION", appVersion)
        }
    }

private fun HttpClientConfig<*>.installCommon(
    logLevel: LogLevel,
    logger: Logger,
) {
    // Non-2xx responses throw ResponseException, which safeApiCall maps to an AppError, instead of
    // being handed to the JSON decoder.
    expectSuccess = true
    install(HttpTimeout) {
        connectTimeoutMillis = TIMEOUT_MILLIS
        socketTimeoutMillis = TIMEOUT_MILLIS
    }
    install(Logging) {
        this.logger = logger
        level = logLevel
        // Never log a token, even at BODY level.
        sanitizeHeader { it == HttpHeaders.Authorization }
    }
}

/**
 * Decodes a response body with [networkJson] whatever its `Content-Type`, as Retrofit's converter did.
 * Ktor's content negotiation skips a response with no `Content-Type`, and it would add an
 * `Accept` header the BFF never received before; a non-JSON body (a captive portal page) fails
 * here with a `SerializationException`.
 */
internal suspend inline fun <reified T> HttpResponse.decode(): T = networkJson.decodeFromString(bodyAsText())
