package com.deepintv.launcher.data

import android.content.Context

/** The dock's favourite apps, as an ordered package-name list in SharedPreferences. */
class FavoritesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_favorites", Context.MODE_PRIVATE)

    fun load(): List<String> =
        prefs.getString(KEY_ORDER, null)?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()

    fun save(packages: List<String>) {
        prefs.edit().putString(KEY_ORDER, packages.joinToString(SEPARATOR)).apply()
    }

    fun add(packageName: String) {
        val current = load()
        if (packageName in current) return
        save(current + packageName)
    }

    fun remove(packageName: String) {
        save(load() - packageName)
    }

    fun move(packageName: String, delta: Int) {
        val current = load()
        val moved = moved(current, packageName, delta)
        if (moved != current) save(moved)
    }

    companion object {
        /** [order] with [packageName] shifted by [delta] places, stopping at either end. */
        internal fun moved(order: List<String>, packageName: String, delta: Int): List<String> {
            val from = order.indexOf(packageName)
            if (from < 0) return order
            val to = (from + delta).coerceIn(0, order.lastIndex)
            if (to == from) return order
            return order.toMutableList().apply {
                removeAt(from)
                add(to, packageName)
            }
        }

        private const val KEY_ORDER = "order"
        private const val SEPARATOR = ","
    }
}
