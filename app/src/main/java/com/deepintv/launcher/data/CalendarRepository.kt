package com.deepintv.launcher.data

import android.content.Context
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/** The iCalendar feeds the Calendar widget shows, and the events read from them. A feed is a
 * public .ics link (Google Calendar's "secret address in iCal format", Outlook, Nextcloud...). */
class CalendarRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_calendar", Context.MODE_PRIVATE)
    private val cacheDir = File(context.cacheDir, "calendar").apply { mkdirs() }

    fun feeds(): List<String> = prefs.getString(KEY_FEEDS, null)
        ?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    /** Adds a feed, ignoring duplicates. False when [url] is not a usable link. */
    fun addFeed(url: String): Boolean {
        val clean = normalise(url) ?: return false
        if (clean in feeds()) return true
        save(feeds() + clean)
        return true
    }

    fun removeFeed(url: String) {
        save(feeds() - url)
        cacheFile(url).delete()
    }

    private fun save(urls: List<String>) {
        prefs.edit().putString(KEY_FEEDS, urls.joinToString(SEPARATOR)).apply()
    }

    /** Events from now on for [days] days. Blocking: call off the main thread. Null when there is
     * a feed but none could be read or remembered. */
    fun upcoming(maxAgeMinutes: Int, days: Long = 14): List<CalendarEvent>? {
        val urls = feeds()
        if (urls.isEmpty()) return emptyList()
        val zone = ZoneId.systemDefault()
        val from = Instant.now().atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val to = from.plus(days, ChronoUnit.DAYS)

        var anyFeedRead = false
        val events = urls.flatMap { url ->
            val text = load(url, maxAgeMinutes) ?: return@flatMap emptyList()
            anyFeedRead = true
            runCatching { IcsParser.parse(text, from, to, zone) }.getOrDefault(emptyList())
        }
        if (!anyFeedRead) return null
        val now = Instant.now()
        return events.filter { it.end.toInstant() > now }.sortedBy { it.start }
    }

    /** Fresh copy if the cache is older than [maxAgeMinutes], otherwise the cached one; if the
     * network fails the stale copy is still better than nothing. */
    private fun load(url: String, maxAgeMinutes: Int): String? {
        val file = cacheFile(url)
        val age = System.currentTimeMillis() - file.lastModified()
        if (file.exists() && age < TimeUnit.MINUTES.toMillis(maxAgeMinutes.toLong())) return file.readText()
        val fresh = download(url)
        if (fresh != null) {
            file.writeText(fresh)
            return fresh
        }
        return file.takeIf { it.exists() }?.readText()
    }

    /** True when any feed's cached copy is older than the interval: its download keeps failing. */
    fun isStale(maxAgeMinutes: Int): Boolean =
        feeds().any { isOlderThan(cacheFile(it).lastModified(), maxAgeMinutes) }

    private fun download(url: String): String? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.readTextCapped(MAX_CALENDAR_BYTES)
        } finally {
            connection.disconnect()
        }
    }.onFailure { Log.w(TAG, "Calendar download failed: ${it.message}") }.getOrNull()

    private fun cacheFile(url: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(cacheDir, "$hash.ics")
    }

    companion object {
        private const val TAG = "CalendarRepository"
        private const val KEY_FEEDS = "feeds"
        private const val SEPARATOR = "\n"

        /** webcal:// is just https:// for calendars. Plain http is refused: the feed link is
         * often a secret, and it would travel in the clear. */
        fun normalise(raw: String): String? {
            val text = raw.trim()
            val url = when {
                text.startsWith("webcal://", ignoreCase = true) -> "https://" + text.substring(9)
                text.startsWith("https://", ignoreCase = true) -> text
                else -> return null
            }
            return url.takeIf { it.length > "https://x".length && ' ' !in it }
        }
    }
}
