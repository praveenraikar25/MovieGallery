package com.raikar.moviegallery.di.qualifier

import javax.inject.Qualifier

/**
 * The authenticated OkHttp client. Distinguished from the unqualified base client
 * so that the TMDB bearer token can never be attached to image requests.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmdbClient
