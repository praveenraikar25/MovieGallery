package com.raikar.moviegallery.di

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named

/*
 * Koin qualifiers for the bindings that share a type. Always refer to these constants, never to
 * the raw names, so a typo fails to compile.
 */

/**
 * The TMDB Ktor `HttpClient`, which adds the TMDB bearer token. Its own qualifier keeps it apart from
 * the BFF client, and the token never reaches the BFF client or the unqualified base `OkHttpClient`
 * that images use.
 */
val TmdbClient: Qualifier = named("TmdbClient")

/**
 * The BFF demo host's Ktor `HttpClient`, which adds the user's ID token. Distinguished from TMDB's
 * so the two `HttpClient` bindings don't collide, and so no TMDB bearer token is ever attached here.
 */
val BffClient: Qualifier = named("BffClient")

/** The dispatcher repositories move blocking and network work onto. */
val IoDispatcher: Qualifier = named("IoDispatcher")
