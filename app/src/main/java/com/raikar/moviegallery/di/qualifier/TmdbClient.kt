package com.raikar.moviegallery.di.qualifier

import javax.inject.Qualifier

/**
 * The TMDB Ktor `HttpClient`, which adds the TMDB bearer token. Its own qualifier keeps it apart from
 * the BFF client, and the token never reaches the unqualified base `OkHttpClient` that images use.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmdbClient
