package com.raikar.moviegallery.data.remote

/**
 * Reads a JSON fixture from `app/src/test/resources`. Only the Countries GraphQL fixtures live
 * there now; the TMDB and BFF ones moved to `:shared` commonTest as `TestFixtures.kt`.
 */
internal fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.classLoader.getResourceAsStream(name)) {
        "Missing test fixture: $name"
    }.use { it.readBytes().decodeToString() }
