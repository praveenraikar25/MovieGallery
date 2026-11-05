package com.raikar.moviegallery.testutil

import com.raikar.moviegallery.domain.repository.AuthTokenProvider

/** Serves [cached] for normal lookups and [refreshed] for forced refreshes, and records every lookup. */
class FakeAuthTokenProvider(
    var cached: String? = "cached",
    var refreshed: String? = "refreshed",
) : AuthTokenProvider {
    /** The `forceRefresh` argument of each lookup, in call order. */
    val forceRefreshCalls = mutableListOf<Boolean>()

    override suspend fun idToken(forceRefresh: Boolean): String? {
        forceRefreshCalls += forceRefresh
        return if (forceRefresh) refreshed else cached
    }
}
