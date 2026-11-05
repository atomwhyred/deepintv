package com.deepintv.launcher.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class CoinQuote(
    val symbol: String,
    val priceUsd: Double,
    val change24hPercent: Double,
    val sparkline7d: List<Float>,
)

/** CoinGecko, no API key. The cache belongs to one set of coins and lives for the refresh interval
 * the person chose, so changing either never serves the wrong data and a free public API is not
 * polled on every request. */
class CryptoRepository(context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_crypto", Context.MODE_PRIVATE)

    fun quotes(ids: List<String> = DEFAULT_IDS, maxAgeMinutes: Int = DEFAULT_MAX_AGE_MINUTES): List<CoinQuote>? {
        val key = ids.joinToString(",")
        val sameCoins = prefs.getString(KEY_IDS, null) == key
        val cachedAt = if (sameCoins) prefs.getLong(KEY_FETCHED_AT, 0) else 0
        val cachedJson = if (sameCoins) prefs.getString(KEY_JSON, null) else null
        val isFresh = System.currentTimeMillis() - cachedAt < TimeUnit.MINUTES.toMillis(maxAgeMinutes.toLong())

        val json = if (isFresh && cachedJson != null) {
            cachedJson
        } else {
            fetch(ids)?.also {
                prefs.edit().putString(KEY_JSON, it).putString(KEY_IDS, key)
                    .putLong(KEY_FETCHED_AT, System.currentTimeMillis()).apply()
            } ?: cachedJson
        } ?: return null

        return runCatching { parse(json) }
            .onFailure { Log.w(TAG, "Unreadable crypto data", it) }
            .getOrNull()
    }

    /** True when the last good download is older than the interval: what is on screen is a
     * leftover from before the network went away. */
    fun isStale(maxAgeMinutes: Int): Boolean = isOlderThan(prefs.getLong(KEY_FETCHED_AT, 0), maxAgeMinutes)

    private fun fetch(ids: List<String>): String? = runCatching {
        val url = URL(
            "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd" +
                "&ids=${ids.joinToString(",")}&sparkline=true&price_change_percentage=24h",
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
    }.onFailure { Log.w(TAG, "Crypto download failed: ${it.message}") }.getOrNull()

    private fun parse(json: String): List<CoinQuote> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val coin = array.getJSONObject(i)
            val sparkline = coin.optJSONObject("sparkline_in_7d")?.optJSONArray("price")
            val points = sparkline?.let { arr -> (0 until arr.length()).map { arr.getDouble(it).toFloat() } } ?: emptyList()
            CoinQuote(
                symbol = coin.getString("symbol").uppercase(),
                priceUsd = coin.getDouble("current_price"),
                change24hPercent = coin.optDouble("price_change_percentage_24h", 0.0),
                sparkline7d = points,
            )
        }
    }

    companion object {
        private const val TAG = "CryptoRepository"
        private const val KEY_JSON = "json"
        private const val KEY_IDS = "ids"
        const val DEFAULT_MAX_AGE_MINUTES = 4
        private const val KEY_FETCHED_AT = "fetched_at"
        val DEFAULT_IDS = listOf("bitcoin", "ethereum")
    }
}
