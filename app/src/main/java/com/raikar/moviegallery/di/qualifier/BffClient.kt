package com.raikar.moviegallery.di.qualifier

import javax.inject.Qualifier

/**
 * The BFF demo host's Ktor `HttpClient`, which adds the user's ID token. Distinguished from TMDB's
 * so the two `HttpClient` bindings don't collide, and so no TMDB bearer token is ever attached here.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BffClient
