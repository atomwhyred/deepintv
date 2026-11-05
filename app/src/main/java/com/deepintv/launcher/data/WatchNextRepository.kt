package com.deepintv.launcher.data

import android.content.Context
import android.content.pm.PackageManager
import android.media.tv.TvContract
import android.util.Log

/** One "continue watching" entry published by a streaming app to the TV's Watch Next row. */
data class WatchNextItem(
    val title: String,
    val subtitle: String?,
    /** An Intent URI (`Intent.parseUri`) that reopens the content where it was left. */
    val intentUri: String,
    val lastEngagementMs: Long,
    /** 0..1 when the app reports position and duration, else null. */
    val progress: Float?,
)

/** What a query found: the entries, or that the TV refused to share them without the permission. */
class WatchNextResult(val items: List<WatchNextItem>, val permissionDenied: Boolean)

/** Reads the system Watch Next row. Needs READ_TV_LISTINGS (a runtime permission on the TCL TV),
 * and it may still come back empty if the TV doesn't hand that row to other apps. */
class WatchNextRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** Null on an unexpected failure (so the caller retries sooner); never throws. */
    fun load(limit: Int = DEFAULT_LIMIT): WatchNextResult? {
        if (!hasPermission()) return WatchNextResult(emptyList(), permissionDenied = true)
        return try {
            val items = ArrayList<WatchNextItem>()
            context.contentResolver.query(
                TvContract.WatchNextPrograms.CONTENT_URI,
                PROJECTION,
                null,
                null,
                null,
            )?.use { cursor ->
                // A few apps publish many rows: stop reading well before memory could matter.
                var read = 0
                while (cursor.moveToNext() && read < MAX_ROWS) {
                    read++
                    watchNextItem(
                        title = cursor.getString(0),
                        subtitle = cursor.getString(1),
                        intentUri = cursor.getString(2),
                        lastEngagementMs = cursor.getLong(3),
                        positionMs = cursor.getLong(4),
                        durationMs = cursor.getLong(5),
                    )?.let(items::add)
                }
            }
            WatchNextResult(rankWatchNext(items, limit), permissionDenied = false)
        } catch (e: SecurityException) {
            Log.w(TAG, "Watch Next not readable", e)
            WatchNextResult(emptyList(), permissionDenied = true)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Watch Next query failed", e)
            null
        }
    }

    companion object {
        private const val TAG = "WatchNext"
        /** Not in the public SDK constants, but declared and requestable all the same. */
        const val PERMISSION = "android.permission.READ_TV_LISTINGS"
        const val DEFAULT_LIMIT = 5
        private const val MAX_ROWS = 50

        // Column names of TvContract.WatchNextPrograms, in the order the cursor is read above.
        private val PROJECTION = arrayOf(
            "title",
            "short_description",
            "intent_uri",
            "last_engagement_time_utc_millis",
            "last_playback_position_millis",
            "duration_millis",
        )
    }
}

/** One cursor row as an entry, or null when it can't be shown or reopened (no title or intent). */
internal fun watchNextItem(
    title: String?,
    subtitle: String?,
    intentUri: String?,
    lastEngagementMs: Long,
    positionMs: Long,
    durationMs: Long,
): WatchNextItem? {
    if (title.isNullOrBlank() || intentUri.isNullOrBlank()) return null
    val progress = if (durationMs > 0 && positionMs >= 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else null
    return WatchNextItem(
        title = title.trim(),
        subtitle = subtitle?.trim()?.takeIf { it.isNotEmpty() },
        intentUri = intentUri,
        lastEngagementMs = lastEngagementMs,
        progress = progress,
    )
}

/** Most recently watched first, without repeats of the same title, at most [limit]. */
internal fun rankWatchNext(items: List<WatchNextItem>, limit: Int): List<WatchNextItem> =
    items.sortedByDescending { it.lastEngagementMs }.distinctBy { it.title }.take(limit)
