package com.deepintv.launcher.data

import android.content.Context

data class CoinOption(val id: String, val symbol: String)

/** Coins the Crypto widget can track — CoinGecko ids, not just BTC/ETH, so the person can add
 * more ("necesito poder... añadir más criptomonedas"). */
val AVAILABLE_COINS = listOf(
    CoinOption("bitcoin", "BTC"),
    CoinOption("ethereum", "ETH"),
    CoinOption("binancecoin", "BNB"),
    CoinOption("solana", "SOL"),
    CoinOption("ripple", "XRP"),
    CoinOption("cardano", "ADA"),
    CoinOption("dogecoin", "DOGE"),
    CoinOption("polkadot", "DOT"),
)

class CryptoPrefsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_crypto_prefs", Context.MODE_PRIVATE)

    fun selectedIds(): List<String> {
        val saved = prefs.getString(KEY_IDS, null) ?: return CryptoRepository.DEFAULT_IDS
        val ids = saved.split(SEPARATOR).filter { it.isNotBlank() }
        return ids.ifEmpty { CryptoRepository.DEFAULT_IDS }
    }

    fun setSelected(ids: List<String>) {
        prefs.edit().putString(KEY_IDS, ids.joinToString(SEPARATOR)).apply()
    }

    fun toggle(id: String, enabled: Boolean) {
        val current = selectedIds()
        val updated = if (enabled) {
            if (id in current) current else current + id
        } else {
            current - id
        }
        setSelected(updated)
    }

    companion object {
        private const val KEY_IDS = "selected_ids"
        private const val SEPARATOR = ","
    }
}
