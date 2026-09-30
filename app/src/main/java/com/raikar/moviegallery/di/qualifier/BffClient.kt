package com.raikar.moviegallery.di.qualifier

import javax.inject.Qualifier

/**
 * The BFF demo host's client/Retrofit instance. Distinguished from TMDB's so the
 * two unqualified `OkHttpClient`/`Retrofit` bindings Hilt would otherwise need to
 * disambiguate don't collide, and so no TMDB bearer token is ever attached here.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BffClient
