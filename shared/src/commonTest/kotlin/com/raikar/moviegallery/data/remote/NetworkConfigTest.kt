package com.raikar.moviegallery.data.remote

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class NetworkConfigTest {
    @Test
    fun `toString never prints the TMDB token`() {
        val config = NetworkConfig(tmdbAccessToken = "secret-tmdb-token", appVersion = "42")

        val printed = config.toString()

        assertFalse("secret-tmdb-token" in printed, printed)
        assertContains(printed, "appVersion=42")
    }
}
