package com.deepintv.launcher.data

import android.Manifest
import com.deepintv.launcher.R
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WeatherLocation(val latitude: Double, val longitude: Double, val label: String, val approximate: Boolean)

/** Where the weather widget looks up the forecast: the place the person picked, or Madrid until
 * they do. "Current location" asks the device first and falls back to an approximate place from
 * the public IP, because most TVs have no GPS and no network location provider. */
class WeatherLocationRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_weather", Context.MODE_PRIVATE)

    fun saved(): WeatherLocation? {
        if (!prefs.contains(KEY_LAT)) return null
        return WeatherLocation(
            latitude = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LAT, 0)),
            longitude = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LON, 0)),
            label = prefs.getString(KEY_LABEL, null) ?: context.getString(R.string.weather_current_location),
            approximate = prefs.getBoolean(KEY_APPROX, false),
        )
    }

    fun save(location: WeatherLocation) {
        prefs.edit()
            .putLong(KEY_LAT, java.lang.Double.doubleToRawLongBits(location.latitude))
            .putLong(KEY_LON, java.lang.Double.doubleToRawLongBits(location.longitude))
            .putString(KEY_LABEL, location.label)
            .putBoolean(KEY_APPROX, location.approximate)
            .apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_LAT).remove(KEY_LON).remove(KEY_LABEL).remove(KEY_APPROX).apply()
    }

    /** Blocking: call off the main thread. Null when neither the device nor the network knows. */
    fun detectCurrent(): WeatherLocation? = fromDevice() ?: fromPublicIp()

    private fun fromDevice(): WeatherLocation? = runCatching {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val manager = context.getSystemService(LocationManager::class.java)
        val fix = manager.getProviders(true)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time } ?: return null
        WeatherLocation(fix.latitude, fix.longitude, cityName(fix.latitude, fix.longitude) ?: context.getString(R.string.weather_current_location), approximate = false)
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun cityName(latitude: Double, longitude: Double): String? = runCatching {
        if (!Geocoder.isPresent()) return null
        Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)
            ?.firstOrNull()?.let { it.locality ?: it.subAdminArea }
    }.getOrNull()

    private fun fromPublicIp(): WeatherLocation? = runCatching {
        val connection = URL("https://ipwho.is/?fields=success,latitude,longitude,city").openConnection() as HttpURLConnection
        val body = try {
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.inputStream.readTextCapped(MAX_API_BYTES)
        } finally {
            connection.disconnect()
        }
        val json = JSONObject(body)
        if (!json.optBoolean("success", false)) return null
        WeatherLocation(
            latitude = json.getDouble("latitude"),
            longitude = json.getDouble("longitude"),
            label = json.optString("city").ifBlank { context.getString(R.string.weather_current_location) },
            approximate = true,
        )
    }.getOrNull()

    companion object {
        private const val KEY_LAT = "loc_lat"
        private const val KEY_LON = "loc_lon"
        private const val KEY_LABEL = "loc_label"
        private const val KEY_APPROX = "loc_approx"
    }
}
