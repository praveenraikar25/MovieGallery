package com.raikar.moviegallery.di

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.http.LoggingInterceptor
import com.raikar.moviegallery.data.remote.COUNTRIES_URL
import com.raikar.moviegallery.data.remote.NetworkConfig
import com.raikar.moviegallery.data.remote.auth.IosAuthTokenProvider
import com.raikar.moviegallery.data.repository.IosAiChatRepository
import com.raikar.moviegallery.data.repository.IosAuthRepository
import com.raikar.moviegallery.domain.repository.AiChatRepository
import com.raikar.moviegallery.domain.repository.AuthRepository
import com.raikar.moviegallery.domain.repository.AuthTokenProvider
import com.raikar.moviegallery.platform.FirebaseAiBridge
import com.raikar.moviegallery.platform.FirebaseAuthBridge
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The per-build values the iOS app supplies at launch (5b fills them from its build settings).
 *
 * @property tmdbAccessToken TMDB v4 read access token, sent as a bearer token to TMDB only.
 * @property appVersion sent to the BFF in `X-APP-VERSION`.
 * @property isDebug logs HTTP traffic to the Xcode console when true; Swift passes its `DEBUG` flag.
 */
data class IosAppConfig(
    val tmdbAccessToken: String,
    val appVersion: String,
    val isDebug: Boolean,
) {
    /** Redacts the token, so a logged or crash-reported config never prints it. */
    override fun toString(): String =
        "IosAppConfig(tmdbAccessToken=<redacted>, appVersion=$appVersion, isDebug=$isDebug)"
}

/**
 * What the shared modules need from iOS (see `sharedModules`), the counterpart to `:app`'s
 * `androidPlatformModule` (add-ios-platform-layer design, decision 6). Firebase comes from Swift
 * through the two bridges.
 */
internal fun iosPlatformModule(
    config: IosAppConfig,
    authBridge: FirebaseAuthBridge,
    aiBridge: FirebaseAiBridge,
): Module =
    module {
        /*
         * A factory, not a single: each Ktor client gets its own engine, so closing one never
         * breaks the other. NSURLSession's default timeouts apply; Android's 15s OkHttp timeouts
         * aren't copied until real iOS traffic shows what fits.
         */
        factory<HttpClientEngine> { Darwin.create() }

        /*
         * Headers and bodies to stdout (the Xcode console) in debug builds, nothing in release.
         * Ktor redacts `Authorization` at every level.
         */
        single {
            NetworkConfig(
                tmdbAccessToken = config.tmdbAccessToken,
                appVersion = config.appVersion,
                logLevel = if (config.isDebug) LogLevel.ALL else LogLevel.NONE,
                logger = Logger.SIMPLE,
            )
        }

        // On Apollo's default iOS transport (NSURLSession), logging bodies in debug as Android does.
        single {
            ApolloClient
                .Builder()
                .serverUrl(COUNTRIES_URL)
                .apply { if (config.isDebug) addHttpInterceptor(LoggingInterceptor(LoggingInterceptor.Level.BODY)) }
                .build()
        }

        single { authBridge }
        single { aiBridge }
        single<AuthRepository> { IosAuthRepository(get()) }
        single<AuthTokenProvider> { IosAuthTokenProvider(get()) }
        single<AiChatRepository> { IosAiChatRepository(get(), get(IoDispatcher)) }
    }
