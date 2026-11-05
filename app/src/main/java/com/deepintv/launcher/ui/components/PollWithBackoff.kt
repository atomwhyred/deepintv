package com.deepintv.launcher.ui.components

import kotlinx.coroutines.delay

/** What one poll produced: the data to show (null when there is none at all) and whether it is
 * a leftover from before the network went away. */
class Polled<T>(val value: T?, val stale: Boolean)

private const val RETRY_BASE_MS = 30_000L
private const val MAX_BACKOFF_STEPS = 6

/** Runs [fetch] every [intervalMs] and hands each outcome to [onResult]. After a failure it
 * retries sooner (30 s, 1 min, 2 min... never later than the normal interval) instead of showing
 * nothing for a whole interval, which on an hourly widget is a long time to wait out a blip. */
suspend fun <T> pollWithBackoff(intervalMs: Long, fetch: suspend () -> Polled<T>, onResult: (Polled<T>) -> Unit): Nothing {
    var failures = 0
    while (true) {
        val result = fetch()
        onResult(result)
        failures = if (result.value == null || result.stale) failures + 1 else 0
        val wait = if (failures == 0) intervalMs else minOf(intervalMs, RETRY_BASE_MS shl (failures - 1).coerceAtMost(MAX_BACKOFF_STEPS))
        delay(wait)
    }
}
