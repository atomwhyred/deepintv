package com.deepintv.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchNextParseTest {
    private fun item(title: String, at: Long) =
        WatchNextItem(title, null, "intent:#Intent;end", at, null)

    @Test
    fun `progress is position over duration`() {
        val item = watchNextItem("Film", "S1", "intent:#Intent;end", 10, 30_000, 120_000)!!
        assertEquals(0.25f, item.progress!!, 0.0001f)
    }

    @Test
    fun `progress is clamped and missing without a duration`() {
        assertEquals(1f, watchNextItem("A", null, "i", 1, 500, 100)!!.progress!!, 0f)
        assertNull(watchNextItem("A", null, "i", 1, 500, 0)!!.progress)
        assertNull(watchNextItem("A", null, "i", 1, -1, 100)!!.progress)
    }

    @Test
    fun `rows without a title or an intent are dropped`() {
        assertNull(watchNextItem(null, null, "i", 1, 0, 0))
        assertNull(watchNextItem("  ", null, "i", 1, 0, 0))
        assertNull(watchNextItem("Film", null, null, 1, 0, 0))
        assertNull(watchNextItem("Film", null, "", 1, 0, 0))
    }

    @Test
    fun `blank subtitle becomes null and text is trimmed`() {
        val item = watchNextItem(" Film ", "  ", "i", 1, 0, 0)!!
        assertEquals("Film", item.title)
        assertNull(item.subtitle)
    }

    @Test
    fun `newest first, no repeated titles, limited`() {
        val ranked = rankWatchNext(listOf(item("A", 1), item("B", 5), item("A", 9), item("C", 3)), limit = 2)
        assertEquals(listOf("A", "B"), ranked.map { it.title })
        assertEquals(9L, ranked.first().lastEngagementMs)
    }
}
