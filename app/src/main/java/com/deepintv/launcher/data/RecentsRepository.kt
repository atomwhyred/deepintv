package com.deepintv.launcher.data

import android.content.Context

/** The most recently launched apps, newest first, as a package-name list in SharedPreferences.
 * [UsageRepository] only keeps counters, so it can't tell which app was opened last. */
class RecentsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_recents", Context.MODE_PRIVATE)

    fun load(): List<String> =
        prefs.getString(KEY_ORDER, null)?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()

    fun push(packageName: String) {
        val current = load()
        val updated = pushed(current, packageName)
        if (updated != current) prefs.edit().putString(KEY_ORDER, updated.joinToString(SEPARATOR)).apply()
    }

    companion object {
        internal const val MAX_RECENTS = 30

        /** [recents] with [packageName] moved to the front (no duplicates), trimmed to [max]. */
        internal fun pushed(recents: List<String>, packageName: String, max: Int = MAX_RECENTS): List<String> =
            (listOf(packageName) + recents.filter { it != packageName }).take(max)

        private const val KEY_ORDER = "order"
        private const val SEPARATOR = ","
    }
}
