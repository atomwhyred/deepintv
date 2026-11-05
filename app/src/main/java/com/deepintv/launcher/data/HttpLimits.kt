package com.deepintv.launcher.data

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Most a widget download may weigh: an API answer is a few KB, a busy calendar a few hundred. */
internal const val MAX_API_BYTES = 256 * 1024
internal const val MAX_CALENDAR_BYTES = 2 * 1024 * 1024

/**
 * Reads the whole stream as UTF-8 text, but gives up with an [IOException] past [maxBytes], so an
 * oversized (or endless) answer cannot run a 1 GB TV out of memory. Callers already treat a failed
 * download as "no data". Closes the stream.
 */
internal fun InputStream.readTextCapped(maxBytes: Int): String = use { input ->
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    while (true) {
        val n = input.read(buffer)
        if (n < 0) break
        if (out.size() + n > maxBytes) throw IOException("Response larger than $maxBytes bytes")
        out.write(buffer, 0, n)
    }
    out.toString(Charsets.UTF_8.name())
}
