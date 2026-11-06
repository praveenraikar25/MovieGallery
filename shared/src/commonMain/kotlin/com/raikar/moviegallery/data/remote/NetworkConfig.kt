package com.raikar.moviegallery.data.remote

import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger

/**
 * The per-build values the two Ktor clients need, supplied by each platform: Android builds it
 * from `BuildConfig`. Grouped in one binding so the graph never carries a bare `String` token
 * that something else could resolve by mistake.
 *
 * @property tmdbAccessToken TMDB v4 read access token, sent as a bearer token to TMDB only.
 * @property appVersion sent to the BFF in `X-APP-VERSION`.
 */
data class NetworkConfig(
    val tmdbAccessToken: String,
    val appVersion: String,
    val logLevel: LogLevel = LogLevel.NONE,
    val logger: Logger = Logger.EMPTY,
) {
    /** Redacts the token, so a logged or crash-reported config never prints it. */
    override fun toString(): String =
        "NetworkConfig(tmdbAccessToken=<redacted>, appVersion=$appVersion, logLevel=$logLevel)"
}
