package com.raikar.moviegallery.testutil

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A scripted HTTP backend for API and repository tests, independent of the HTTP stack under test.
 *
 * Tests talk only to this interface, never to MockWebServer or Ktor's MockEngine directly, so
 * the same test bodies run unchanged whichever transport backs it (see the
 * share-network-layer-ktor design, decision 2). Responses are planned per request path; a
 * request with no plan for its path takes the next path-less plan, and with none left it gets
 * [UNPLANNED_STATUS] so a missing plan fails loudly instead of looking like a real error.
 */
interface FakeHttpServer : AutoCloseable {
    /** Scheme and authority the server answers on, e.g. `http://127.0.0.1:52817`, no trailing slash. */
    val origin: String

    /** Host name of [origin]. */
    val host: String

    /** Number of requests that reached the server so far. */
    val requestCount: Int

    /** Queues one response for the next request to [path] (encoded, e.g. `/3/movie/popular`), or to any path. */
    fun enqueue(
        status: Int,
        body: String = "",
        path: String? = null,
    )

    /** Makes every request to [path] (or to any path without a plan) fail at the connection. */
    fun failConnections(path: String? = null)

    /** The oldest request not yet taken, in arrival order. */
    fun takeRequest(): RecordedHttpRequest

    companion object {
        const val UNPLANNED_STATUS = 418
    }
}

/** What a test can observe about one request: its encoded path, query parameters and headers. */
class RecordedHttpRequest(
    val path: String,
    private val query: Map<String, List<String>>,
    private val headers: Map<String, List<String>>,
) {
    /** First value of query parameter [name], or null when absent. */
    fun queryParameter(name: String): String? = query[name]?.firstOrNull()

    /** All values of header [name], matched case-insensitively and joined with `,`, or null when absent. */
    fun header(name: String): String? =
        headers.entries
            .filter { it.key.equals(name, ignoreCase = true) }
            .flatMap { it.value }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(",")
}

/** The plan for one request. */
internal sealed interface PlannedResponse {
    data class Respond(
        val status: Int,
        val body: String,
    ) : PlannedResponse

    data object FailConnection : PlannedResponse
}

/**
 * The transport-independent bookkeeping behind [FakeHttpServer]: per-path queues, sticky
 * connection failures and the request log. Lock-free over immutable state, because MockEngine
 * answers concurrent requests on its own threads and `synchronized` is JVM-only.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class ResponsePlan {
    private data class State(
        val queues: Map<String?, List<PlannedResponse>> = emptyMap(),
        val failingPaths: Set<String?> = emptySet(),
        val recorded: List<RecordedHttpRequest> = emptyList(),
        val count: Int = 0,
    )

    private val state = AtomicReference(State())

    val requestCount: Int get() = state.load().count

    fun enqueue(
        path: String?,
        response: PlannedResponse,
    ) {
        update { it.copy(queues = it.queues + (path to it.queues[path].orEmpty() + response)) }
    }

    fun failConnections(path: String?) {
        update { it.copy(failingPaths = it.failingPaths + path) }
    }

    /** Records [request] and returns the plan for it. */
    fun next(request: RecordedHttpRequest): PlannedResponse {
        var plan: PlannedResponse? = null
        update { current ->
            val (chosen, queues) = current.choose(request.path)
            plan = chosen
            current.copy(queues = queues, recorded = current.recorded + request, count = current.count + 1)
        }
        return checkNotNull(plan)
    }

    fun take(): RecordedHttpRequest {
        var taken: RecordedHttpRequest? = null
        update { current ->
            taken = checkNotNull(current.recorded.firstOrNull()) { "No request was recorded" }
            current.copy(recorded = current.recorded.drop(1))
        }
        return checkNotNull(taken)
    }

    /** Path-specific plan first, then a sticky failure for the path, then the path-less queue. */
    private fun State.choose(path: String): Pair<PlannedResponse, Map<String?, List<PlannedResponse>>> {
        queues[path]?.firstOrNull()?.let { return it to queues + (path to queues.getValue(path).drop(1)) }
        if (path in failingPaths) return PlannedResponse.FailConnection to queues
        queues[null]?.firstOrNull()?.let { return it to queues + (null to queues.getValue(null).drop(1)) }
        if (null in failingPaths) return PlannedResponse.FailConnection to queues
        return PlannedResponse.Respond(FakeHttpServer.UNPLANNED_STATUS, "no response planned for $path") to queues
    }

    private inline fun update(transform: (State) -> State) {
        while (true) {
            val current = state.load()
            if (state.compareAndSet(current, transform(current))) return
        }
    }
}
