package com.raikar.moviegallery.data.remote

import kotlinx.serialization.json.Json

/** The same configuration NetworkModule installs, so tests exercise the real parser. */
internal val testJson: Json =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

/** Reads a JSON fixture from `app/src/test/resources`. */
internal fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.classLoader.getResourceAsStream(name)) {
        "Missing test fixture: $name"
    }.use { it.readBytes().decodeToString() }
