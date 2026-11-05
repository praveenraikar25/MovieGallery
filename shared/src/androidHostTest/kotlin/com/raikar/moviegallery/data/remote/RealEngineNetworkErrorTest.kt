package com.raikar.moviegallery.data.remote

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A refused connection on OkHttp, the engine Android uses, maps to Network. MockEngine can't show this: what
 * a real engine throws for a socket failure is platform-specific (share-network-layer-ktor design,
 * decision 8). Nothing listens on port 1, so the connection is refused at once, even offline.
 */
class RealEngineNetworkErrorTest {
    @Test
    fun `a refused connection maps to Network`() =
        runTest {
            val api =
                TmdbApi(
                    createTmdbHttpClient(engine = OkHttp.create(), accessToken = "t", baseUrl = "http://127.0.0.1:1/"),
                )

            assertEquals(DataResult.Failure(AppError.Network), safeApiCall { api.popular() })
        }
}
