package com.deepintv.launcher.data

import android.os.Environment
import android.os.StatFs
import java.io.RandomAccessFile

data class SystemStats(val usedMemKb: Long, val totalMemKb: Long)
data class StorageStats(val usedBytes: Long, val totalBytes: Long)

/** RAM and storage for the System widget. CPU is not read: since Android 8 an app cannot see
 * /proc/stat, and no other source is open to a normal app. */
class SystemStatsRepository {

    fun memoryStats(): SystemStats {
        var totalKb = 0L
        var availableKb = 0L
        runCatching {
            RandomAccessFile("/proc/meminfo", "r").use { file ->
                generateSequence { file.readLine() }.forEach { line ->
                    when {
                        line.startsWith("MemTotal:") -> totalKb = line.filter { it.isDigit() }.toLongOrNull() ?: 0
                        line.startsWith("MemAvailable:") -> availableKb = line.filter { it.isDigit() }.toLongOrNull() ?: 0
                    }
                }
            }
        }
        return SystemStats(usedMemKb = (totalKb - availableKb).coerceAtLeast(0), totalMemKb = totalKb)
    }

    /** Internal storage (the /data partition, where apps and their data live). */
    fun storageStats(): StorageStats? = runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        StorageStats(usedBytes = (total - stat.availableBytes).coerceAtLeast(0), totalBytes = total)
    }.getOrNull()?.takeIf { it.totalBytes > 0 }
}
