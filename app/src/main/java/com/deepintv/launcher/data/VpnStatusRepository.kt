package com.deepintv.launcher.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.NetworkInterface
import java.util.Collections

data class VpnStatus(val connected: Boolean, val interfaceName: String?)

/** Detects an active VPN without asking any particular app: Android reports a VPN transport on
 * the active network, and every client also brings up a tun/ppp/wg interface. */
class VpnStatusRepository(private val context: Context) {
    fun currentStatus(): VpnStatus {
        val iface = runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces())
                .firstOrNull { iface ->
                    iface.isUp && !iface.isLoopback &&
                        (iface.name.startsWith("tun") || iface.name.startsWith("ppp") || iface.name.startsWith("wg"))
                }
        }.getOrNull()
        val transport = runCatching {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }.getOrDefault(false)
        return VpnStatus(connected = iface != null || transport, interfaceName = iface?.name)
    }
}
