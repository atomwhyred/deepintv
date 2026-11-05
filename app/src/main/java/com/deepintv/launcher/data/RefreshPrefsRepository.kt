package com.deepintv.launcher.data

import android.content.Context

/** How often each network- or sensor-backed widget updates. Longer intervals mean fewer requests
 * and less work on a TV with little RAM; the options are fixed steps so a remote can cycle them. */
data class RefreshSettings(
    val weatherMinutes: Int,
    val cryptoMinutes: Int,
    val calendarMinutes: Int,
    val systemSeconds: Int,
    val watchNextMinutes: Int,
)

class RefreshPrefsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_refresh", Context.MODE_PRIVATE)
    private val light = DeviceProfileRepository(context).tier() == DeviceTier.LIGHT

    /** Only the defaults depend on the device tier; anything the person picked is kept as is. */
    fun load() = RefreshSettings(
        weatherMinutes = prefs.getInt(KEY_WEATHER, DEFAULT_WEATHER),
        cryptoMinutes = prefs.getInt(KEY_CRYPTO, if (light) LIGHT_CRYPTO else DEFAULT_CRYPTO),
        calendarMinutes = prefs.getInt(KEY_CALENDAR, DEFAULT_CALENDAR),
        systemSeconds = prefs.getInt(KEY_SYSTEM, if (light) LIGHT_SYSTEM else DEFAULT_SYSTEM),
        watchNextMinutes = prefs.getInt(KEY_WATCH_NEXT, DEFAULT_WATCH_NEXT),
    )

    fun setWeatherMinutes(value: Int) = prefs.edit().putInt(KEY_WEATHER, value).apply()
    fun setCryptoMinutes(value: Int) = prefs.edit().putInt(KEY_CRYPTO, value).apply()
    fun setCalendarMinutes(value: Int) = prefs.edit().putInt(KEY_CALENDAR, value).apply()
    fun setSystemSeconds(value: Int) = prefs.edit().putInt(KEY_SYSTEM, value).apply()
    fun setWatchNextMinutes(value: Int) = prefs.edit().putInt(KEY_WATCH_NEXT, value).apply()

    companion object {
        private const val KEY_WEATHER = "weather_minutes"
        private const val KEY_CRYPTO = "crypto_minutes"
        private const val KEY_CALENDAR = "calendar_minutes"
        private const val KEY_SYSTEM = "system_seconds"
        private const val KEY_WATCH_NEXT = "watch_next_minutes"
        const val DEFAULT_WEATHER = 60
        const val DEFAULT_CRYPTO = 4
        const val DEFAULT_CALENDAR = 30
        const val DEFAULT_SYSTEM = 4
        const val DEFAULT_WATCH_NEXT = 15
        private const val LIGHT_CRYPTO = 15
        private const val LIGHT_SYSTEM = 10

        val WEATHER_OPTIONS = listOf(15, 30, 60, 180)
        val CRYPTO_OPTIONS = listOf(1, 4, 15, 30)
        val CALENDAR_OPTIONS = listOf(15, 30, 60, 180)
        val SYSTEM_OPTIONS = listOf(2, 4, 10, 30)
        val WATCH_NEXT_OPTIONS = listOf(5, 15, 30, 60)

        /** The option after [current], wrapping round: one OK press on the remote steps through. */
        fun next(options: List<Int>, current: Int): Int {
            val index = options.indexOf(current)
            return options[(index + 1) % options.size]
        }
    }
}
