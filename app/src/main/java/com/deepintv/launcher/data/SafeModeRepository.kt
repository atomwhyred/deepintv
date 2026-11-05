package com.deepintv.launcher.data

import android.content.Context

/** Remembers recent crashes. A launcher that crashes on every start leaves the TV with no way to
 * reach anything, so after [CRASH_LIMIT] crashes inside [WINDOW_MS] the widgets that talk to the
 * network (the usual suspects: odd server answers, slow DNS on a weak TV) are switched off until
 * the launcher has run for a while without crashing. Nothing the person set up is changed. */
class SafeModeRepository(context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_safety", Context.MODE_PRIVATE)

    /** Called from the uncaught-exception handler, so it writes synchronously: the process is
     * about to die and an `apply()` could be lost. */
    fun recordCrash(nowMs: Long = System.currentTimeMillis()) {
        prefs.edit().putString(KEY_CRASHES, (recent(nowMs) + nowMs).joinToString(",")).commit()
    }

    fun isActive(nowMs: Long = System.currentTimeMillis()): Boolean = recent(nowMs).size >= CRASH_LIMIT

    /** The launcher has been up long enough to call the last start healthy. */
    fun markHealthy() {
        prefs.edit().remove(KEY_CRASHES).apply()
    }

    private fun recent(nowMs: Long): List<Long> =
        prefs.getString(KEY_CRASHES, null)?.split(",")?.mapNotNull { it.toLongOrNull() }.orEmpty()
            .filter { nowMs - it in 0..WINDOW_MS }

    companion object {
        const val CRASH_LIMIT = 3
        const val WINDOW_MS = 5 * 60_000L
        const val HEALTHY_AFTER_MS = 2 * 60_000L
        private const val KEY_CRASHES = "crashes"
    }
}
