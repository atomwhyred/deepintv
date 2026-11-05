package com.deepintv.launcher.data

import android.app.ActivityManager
import android.content.Context
import java.io.RandomAccessFile

/** Reads /proc/meminfo directly: ActivityManager.getMemoryInfo() rounds to the nearest threshold
 * on recent Android and is not precise enough to show how much Autocleanup actually freed. */
class MemoryRepository(private val context: Context) {

    fun availableMemoryKb(): Long {
        return runCatching {
            RandomAccessFile("/proc/meminfo", "r").use { file ->
                generateSequence { file.readLine() }
                    .firstOrNull { it.startsWith("MemAvailable:") }
                    ?.filter { it.isDigit() }
                    ?.toLongOrNull()
            }
        }.getOrNull() ?: 0L
    }

    /** Kills the cached/background process of every other launchable app except the TV vendor's own. Returns KB freed
     * (best-effort: a killed process's memory is not reclaimed instantly, so this is a lower bound). */
    fun autocleanup(apps: List<AppInfo>): Long {
        val before = availableMemoryKb()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        apps.map { it.packageName }.distinct()
            .filterNot { pkg -> PROTECTED_PREFIXES.any { pkg.startsWith(it) } }
            .forEach { pkg -> runCatching { activityManager.killBackgroundProcesses(pkg) } }
        // The kernel reclaims a killed process's pages lazily, not the instant it dies; without
        // this pause "after" reads barely above "before" and Autocleanup looks like it did nothing.
        Thread.sleep(400)
        return (availableMemoryKb() - before).coerceAtLeast(0)
    }

    private companion object {
        /** TV vendor and SoC services that also show a leanback launcher entry (live TV, input
         * and sound setup): killing their processes drops the picture or the remote. */
        val PROTECTED_PREFIXES = listOf("com.tcl.", "com.realtek.", "com.mediatek.", "com.android.tv", "com.google.android.tv")
    }
}
