package com.deepintv.launcher.data

import android.content.Context

/** Per-app launch counts, for the app drawer's "by frequency" sort. */
class UsageRepository(context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_usage", Context.MODE_PRIVATE)

    fun recordLaunch(packageName: String) {
        prefs.edit().putInt(packageName, countFor(packageName) + 1).apply()
    }

    fun countFor(packageName: String): Int = prefs.getInt(packageName, 0)
}
