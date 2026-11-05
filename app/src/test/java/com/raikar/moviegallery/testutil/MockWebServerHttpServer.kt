package com.raikar.moviegallery.testutil

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect

/**
 * [FakeHttpServer] over a real local HTTP server, so requests go through a real HTTP stack and
 * socket. A planned connection failure shuts the connection down as the response starts, which
 * OkHttp reports as an `IOException`.
 */
internal class MockWebServerHttpServer : FakeHttpServer {
    private val plan = ResponsePlan()

    private val server =
        MockWebServer().apply {
            dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse =
                        when (val planned = plan.next(request.toRecorded())) {
                            is PlannedResponse.Respond ->
                                MockResponse
                                    .Builder()
                                    .code(planned.status)
                                    .body(planned.body)
                                    .build()
                            PlannedResponse.FailConnection ->
                                MockResponse
                                    .Builder()
                                    .onResponseStart(SocketEffect.ShutdownConnection)
                                    .build()
                        }
                }
            start()
        }

    override val origin: String = server.url("/").toString().removeSuffix("/")

    override val host: String = server.hostName

    override val requestCount: Int get() = plan.requestCount

    override fun enqueue(
        status: Int,
        body: String,
        path: String?,
    ) = plan.enqueue(path, PlannedResponse.Respond(status, body))

    override fun failConnections(path: String?) = plan.failConnections(path)

    override fun takeRequest(): RecordedHttpRequest = plan.take()

    override fun close() = server.close()

    private fun RecordedRequest.toRecorded(): RecordedHttpRequest {
        val url = this.url
        return RecordedHttpRequest(
            path = url.encodedPath,
            query = url.queryParameterNames.associateWith { name -> url.queryParameterValues(name).filterNotNull() },
            headers = headers.names().associateWith { name -> headers.values(name) },
        )
    }
}
