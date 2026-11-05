package com.deepintv.launcher.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class VpnApp(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val launchIntent: Intent?,
)

/** The VPN clients installed on the TV, and which one the VPN widget represents. An app is a VPN
 * client if it declares a VpnService, so Proton, WireGuard, OpenVPN and the rest all show up
 * with no per-app list to maintain. */
class VpnAppRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_vpn", Context.MODE_PRIVATE)

    fun installed(): List<VpnApp> {
        val pm = context.packageManager
        return pm.queryIntentServices(Intent(VPN_SERVICE_ACTION), 0)
            .mapNotNull { it.serviceInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { info ->
                VpnApp(
                    packageName = info.packageName,
                    label = info.loadLabel(pm).toString(),
                    icon = info.loadIcon(pm),
                    launchIntent = pm.getLeanbackLaunchIntentForPackage(info.packageName)
                        ?: pm.getLaunchIntentForPackage(info.packageName),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun selectedPackage(): String? = prefs.getString(KEY_SELECTED, null)

    fun select(packageName: String) {
        prefs.edit().putString(KEY_SELECTED, packageName).apply()
    }

    /** The chosen app if it is still installed, otherwise the first one found. */
    fun selected(installed: List<VpnApp> = installed()): VpnApp? =
        installed.firstOrNull { it.packageName == selectedPackage() } ?: installed.firstOrNull()

    private companion object {
        const val VPN_SERVICE_ACTION = "android.net.VpnService"
        const val KEY_SELECTED = "selected_package"
    }
}
