package com.deepintv.launcher.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class CityResult(val location: WeatherLocation, val detail: String)

/** Looks a typed or spoken place name up with Open-Meteo's geocoding, the same provider the
 * forecast comes from, so one provider covers both and needs no key. */
class CitySearchRepository {

    /** Blocking: call off the main thread. Empty when nothing matches or there is no network. */
    fun search(query: String, locale: Locale = Locale.getDefault()): List<CityResult> = runCatching {
        val text = query.trim()
        if (text.length < 2) return emptyList()
        val url = URL(
            "https://geocoding-api.open-meteo.com/v1/search?count=5&format=json" +
                "&language=${locale.language}&name=${URLEncoder.encode(text, "UTF-8")}",
        )
        val connection = url.openConnection() as HttpURLConnection
        val body = try {
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.inputStream.readTextCapped(MAX_API_BYTES)
        } finally {
            connection.disconnect()
        }
        val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
        (0 until results.length()).mapNotNull { i ->
            val item = results.getJSONObject(i)
            val name = item.optString("name").ifBlank { return@mapNotNull null }
            val detail = listOf(item.optString("admin1"), item.optString("country"))
                .filter { it.isNotBlank() && it != name }
                .joinToString(", ")
            CityResult(
                location = WeatherLocation(
                    latitude = item.getDouble("latitude"),
                    longitude = item.getDouble("longitude"),
                    label = name,
                    approximate = false,
                ),
                detail = detail,
            )
        }
    }.getOrDefault(emptyList())
}
