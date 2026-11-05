package com.raikar.moviegallery.testutil

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.io.IOException

/**
 * [FakeHttpServer] over Ktor's [MockEngine], so the same tests run on the JVM and on iOS without a
 * socket. A planned connection failure throws the [IOException] a real engine reports for a
 * dropped connection; the real engines' own failures are covered by `RealEngineNetworkErrorTest`.
 */
internal class MockEngineHttpServer : FakeHttpServer {
    private val plan = ResponsePlan()

    /** Shared by every client a test builds; never closed, since a closed engine rejects requests. */
    val engine: MockEngine =
        MockEngine { request ->
            val recorded =
                RecordedHttpRequest(
                    path = request.url.encodedPath,
                    query =
                        request.url.parameters
                            .entries()
                            .associate { it.key to it.value },
                    headers = request.headers.entries().associate { it.key to it.value },
                )
            when (val planned = plan.next(recorded)) {
                is PlannedResponse.Respond -> respond(planned.body, HttpStatusCode.fromValue(planned.status))
                PlannedResponse.FailConnection -> throw IOException("Connection dropped by FakeHttpServer")
            }
        }

    override val host: String = "fake-server.test"

    override val origin: String = "http://$host"

    override val requestCount: Int get() = plan.requestCount

    override fun enqueue(
        status: Int,
        body: String,
        path: String?,
    ) = plan.enqueue(path, PlannedResponse.Respond(status, body))

    override fun failConnections(path: String?) = plan.failConnections(path)

    override fun takeRequest(): RecordedHttpRequest = plan.take()

    override fun close() = Unit
}
