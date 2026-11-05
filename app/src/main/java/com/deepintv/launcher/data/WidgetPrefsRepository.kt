package com.deepintv.launcher.data

import android.content.Context
import androidx.annotation.StringRes
import com.deepintv.launcher.R

/** VPN is not a widget of its own any more: its state lives inside SYSTEM. */
enum class WidgetKind(@StringRes val labelRes: Int) {
    WEATHER(R.string.widget_weather),
    CALENDAR(R.string.widget_calendar),
    SYSTEM(R.string.widget_system),
    CRYPTO(R.string.widget_crypto),
    WATCH_NEXT(R.string.widget_watch_next),
    NOTIFICATIONS(R.string.widget_notifications),
}

/** Which widget cards show on Home, and in what order — the "editable widgets" the person asked
 * for: add, remove, reorder, all persisted in SharedPreferences. */
class WidgetPrefsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_widgets", Context.MODE_PRIVATE)

    fun enabledWidgets(): List<WidgetKind> {
        val saved = prefs.getString(KEY_ORDER, null) ?: return DEFAULT_ORDER
        return saved.split(SEPARATOR)
            // The standalone VPN card was merged into System: keep its slot for people who had it.
            .map { name -> if (name == "VPN") WidgetKind.SYSTEM.name else name }
            .mapNotNull { name -> runCatching { WidgetKind.valueOf(name) }.getOrNull() }
            .distinct()
    }

    private fun save(kinds: List<WidgetKind>) {
        prefs.edit().putString(KEY_ORDER, kinds.joinToString(SEPARATOR) { it.name }).apply()
    }

    /** Replaces which widgets show and in what order (used by the phone pairing page). */
    fun setOrder(kinds: List<WidgetKind>) = save(kinds.distinct())

    fun setEnabled(kind: WidgetKind, enabled: Boolean) {
        val current = enabledWidgets()
        val updated = if (enabled) {
            if (kind in current) current else current + kind
        } else {
            current - kind
        }
        save(updated)
    }

    fun move(kind: WidgetKind, delta: Int) {
        val current = enabledWidgets().toMutableList()
        val from = current.indexOf(kind)
        if (from < 0) return
        val to = (from + delta).coerceIn(0, current.lastIndex)
        if (to == from) return
        current.removeAt(from)
        current.add(to, kind)
        save(current)
    }

    companion object {
        private const val KEY_ORDER = "enabled_order"
        /** Calendar stays opt-in: it needs a feed URL before it shows anything. */
        private val DEFAULT_ORDER = listOf(WidgetKind.WEATHER, WidgetKind.CRYPTO, WidgetKind.SYSTEM)
        private const val SEPARATOR = ","
    }
}
