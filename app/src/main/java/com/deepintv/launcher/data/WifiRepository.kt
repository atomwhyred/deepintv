package com.deepintv.launcher.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager

data class WifiStatus(val connected: Boolean, val ssid: String?)

/** Read-only Wi-Fi status: apps cannot toggle Wi-Fi themselves since Android 10 (API 29), so the
 * control center links to the system Wi-Fi settings instead of pretending to have a switch. */
class WifiRepository(private val context: Context) {

    fun currentStatus(): WifiStatus {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return WifiStatus(false, null)
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        if (!isWifi) return WifiStatus(false, null)

        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ssid = wifiManager.connectionInfo?.ssid
            ?.trim('"')
            ?.takeIf { it.isNotBlank() && it != WifiManager.UNKNOWN_SSID }
        return WifiStatus(connected = true, ssid = ssid)
    }

    /** Calls [onChange] whenever the default network comes, goes or changes, so a panel that is
     * already open shows the current state. Returns the function that stops listening. */
    fun observe(onChange: () -> Unit): () -> Unit {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onChange()
            override fun onLost(network: Network) = onChange()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = onChange()
        }
        return runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)
            ({ runCatching { connectivityManager.unregisterNetworkCallback(callback) }; Unit })
        }.getOrDefault({})
    }
}
