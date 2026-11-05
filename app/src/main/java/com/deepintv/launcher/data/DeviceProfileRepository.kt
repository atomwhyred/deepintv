package com.deepintv.launcher.data

import android.app.ActivityManager
import android.content.Context

enum class DeviceTier { LIGHT, FULL }

/** Sorts the TV into a tier by RAM and cores, so a 1 GB set can do less background work than a
 * big one. A tier only ever changes how often things refresh and how many layers a glow is
 * drawn with, never colours, radii or alpha: Home looks the same on both. */
class DeviceProfileRepository(private val context: Context) {

    fun tier(): DeviceTier = cached ?: compute().also { cached = it }

    private fun compute(): DeviceTier {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val totalMem = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }.totalMem
        val light = activityManager.isLowRamDevice ||
            totalMem in 1 until LIGHT_TOTAL_MEM_BYTES ||
            Runtime.getRuntime().availableProcessors() <= LIGHT_MAX_CORES
        return if (light) DeviceTier.LIGHT else DeviceTier.FULL
    }

    private companion object {
        const val LIGHT_TOTAL_MEM_BYTES = 1_610_612_736L // 1.5 GiB: 1 GB sets report ~0.9-1.0 GiB usable
        const val LIGHT_MAX_CORES = 2
        @Volatile var cached: DeviceTier? = null
    }
}
