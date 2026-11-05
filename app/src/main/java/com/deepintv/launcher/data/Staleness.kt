package com.deepintv.launcher.data

import java.util.concurrent.TimeUnit

/** Slack on top of the refresh interval, so a download that is merely a little late is not
 * reported as old data. */
private const val GRACE_MINUTES = 2L

/** True when [fetchedAtMs] (0 = never) is further back than [maxAgeMinutes] plus a short grace. */
fun isOlderThan(fetchedAtMs: Long, maxAgeMinutes: Int, nowMs: Long = System.currentTimeMillis()): Boolean =
    fetchedAtMs <= 0 || nowMs - fetchedAtMs > TimeUnit.MINUTES.toMillis(maxAgeMinutes + GRACE_MINUTES)
