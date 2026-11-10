package com.raikar.moviegallery.platform

import kotlinx.coroutines.CompletableDeferred

/**
 * Suspends until a bridge [call] completes, and returns what it completed with
 * (add-ios-platform-layer design, decision 2).
 *
 * - The deferred is thread-safe, so Swift may complete on any queue.
 * - A second completion is ignored, because [CompletableDeferred.complete] returns `false`
 *   instead of throwing as a second coroutine resume would.
 * - Cancelling the caller cancels the wait, and a late completion is dropped.
 */
internal suspend fun <T> awaitBridge(call: (complete: (T) -> Unit) -> Unit): T {
    val result = CompletableDeferred<T>()
    call { result.complete(it) }
    return result.await()
}
