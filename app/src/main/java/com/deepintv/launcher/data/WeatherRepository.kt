package com.deepintv.launcher.data

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class DayForecast(val weekday: String, val maxC: Int, val minC: Int, val code: Int)
data class WeatherNow(val city: String, val tempC: Int, val code: Int, val days: List<DayForecast>)

/**
 * Open-Meteo, no API key needed. Cached in SharedPreferences for the refresh interval the person chose (an hour
 * by default), so this is not polling.
 *
 * The coordinates come from WeatherLocationRepository (the widget's "use my current location"),
 * and are Madrid until the person picks something else.
 */
class WeatherRepository(context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_weather", Context.MODE_PRIVATE)

    fun currentWeather(location: WeatherLocation? = null, maxAgeMinutes: Int = 60): WeatherNow? {
        val latitude = location?.latitude ?: DEFAULT_LAT
        val longitude = location?.longitude ?: DEFAULT_LON
        val cityLabel = location?.label ?: DEFAULT_CITY
        // The cache belongs to one place: a different one must not be served from it.
        val coords = "$latitude,$longitude"
        val cachedAt = if (prefs.getString(KEY_COORDS, null) == coords) prefs.getLong(KEY_FETCHED_AT, 0) else 0
        val cachedJson = if (prefs.getString(KEY_COORDS, null) == coords) prefs.getString(KEY_JSON, null) else null
        val isFresh = System.currentTimeMillis() - cachedAt < TimeUnit.MINUTES.toMillis(maxAgeMinutes.toLong())

        val json = if (isFresh && cachedJson != null) {
            cachedJson
        } else {
            fetch(latitude, longitude)?.also {
                prefs.edit().putString(KEY_JSON, it).putString(KEY_COORDS, coords).putLong(KEY_FETCHED_AT, System.currentTimeMillis()).apply()
            } ?: cachedJson
        } ?: return null

        return runCatching { parse(json, cityLabel) }
            .onFailure { Log.w(TAG, "Unreadable weather data", it) }
            .getOrNull()
    }

    /** True when the last good download is older than the interval (offline since then). */
    fun isStale(maxAgeMinutes: Int): Boolean = isOlderThan(prefs.getLong(KEY_FETCHED_AT, 0), maxAgeMinutes)

    private fun fetch(latitude: Double, longitude: Double): String? = runCatching {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min,weather_code" +
                "&timezone=auto&forecast_days=5",
        )
        (url.openConnection() as HttpURLConnection).run {
            connectTimeout = 8000
            readTimeout = 8000
            try {
                inputStream.readTextCapped(MAX_API_BYTES)
            } finally {
                disconnect()
            }
        }
    }.onFailure { Log.w(TAG, "Weather download failed: ${it.message}") }.getOrNull()

    private fun parse(json: String, cityLabel: String): WeatherNow {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val dailyTimes = daily.getJSONArray("time")
        val dailyMax = daily.getJSONArray("temperature_2m_max")
        val dailyMin = daily.getJSONArray("temperature_2m_min")
        val dailyCode = daily.getJSONArray("weather_code")

        val days = (0 until dailyTimes.length()).map { i ->
            DayForecast(
                weekday = weekdayLabel(dailyTimes.getString(i)),
                maxC = dailyMax.getDouble(i).toInt(),
                minC = dailyMin.getDouble(i).toInt(),
                code = dailyCode.getInt(i),
            )
        }
        return WeatherNow(
            city = cityLabel,
            tempC = current.getDouble("temperature_2m").toInt(),
            code = current.getInt("weather_code"),
            days = days,
        )
    }

    private fun weekdayLabel(isoDate: String): String = runCatching {
        val (y, m, d) = isoDate.split("-").map { it.toInt() }
        val calendar = java.util.GregorianCalendar(y, m - 1, d)
        calendar.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SHORT, java.util.Locale.getDefault())
            ?.replaceFirstChar { it.uppercase() } ?: isoDate
    }.getOrDefault(isoDate)

    companion object {
        private const val TAG = "WeatherRepository"
        private const val KEY_JSON = "json"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val KEY_COORDS = "coords"
        const val DEFAULT_LAT = 40.4168
        const val DEFAULT_LON = -3.7038
        const val DEFAULT_CITY = "Madrid"
    }
}
